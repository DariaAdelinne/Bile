# Cum rulez — bilet_47 (SPSD-64)

## Cerinte

- Docker + Docker Compose (pentru Kafka)
- Java 17 + Gradle

## Pasul 1 — Porneste Kafka cu Docker

```bash
cd bilet_47
docker compose up -d
```

Asteapta ~10 secunde sa porneasca Kafka si Zookeeper.

## Pasul 2 — Porneste aplicatia Spring Boot

```bash
./gradlew bootRun
```

Sau din IntelliJ: deschide folderul `bilet_47` ca proiect Gradle, ruleaza `AutomatonApp.kt`.

## Pasul 3 — Trimite un sir de biti spre procesare

```bash
curl -X POST http://localhost:8080/automaton/submit \
     -H "Content-Type: application/json" \
     -d '{"bits": "0110"}'
```

Raspuns:
```json
{
  "id": "a3f7c2b1",
  "bits": "0110",
  "message": "Trimis in Kafka. Verifica rezultatul la GET /automaton/result/a3f7c2b1"
}
```

## Pasul 4 — Verifica rezultatul

```bash
curl http://localhost:8080/automaton/result/a3f7c2b1
```

Raspuns (dupa procesare):
```json
{
  "id": "a3f7c2b1",
  "bits": "0110",
  "output": 1,
  "done": true,
  "finalState": "S11",
  "history": [
    "[...] S00 --0--> S00",
    "[...] S00 --1--> S01",
    "[...] S01 --1--> S11",
    "[...] Output=1 (toate bitii procesati)"
  ]
}
```

## Toate rezultatele

```bash
curl http://localhost:8080/automaton/results
```

## Exemple de teste

| Sir de biti | Output asteptat | Explicatie               |
|-------------|-----------------|--------------------------|
| `"0110"`    | 1               | contine "11"             |
| `"0101"`    | 0               | nu contine "11"          |
| `"111"`     | 1               | contine "11"             |
| `"000"`     | 0               | niciun 1                 |
| `"01011"`   | 1               | contine "11" la sfarsit  |

## Oprire

```bash
docker compose down
```

## Automatul implementat (diagrama)

```
Reset → S00 --1--> S01 --1--> S11 (output=1)
         |          |
         0          0
         |          ↓
         ↓        S10 --1--> S01
        S00        |
                   0
                   ↓
                  S00
```

Detecteaza secventa "11" (doi de 1 consecutivi) in sirul de biti primit.
