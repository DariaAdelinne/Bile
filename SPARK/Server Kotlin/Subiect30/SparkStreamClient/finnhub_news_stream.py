#!/usr/bin/env python3
"""
finnhub_news_stream.py - Flux de date direct (direct stream) cu PySpark.

Preia stiri de la FinnhubNewsTCPServer (localhost:9999) si le filtreaza:
  1. Se pastreaza doar stirile care folosesc o imagine PNG (.png in URL-ul imaginii)
  2. Se pastreaza doar stirile al caror URL nu depaseste 80 de caractere
  3. Se afiseaza URL-ul, data si titlul fiecarei stiri care trece filtrele

Rulare:
  python3 finnhub_news_stream.py
  (serverul Kotlin trebuie sa ruleze inainte pe portul 9999)
"""

import json
import os
import sys
import datetime
from pyspark import SparkContext
from pyspark.streaming import StreamingContext

TCP_HOST = "localhost"
TCP_PORT = 9999
BATCH_INTERVAL = 3   # secunde - sincronizat cu intervalul serverului
MAX_URL_LENGTH = 80


def unix_to_date(ts):
    """Converteste un Unix timestamp la un string data lizibil."""
    try:
        return datetime.datetime.utcfromtimestamp(int(ts)).strftime("%Y-%m-%d %H:%M UTC")
    except Exception:
        return "data necunoscuta"


def process_rdd(rdd):
    """Proceseaza fiecare RDD: filtreaza stirile dupa imagine PNG si lungime URL."""
    records = rdd.collect()
    if not records:
        return

    filtered = []
    for line in records:
        line = line.strip()
        if not line:
            continue
        try:
            article = json.loads(line)

            image_url = article.get("image", "")
            news_url  = article.get("url", "")
            headline  = article.get("headline", "")
            datetime_ = article.get("datetime", 0)

            # Filtru 1: imaginea trebuie sa fie PNG
            if not image_url.lower().endswith(".png"):
                continue

            # Filtru 2: URL-ul stirii nu depaseste 80 de caractere
            if len(news_url) > MAX_URL_LENGTH:
                continue

            filtered.append({
                "url":      news_url,
                "date":     unix_to_date(datetime_),
                "headline": headline
            })
        except (json.JSONDecodeError, ValueError, KeyError) as e:
            print(f"  [WARN] Linie invalida ignorata: {e}")

    if filtered:
        print("=" * 70)
        print(f"Stiri filtrate (PNG + URL <= {MAX_URL_LENGTH} caractere):")
        for item in filtered:
            print(f"  URL:   {item['url']}")
            print(f"  Data:  {item['date']}")
            print(f"  Titlu: {item['headline']}")
            print("-" * 70)
        print("=" * 70)
    else:
        # afisam un mesaj discret ca am primit ceva dar nimic nu a trecut filtrele
        total = sum(1 for r in records if r.strip())
        if total > 0:
            print(f"[RDD] {total} stire(i) primita(e), niciuna nu indeplineste filtrele (PNG + URL <= {MAX_URL_LENGTH}).")


def main():
    os.environ.setdefault("PYSPARK_PYTHON", sys.executable)

    sc = SparkContext("local[2]", "FinnhubNewsStream")
    sc.setLogLevel("ERROR")

    # StreamingContext cu batch interval de 3 secunde
    ssc = StreamingContext(sc, BATCH_INTERVAL)

    # flux de date direct (direct stream) de la serverul TCP Kotlin
    lines = ssc.socketTextStream(TCP_HOST, TCP_PORT)

    # pentru fiecare RDD din batch se aplica filtrele
    lines.foreachRDD(process_rdd)

    ssc.start()
    print(f"Stream PySpark pornit. Astept stiri de la {TCP_HOST}:{TCP_PORT} ...")
    ssc.awaitTermination()


if __name__ == "__main__":
    main()
