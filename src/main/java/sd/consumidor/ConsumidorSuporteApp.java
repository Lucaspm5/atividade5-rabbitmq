package sd.consumidor;

import java.util.Map;
import sd.common.Topologia;

public class ConsumidorSuporteApp {
    public static void main(String[] args) throws Exception {
        ConsumidorBase.executar(
                "consumidor-suporte",
                Topologia.FILA_SUPORTE,
                "/suporte_treino.csv",
                Map.of("acesso", "Problema de acesso",
                       "desempenho", "Problema de desempenho",
                       "conexao", "Problema de conexão"));
    }
}
