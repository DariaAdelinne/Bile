#!/usr/bin/env python3
"""
finnhub_spark_stream.py - Flux de date direct (direct stream) cu PySpark.

Preia date de la FinnhubTCPServer (localhost:9999) si calculeaza
profitul mediu posibil per companie pe baza preturilor tinta ale analistilor.

Formula profit mediu:
  profit_mediu = (targetMean - targetLow) / targetLow * 100  [procent]

Filtrare: se pastreaza doar companiile cu profit_mediu > 40%.

Rulare:
  python3 finnhub_spark_stream.py
  (serverul Kotlin trebuie sa ruleze inainte pe portul 9999)
"""

import json
import os
import sys
from pyspark import SparkContext
from pyspark.streaming import StreamingContext

TCP_HOST = "localhost"
TCP_PORT = 9999
BATCH_INTERVAL = 3   # secunde - se sincronizeaza cu intervalul serverului
PROFIT_THRESHOLD = 40.0


def process_rdd(rdd):
    """Proceseaza fiecare RDD din stream: calculeaza profitul si filtreaza."""
    records = rdd.collect()
    if not records:
        return

    results = []
    for line in records:
        line = line.strip()
        if not line:
            continue
        try:
            data = json.loads(line)
            symbol = data.get("symbol", "?")
            target_mean = float(data.get("targetMean", 0))
            target_low = float(data.get("targetLow", 0))

            if target_low <= 0:
                continue

            # profit mediu in luna curenta: diferenta dintre pretul mediu tinta
            # si pretul minim estimat, raportata la pretul minim (ca procent)
            profit_pct = (target_mean - target_low) / target_low * 100.0
            results.append((symbol, profit_pct))
        except (json.JSONDecodeError, ValueError, KeyError) as e:
            print(f"  [WARN] Linie invalida ignorata: {e}")

    # filtrare: profit > 40%
    filtered = [(sym, pct) for sym, pct in results if pct > PROFIT_THRESHOLD]

    if filtered:
        print("=" * 50)
        print(f"Companii cu profit mediu estimat > {PROFIT_THRESHOLD}%:")
        for sym, pct in filtered:
            print(f"  {sym:<10s}  profit mediu: {pct:.2f}%")
        print("=" * 50)
    else:
        symbols_seen = [s for s, _ in results]
        if symbols_seen:
            print(f"[RDD] {len(symbols_seen)} companii procesate, niciuna nu depaseste {PROFIT_THRESHOLD}% profit.")


def main():
    os.environ.setdefault("PYSPARK_PYTHON", sys.executable)

    sc = SparkContext("local[2]", "FinnhubStockStream")
    sc.setLogLevel("ERROR")

    # StreamingContext cu batch interval de 3 secunde (egal cu intervalul serverului)
    ssc = StreamingContext(sc, BATCH_INTERVAL)

    # flux de date direct (direct stream) de la serverul TCP Kotlin
    lines = ssc.socketTextStream(TCP_HOST, TCP_PORT)

    # pentru fiecare RDD din batch se apeleaza process_rdd
    lines.foreachRDD(process_rdd)

    ssc.start()
    print(f"Stream PySpark pornit. Astept date de la {TCP_HOST}:{TCP_PORT} ...")
    ssc.awaitTermination()


if __name__ == "__main__":
    main()
