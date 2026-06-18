# Subiectul 14 — Okazii cu evaluări 1–5

Proiect Kotlin/JVM, bazat pe aplicația din Laboratorul 7, care păstrează licitația reactivă și adaugă primirea unei evaluări a serviciului între 1 și 5. Numele utilizatorului și evaluarea sunt persistate local în `data/ratings.csv`.

## Ce conține

- `AuctioneerMicroservice` — primește ofertele timp de 15 secunde și anunță rezultatul.
- `MessageProcessorMicroservice` — elimină duplicatele cu `distinct`, apoi sortează ofertele după timestamp.
- `BiddingProcessorMicroservice` — alege oferta maximă.
- `BidderMicroservice` — licitează, primește rezultatul și trimite numele + evaluarea.
- `RatingMicroservice` — validează scorul 1–5 și scrie în fișierul CSV.
- `message-library` — contractul comun `Message`.

Porturi: 1500 bidder→auctioneer, 1501 bidding-processor→auctioneer, 1600 auctioneer→message-processor, 1700 message-processor→bidding-processor, 1800 bidder→rating-service.

## Instalare pe Debian (mașina virtuală)

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk maven graphviz
java -version
mvn -version
```

JDK 17 este alegerea recomandată. Proiectul nu are nevoie de IntelliJ ca să ruleze.

## Compilare

```bash
cd subiect14-okazii-rating
chmod +x scripts/*.sh
./scripts/build.sh
```

Rezultatul apare în `dist/` sub forma a cinci JAR-uri executabile, fiecare cu toate dependențele incluse.

## Rulare manuală — exact în ordinea corectă

Deschide 6 terminale în directorul proiectului.

Terminal 1:
```bash
java -jar dist/RatingMicroservice.jar
```

Terminal 2:
```bash
java -jar dist/MessageProcessorMicroservice.jar
```

Terminal 3:
```bash
java -jar dist/BiddingProcessorMicroservice.jar
```

Terminal 4:
```bash
java -jar dist/AuctioneerMicroservice.jar
```

În maximum 15 secunde, pornește bidderii în alte terminale:
```bash
java -jar dist/BidderMicroservice.jar --name "Daria" --rating 5
java -jar dist/BidderMicroservice.jar --name "Mihai" --rating 4
```

Fără `--rating`, aplicația cere evaluarea de la tastatură:
```bash
java -jar dist/BidderMicroservice.jar --name "Daria"
```

Verificare:
```bash
cat data/ratings.csv
```

Exemplu:
```text
timestampEpochMillis,username,rating
1710000000000,"Daria",5
```

Fișier alternativ:
```bash
RATINGS_FILE=/tmp/evaluari.csv java -jar dist/RatingMicroservice.jar
```

## Demo automat

```bash
./scripts/run-demo.sh
cat data/ratings.csv
cat logs/auctioneer.log
```

## Diagrame

Fișierele gata generate sunt în `diagrams/`:
- `microservices.png` — diagrama microserviciilor și a straturilor;
- `classes.png` — diagrama de clase;
- sursele Graphviz `.dot`, ușor de modificat.

Regenerare:
```bash
dot -Tpng diagrams/microservices.dot -o diagrams/microservices.png
dot -Tpng diagrams/classes.dot -o diagrams/classes.png
```

## Cum explici SOLID la examen

**S — Single Responsibility.** Fiecare microserviciu are o singură responsabilitate de business: colectare, curățare/ordonare, alegerea câștigătorului, ofertare sau salvarea evaluării. În serviciul de rating, validarea, orchestrarea și persistența sunt clase separate.

**O — Open/Closed.** `RatingApplicationService` poate primi altă implementare de `RatingRepository` (JSON, bază de date, cloud) fără modificarea regulilor de validare sau a protocolului de rețea.

**L — Liskov Substitution.** Orice implementare corectă a `RatingRepository` poate înlocui `CsvRatingRepository` fără să schimbe comportamentul serviciului de aplicație.

**I — Interface Segregation.** `RatingRepository` are o interfață mică, doar `save`, fără metode inutile pentru clientul actual.

**D — Dependency Inversion.** `RatingApplicationService` depinde de abstracția `RatingRepository`, nu direct de fișier. `RatingMicroservice` compune implementările concrete la marginea aplicației.

La nivel de microservicii, cuplarea este redusă: ele comunică prin contractul `Message` și TCP, nu își accesează clasele interne sau fișierele reciproc.

## Scenariu de prezentare

1. Arată `diagrams/microservices.png` și explică traseul unei oferte și traseul unei evaluări.
2. Arată `diagrams/classes.png`, insistând pe `RatingRepository`, `CsvRatingRepository`, `RatingApplicationService` și `RatingValidator`.
3. Rulează serviciile în ordinea de mai sus.
4. Pornește doi bidderi și arată rezultatul licitației.
5. Rulează `cat data/ratings.csv` pentru dovada persistenței.
6. În cod, arată `distinct` și `sortedBy` din MessageProcessor, validarea `score in 1..5`, apoi scrierea append în CSV.

## Observații importante

- Ratingul invalid este respins și nu se scrie în fișier.
- Scrierea CSV este sincronizată pentru cereri concurente.
- Duplicatul trimis intenționat de bidder este eliminat pe baza cheii complete a mesajului.
- Pentru simplitatea cerută la examen, comunicația este TCP locală (`localhost`). Pe mai multe VM-uri, hosturile se pot extrage în variabile de mediu.
