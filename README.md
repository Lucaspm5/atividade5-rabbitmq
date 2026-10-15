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

# Evidências de execução

<img width="1893" height="983" alt="Captura de tela 2026-10-15 195920" src="https://github.com/user-attachments/assets/d6c6fb4c-cc53-49fa-b5d7-f72bdd678fae" />
<img width="1908" height="463" alt="Captura de tela 2026-10-15 195800" src="https://github.com/user-attachments/assets/c0932436-cfe7-4ac4-bd41-ec899e1abca0" />
<img width="1907" height="520" alt="Captura de tela 2026-10-15 195508" src="https://github.com/user-attachments/assets/0ce6048a-d33f-4cd5-be3d-86db040a072c" />
<img width="1916" height="828" alt="Captura de tela 2026-10-15 195432" src="https://github.com/user-attachments/assets/35d6237d-8d12-47bb-b5f5-09f31634f096" />
<img width="1121" height="124" alt="Captura de tela 2026-10-15 195124" src="https://github.com/user-attachments/assets/3791fa53-b6b1-4022-8b23-e0ef90a6310d" />
<img width="1357" height="307" alt="Captura de tela 2026-10-15 195103" src="https://github.com/user-attachments/assets/d767f78b-342b-4055-bf0e-9f34d451971a" />
<img width="1351" height="743" alt="Captura de tela 2026-10-15 194922" src="https://github.com/user-attachments/assets/24a86fb8-9c77-42d7-a146-cc2c3efa135a" />
<img width="1355" height="742" alt="Captura de tela 2026-10-15 194901" src="https://github.com/user-attachments/assets/3fd1cac7-6b1a-4613-b1eb-43b026f8ebfe" />
<img width="1358" height="317" alt="Captura de tela 2026-10-15 194826" src="https://github.com/user-attachments/assets/b878e226-daec-4110-8a46-81ea26c3525d" />



## Demonstração e evidências

Capturas da interface do RabbitMQ em `docs/evidencias/`.

Vídeo de demonstração: [LINK_DO_VIDEO](https://youtu.be/pO6R0aV3ZMQ)
