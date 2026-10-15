package sd.consumidor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import sd.common.ClassificadorTexto;
import sd.common.Config;
import sd.common.Mensagem;
import sd.common.Topologia;

public final class ConsumidorBase {

    private ConsumidorBase() {}

    public static void executar(String servico, String fila, String recursoCsv, Map<String, String> nomes)
            throws Exception {

        long atrasoMs = Config.envInt("PROCESSING_DELAY_MS", 1000);
        int prefetch = Config.envInt("PREFETCH", 1);

        System.out.printf("[%s] treinando modelo de IA (Smile) com %s...%n", servico, recursoCsv);
        ClassificadorTexto classificador = new ClassificadorTexto(recursoCsv);
        System.out.printf("[%s] modelo pronto. Atraso por mensagem: %d ms%n", servico, atrasoMs);

        ObjectMapper json = new ObjectMapper();
        Connection conexao = Config.conectar();
        Channel canal = conexao.createChannel();
        Topologia.declarar(canal);
        canal.basicQos(prefetch);

        AtomicLong contador = new AtomicLong();

        DeliverCallback aoReceber = (tag, entrega) -> {
            long deliveryTag = entrega.getEnvelope().getDeliveryTag();
            String corpo = new String(entrega.getBody(), StandardCharsets.UTF_8);
            try {
                Mensagem msg = json.readValue(corpo, Mensagem.class);
                if (msg.id() == null || msg.conteudo() == null || msg.conteudo().isBlank()) {
                    throw new IllegalArgumentException("mensagem sem id ou conteudo");
                }

                Thread.sleep(atrasoMs); // processamento propositalmente lento

                ClassificadorTexto.Resultado r = classificador.classificar(msg.conteudo());
                String nome = nomes.getOrDefault(r.rotulo(), r.rotulo());

                System.out.printf(Locale.ROOT, "[%s] #%d id=%s | \"%s\" => %s (confianca %.2f)%n",
                        servico, contador.incrementAndGet(), msg.id().substring(0, 8),
                        resumir(msg.conteudo()), nome, r.confianca());

                canal.basicAck(deliveryTag, false); // ACK manual
            } catch (Exception e) {
                System.out.printf("[%s] ERRO ao processar (%s: %s) -> enviando para a DLQ%n",
                        servico, e.getClass().getSimpleName(), primeiraLinha(e.getMessage()));
                canal.basicNack(deliveryTag, false, false); // requeue=false -> Dead Letter Queue
            }
        };

        canal.basicConsume(fila, false, aoReceber, consumerTag -> { });
        System.out.printf("[%s] aguardando mensagens na fila '%s'...%n", servico, fila);
        new CountDownLatch(1).await();
    }

    private static String resumir(String texto) {
        return texto.length() <= 80 ? texto : texto.substring(0, 77) + "...";
    }

    private static String primeiraLinha(String texto) {
        if (texto == null) {
            return "sem detalhes";
        }
        String linha = texto.split("\\R", 2)[0];
        return linha.length() <= 90 ? linha : linha.substring(0, 87) + "...";
    }
}
