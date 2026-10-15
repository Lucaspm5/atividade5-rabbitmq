package sd.consumidor;

import java.util.Map;
import sd.common.Topologia;

public class ConsumidorSentimentoApp {
    public static void main(String[] args) throws Exception {
        ConsumidorBase.executar(
                "consumidor-sentimento",
                Topologia.FILA_AVALIACOES,
                "/sentimento_treino.csv",
                Map.of("positiva", "POSITIVA", "negativa", "NEGATIVA", "neutra", "NEUTRA"));
    }
}
