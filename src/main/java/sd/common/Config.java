package sd.common;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public final class Config {

    private Config() {}

    public static String env(String nome, String padrao) {
        String valor = System.getenv(nome);
        return (valor == null || valor.isBlank()) ? padrao : valor;
    }

    public static int envInt(String nome, int padrao) {
        return Integer.parseInt(env(nome, String.valueOf(padrao)));
    }

    public static double envDouble(String nome, double padrao) {
        return Double.parseDouble(env(nome, String.valueOf(padrao)));
    }

    /** Conecta ao RabbitMQ; se ele ainda estiver subindo, tenta de novo a cada 3 s. */
    public static Connection conectar() throws InterruptedException {
        ConnectionFactory fabrica = new ConnectionFactory();
        fabrica.setHost(env("RABBITMQ_HOST", "localhost"));
        fabrica.setPort(envInt("RABBITMQ_PORT", 5672));
        fabrica.setUsername(env("RABBITMQ_USER", "guest"));
        fabrica.setPassword(env("RABBITMQ_PASS", "guest"));
        fabrica.setAutomaticRecoveryEnabled(true);
        fabrica.setNetworkRecoveryInterval(3000);

        while (true) {
            try {
                return fabrica.newConnection();
            } catch (Exception e) {
                System.out.println("[conexao] RabbitMQ indisponivel (" + e.getMessage() + "). Nova tentativa em 3s...");
                Thread.sleep(3000);
            }
        }
    }
}
