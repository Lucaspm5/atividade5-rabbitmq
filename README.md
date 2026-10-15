# Atividade 5 – Sistema Distribuído de Classificação Inteligente de Mensagens com RabbitMQ

Sistema distribuído em Java, Docker e RabbitMQ que gera, distribui e classifica mensagens
automaticamente por meio de dois consumidores independentes, cada um com sua própria IA embarcada (Smile).

## Arquitetura

~~~
+-----------------------+       +-----------------------------------------+
| Container 1: gerador  | ----> | Container 2: RabbitMQ                   |
| (Java, >= 5 msg/s)    |       | Topic Exchange: mensagens.topic         |
+-----------------------+       |  review.product  -> fila.avaliacoes     |
                                |  support.request -> fila.suporte        |
                                +-------------+---------------+-----------+
                                              |               |
                                              v               v
                         +---------------------------+  +---------------------------+
                         | Container 3: consumidor 1 |  | Container 4: consumidor 2 |
                         | IA 1: análise de sentimento| | IA 2: categoria do problema|
                         +---------------------------+  +---------------------------+
~~~

Todos os containers estão na rede Docker `sd-net` e são configurados pelo `docker-compose.yml`.

## Mensagens

JSON com identificador único (UUID), data/hora de geração (ISO-8601), tipo (`review` ou `support`) e conteúdo.
O tipo também é identificado pela routing key (`review.product` ou `support.request`).

## RabbitMQ

- Exchange `mensagens.topic` (tipo topic, durável).
- `fila.avaliacoes` ligada à routing key `review.product`; `fila.suporte` ligada à `support.request`.
- Dead Letter Exchange `mensagens.dlx` com as filas `fila.avaliacoes.dlq` e `fila.suporte.dlq`.
- Confirmação manual (manual ACK): o consumidor só confirma após classificar a mensagem.
  Em caso de erro, faz `basicNack` com `requeue=false` e a mensagem vai para a DLQ.
- Interface de administração em http://localhost:15672 (usuário `admin`, senha `admin123`).

## Inteligência Artificial (biblioteca Smile)

Cada consumidor treina, na inicialização, uma **Regressão Logística multinomial**
(`smile.classification.LogisticRegression`) a partir de um dataset embarcado em CSV.

- Pré-processamento: minúsculas, remoção de acentos, tokenização e vetor de contagem de termos (bag-of-words).
- **IA 1 (sentimento):** `sentimento_treino.csv`; classes positiva, negativa e neutra.
- **IA 2 (suporte):** `suporte_treino.csv`; classes problema de acesso, de desempenho e de conexão.
- Os dois modelos são independentes: cada container tem seu vocabulário e seu classificador.

## Como executar

Pré-requisitos: Docker e Docker Compose.

~~~
docker compose up --build -d
docker compose logs -f gerador consumidor-sentimento consumidor-suporte
~~~

Para encerrar e limpar: `docker compose down -v`.

Variáveis de ambiente (no `docker-compose.yml`):

| Variável | Serviço | Padrão | Descrição |
|---|---|---|---|
| MESSAGES_PER_SECOND | gerador | 10 | Taxa de geração (mínimo 5) |
| ERROR_RATE | gerador | 0.05 | Fração de mensagens corrompidas, para exercitar a DLQ |
| PROCESSING_DELAY_MS | consumidores | 1000 | Atraso por mensagem, para as filas crescerem |
| PREFETCH | consumidores | 1 | Mensagens não confirmadas por consumidor |

## Demonstração e evidências

Capturas da interface do RabbitMQ em `docs/evidencias/`.

Vídeo de demonstração: [LINK_DO_VIDEO](https://youtu.be/pO6R0aV3ZMQ)
