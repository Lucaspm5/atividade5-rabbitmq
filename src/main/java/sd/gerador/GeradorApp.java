package sd.gerador;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import sd.common.Config;
import sd.common.Mensagem;
import sd.common.Topologia;

public class GeradorApp {

    private static final Random ALEATORIO = new Random();

    private static final String[] PRODUTOS = {
        "notebook", "fone de ouvido", "smartphone", "teclado mecânico", "mouse sem fio", "monitor",
        "cadeira gamer", "cafeteira", "smart TV", "caixa de som", "tablet", "impressora"
    };

    private static final String[] AVALIACOES = {
        // positivas
        "Comprei o {p} semana passada e adorei, chegou rápido e funciona muito bem!",
        "O {p} é excelente, qualidade impressionante, recomendo a todos.",
        "Estou muito satisfeito com o {p}, superou o que eu esperava.",
        "Ótimo {p}! Perfeito para o meu dia a dia, valeu cada centavo.",
        "Que maravilha de {p}, produto incrível e entrega ótima.",
        "Gostei bastante do {p}, acabamento lindo e desempenho excelente.",
        // negativas
        "O {p} é péssimo, parou de funcionar depois de poucos dias.",
        "Muito decepcionado com o {p}, chegou com defeito e ninguém resolve.",
        "Horrível esse {p}, qualidade ruim e muito frágil. Não recomendo.",
        "Me arrependi de comprar o {p}, veio quebrado e o suporte é terrível.",
        "Pior {p} que já tive, lixo total, dinheiro jogado fora.",
        "O {p} trava toda hora e a bateria dura pouco, muito insatisfeito.",
        // neutras
        "O {p} é razoável, cumpre o básico e nada além disso.",
        "Recebi o {p} no prazo, é um produto mediano, dentro do esperado.",
        "{p} comum, atende o que promete, sem surpresas.",
        "O {p} é ok, serve para o uso normal, nada de especial.",
        "Produto padrão esse {p}, regular, faz o que deve fazer.",
        "Achei o {p} aceitável pelo preço, nem se destaca nem decepciona."
    };

    private static final String[] SOLICITACOES = {
        // acesso
        "Não consigo fazer login no sistema, aparece usuário ou senha inválidos.",
        "Minha conta foi bloqueada e não tenho permissão para acessar o painel.",
        "Esqueci a senha e o código de verificação nunca chega no meu e-mail.",
        "Acesso negado quando tento entrar, o token de autenticação expirou.",
        "A autenticação falha toda vez que tento entrar com meu usuário.",
        "Perdi as permissões de acesso ao módulo financeiro depois da atualização.",
        // desempenho
        "O sistema está extremamente lento, as telas demoram muito para carregar.",
        "O aplicativo trava e congela sempre que abro os relatórios.",
        "Consumo de memória e CPU altíssimo, a máquina fica lenta e travando.",
        "A busca demora minutos para responder, lentidão insuportável.",
        "Tempo de resposta muito alto no horário de pico, tudo engasga.",
        "Ao exportar os dados a aplicação fica pesada e congela por muito tempo.",
        // conexão
        "Erro de conexão com o servidor, a rede cai o tempo todo.",
        "Timeout ao conectar na API, conexão recusada pelo host remoto.",
        "A VPN desconecta a todo instante e o aplicativo perde a conexão.",
        "Sem conexão com a internet no app, falha de rede ao sincronizar.",
        "Conexão instável com o banco de dados, perda de pacotes constante.",
        "Não consigo conectar ao serviço, aparece erro de DNS e rede indisponível."
    };

    public static void main(String[] args) throws Exception {
        int porSegundo = Math.max(5, Config.envInt("MESSAGES_PER_SECOND", 10));
        double taxaErro = Config.envDouble("ERROR_RATE", 0.05);

        ObjectMapper json = new ObjectMapper();
        Connection conexao = Config.conectar();
        Channel canal = conexao.createChannel();
        Topologia.declarar(canal);

        System.out.printf("[gerador] iniciado: %d msg/s, taxa de mensagens corrompidas = %.0f%%%n",
                porSegundo, taxaErro * 100);

        AtomicLong contador = new AtomicLong();
        long periodoNs = 1_000_000_000L / porSegundo;

        // Um único thread: Channel do RabbitMQ não é thread-safe.
        ScheduledExecutorService agendador = Executors.newSingleThreadScheduledExecutor();
        agendador.scheduleAtFixedRate(() -> {
            try {
                publicar(canal, json, contador.incrementAndGet(), taxaErro);
            } catch (Exception e) {
                System.out.println("[gerador] erro ao publicar: " + e.getMessage());
            }
        }, 0, periodoNs, TimeUnit.NANOSECONDS);
    }

    private static void publicar(Channel canal, ObjectMapper json, long numero, double taxaErro) throws Exception {
        boolean avaliacao = ALEATORIO.nextBoolean();
        String tipo = avaliacao ? "review" : "support";
        String routingKey = avaliacao ? Topologia.RK_REVIEW : Topologia.RK_SUPORTE;
        String conteudo = avaliacao ? gerarAvaliacao() : sortear(SOLICITACOES);

        String id = UUID.randomUUID().toString();
        Mensagem msg = new Mensagem(id, Instant.now().toString(), tipo, conteudo);

        boolean corrompida = ALEATORIO.nextDouble() < taxaErro;
        byte[] corpo = corrompida
                ? ("{\"id\":\"" + id + "\",\"conteudo\": ###MENSAGEM CORROMPIDA###").getBytes(StandardCharsets.UTF_8)
                : json.writeValueAsBytes(msg);

        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .deliveryMode(2)
                .messageId(id)
                .type(tipo)
                .timestamp(new Date())
                .build();

        canal.basicPublish(Topologia.EXCHANGE, routingKey, props, corpo);

        System.out.printf("[gerador] #%d %-15s id=%s %s%n", numero, routingKey, id.substring(0, 8),
                corrompida ? "(CORROMPIDA DE PROPOSITO)" : resumir(conteudo));
    }

    private static String gerarAvaliacao() {
        return sortear(AVALIACOES).replace("{p}", sortear(PRODUTOS));
    }

    private static String sortear(String[] opcoes) {
        return opcoes[ALEATORIO.nextInt(opcoes.length)];
    }

    private static String resumir(String texto) {
        return texto.length() <= 70 ? texto : texto.substring(0, 67) + "...";
    }
}
