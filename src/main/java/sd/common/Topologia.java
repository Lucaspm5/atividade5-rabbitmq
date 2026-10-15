package sd.common;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class Topologia {

    public static final String EXCHANGE = "mensagens.topic";
    public static final String DLX = "mensagens.dlx";

    public static final String RK_REVIEW = "review.product";
    public static final String RK_SUPORTE = "support.request";

    public static final String FILA_AVALIACOES = "fila.avaliacoes";
    public static final String FILA_SUPORTE = "fila.suporte";
    public static final String DLQ_AVALIACOES = "fila.avaliacoes.dlq";
    public static final String DLQ_SUPORTE = "fila.suporte.dlq";

    private Topologia() {}

    public static void declarar(Channel canal) throws IOException {
        canal.exchangeDeclare(EXCHANGE, BuiltinExchangeType.TOPIC, true);
        canal.exchangeDeclare(DLX, BuiltinExchangeType.DIRECT, true);

        declararFilaComDlq(canal, FILA_AVALIACOES, DLQ_AVALIACOES, RK_REVIEW);
        declararFilaComDlq(canal, FILA_SUPORTE, DLQ_SUPORTE, RK_SUPORTE);
    }

    private static void declararFilaComDlq(Channel canal, String fila, String dlq, String routingKey)
            throws IOException {
        // Dead Letter Queue (a routing key na DLX é o próprio nome da DLQ)
        canal.queueDeclare(dlq, true, false, false, null);
        canal.queueBind(dlq, DLX, dlq);

        // Fila principal: mensagens rejeitadas (nack sem requeue) vão para a DLX
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", DLX);
        args.put("x-dead-letter-routing-key", dlq);
        canal.queueDeclare(fila, true, false, false, args);
        canal.queueBind(fila, EXCHANGE, routingKey);
    }
}
