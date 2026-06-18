# Subiect 7 - Finnhub TCP Server + PySpark Stream

## Structura
- `FinnhubTCPServer/` — server Kotlin/Maven care apeleaza API-ul Finnhub si trimite date pe TCP
- `SparkStreamClient/` — script Python cu PySpark care consuma fluxul TCP

## 1. Porneste serverul Kotlin

### In IntelliJ
1. Deschide `FinnhubTCPServer/` ca proiect Maven
2. Gradle panel → Maven → `package` (sau `mvn package`)
3. Run → `FinnhubTCPServerKt`

### In terminal (Debian/Linux)
```bash
cd FinnhubTCPServer
mvn package -DskipTests
java -jar target/FinnhubTCPServer-1.0-SNAPSHOT-jar-with-dependencies.jar
```

Serverul afiseaza:
```
Se preiau simbolurile de pe bursa: US ...
Simboluri preluate: 30
FinnhubTCPServer pornit pe portul 9999. Astept conexiuni...
```

## 2. Porneste clientul PySpark

### Instalare dependente (o singura data)
```bash
pip install pyspark==3.2.0
```

### Rulare
```bash
cd SparkStreamClient
python3 finnhub_spark_stream.py
```

Sau pe Windows:
```bash
python finnhub_spark_stream.py
```

## Cum functioneaza

1. **Serverul Kotlin**:
   - Apeleaza `GET /stock/symbol?exchange=US` → obtine lista simbolurilor (max 30)
   - Pentru fiecare simbol apeleaza `GET /stock/price-target` → preia `targetMean`, `targetLow` etc.
   - Serialzeaza cu `kotlinx.serialization` la JSON si trimite linie cu linie prin TCP
   - Pauza de 3 secunde intre companii (respecta limita API de 60 req/min)

2. **Clientul PySpark**:
   - `socketTextStream("localhost", 9999)` → flux de date direct (direct stream)
   - Fiecare batch (3s) contine 0-1 randuri JSON
   - `foreachRDD` → calculeaza `profit_mediu = (targetMean - targetLow) / targetLow * 100`
   - Filtreaza companiile cu profit > 40%
   - Afiseaza simbolul si procentul de profit

## Output exemplu
```
==================================================
Companii cu profit mediu estimat > 40.0%:
  AAPL       profit mediu: 45.32%
  TSLA       profit mediu: 67.81%
==================================================
```
