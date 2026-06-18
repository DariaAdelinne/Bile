# Subiectul 22 — Okazii cu procesor de flux pentru statistici

Proiect Kotlin/JVM cu microservicii reactive. Păstrează fluxul Okazii și adaugă `StatisticsStreamProcessorMicroservice`, care primește evenimente despre fiecare mesaj de rețea, calculează numărul total de mesaje până la adjudecare și scrie rezultatul în `data/auction-statistics.csv`.

## Formula folosită

Pentru `N` bidderi, sunt numărate mesajele reale de rețea:

- `N` oferte Bidder → Auctioneer;
- `N` oferte Auctioneer → MessageProcessor;
- `1` mesaj `final` Auctioneer → MessageProcessor;
- `1` confirmare MessageProcessor → Auctioneer;
- `N` oferte MessageProcessor → BiddingProcessor;
- `1` mesaj `final` MessageProcessor → BiddingProcessor;
- `1` rezultat BiddingProcessor → Auctioneer;
- `N` rezultate Auctioneer → bidderi.

Total: `4 × N + 4`. Pentru 5 bidderi rezultatul este `24` mesaje.

## Compilare

```bash
chmod +x scripts/*.sh
./scripts/build.sh
```

## Rulare manuală

Deschide terminale separate, din rădăcina proiectului.

1. Procesor statistici:

```bash
java -jar dist/StatisticsStreamProcessorMicroservice.jar
```

2. MessageProcessor:

```bash
java -jar dist/MessageProcessorMicroservice.jar
```

3. BiddingProcessor:

```bash
java -jar dist/BiddingProcessorMicroservice.jar
```

4. Auctioneer:

```bash
java -jar dist/AuctioneerMicroservice.jar
```

5. Imediat, în maximum 15 secunde, pornește 5 bidderi:

```bash
java -jar dist/BidderMicroservice.jar --name Daria &
java -jar dist/BidderMicroservice.jar --name Mihai &
java -jar dist/BidderMicroservice.jar --name Ioana &
java -jar dist/BidderMicroservice.jar --name Andrei &
java -jar dist/BidderMicroservice.jar --name Alex &
wait
```

La final, în terminalul de statistici trebuie să apară:

```text
[Statistics] ADJUDECATĂ licitatie-1; total mesaje=24
[Statistics] salvat în data/auction-statistics.csv
```

Verificare fișier:

```bash
cat data/auction-statistics.csv
```

Exemplu:

```csv
auctionId,startedAt,adjudicatedAt,totalMessages,bids,bidToMessageProcessor,bidsEnd,acks,processedBids,processedEnd,resultToAuctioneer,resultToBidders
licitatie-1,1781690000000,1781690015000,24,5,5,1,1,5,1,1,5
```

## Demo automat

```bash
./scripts/run-demo.sh
```

## Clase importante

- `StatisticsEvent` — eveniment statistic;
- `StatisticsEventType` — tipurile de mesaje numărate;
- `StatisticsClient` — trimite evenimente către procesor;
- `StatisticsStreamProcessorMicroservice` — primește fluxul;
- `AuctionStatisticsService` — agregă și calculează totalul;
- `AuctionStatisticsRepository` — abstracția de persistență;
- `CsvAuctionStatisticsRepository` — scrie local în CSV;
- `AuctionStatisticsSummary` — rezultatul statistic.

## SOLID

- **S**: fiecare clasă are un singur rol: transport, agregare, persistență sau model.
- **O**: se poate adăuga un repository JSON/DB fără modificarea serviciului de agregare.
- **L**: orice implementare a `AuctionStatisticsRepository` poate înlocui repository-ul CSV.
- **I**: interfața repository are doar metoda necesară `save`.
- **D**: `AuctionStatisticsService` depinde de interfața repository, nu de fișierul CSV.

## Diagrame

- `diagrams/microservices.png`
- `diagrams/classes.png`
