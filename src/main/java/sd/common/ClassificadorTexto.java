package sd.common;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import smile.classification.LogisticRegression;

/**
 * Classificador de texto: bag-of-words (contagem de termos) + Regressão Logística
 * multinomial da biblioteca Smile, treinada na inicialização com um CSV embarcado.
 */
public class ClassificadorTexto {

    public record Resultado(String rotulo, double confianca) {}

    private final Map<String, Integer> vocabulario = new HashMap<>();
    private final List<String> rotulos = new ArrayList<>();
    private final LogisticRegression modelo;

    public ClassificadorTexto(String recursoCsv) throws IOException {
        List<String[]> amostras = lerCsv(recursoCsv);

        for (String[] amostra : amostras) {
            if (!rotulos.contains(amostra[0])) {
                rotulos.add(amostra[0]);
            }
            for (String token : tokenizar(amostra[1])) {
                vocabulario.putIfAbsent(token, vocabulario.size());
            }
        }

        double[][] x = new double[amostras.size()][];
        int[] y = new int[amostras.size()];
        for (int i = 0; i < amostras.size(); i++) {
            x[i] = vetorizar(amostras.get(i)[1]);
            y[i] = rotulos.indexOf(amostras.get(i)[0]);
        }

        this.modelo = LogisticRegression.fit(x, y);
    }

    public Resultado classificar(String texto) {
        double[] x = vetorizar(texto);
        double[] posteriori = new double[rotulos.size()];
        int classe = modelo.predict(x, posteriori);
        return new Resultado(rotulos.get(classe), posteriori[classe]);
    }

    private double[] vetorizar(String texto) {
        double[] v = new double[vocabulario.size()];
        for (String token : tokenizar(texto)) {
            Integer indice = vocabulario.get(token);
            if (indice != null) {
                v[indice] += 1.0;
            }
        }
        return v;
    }

    static List<String> tokenizar(String texto) {
        String t = Normalizer.normalize(texto.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        List<String> tokens = new ArrayList<>();
        for (String parte : t.split("[^a-z0-9]+")) {
            if (parte.length() >= 2) {
                tokens.add(parte);
            }
        }
        return tokens;
    }

    private static List<String[]> lerCsv(String recurso) throws IOException {
        InputStream is = ClassificadorTexto.class.getResourceAsStream(recurso);
        if (is == null) {
            throw new FileNotFoundException("Recurso nao encontrado: " + recurso);
        }
        List<String[]> linhas = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String linha;
            while ((linha = br.readLine()) != null) {
                linha = linha.strip();
                if (linha.isEmpty() || linha.startsWith("#")) {
                    continue;
                }
                int i = linha.indexOf(';');
                linhas.add(new String[] {linha.substring(0, i).strip(), linha.substring(i + 1).strip()});
            }
        }
        return linhas;
    }
}
