#!/usr/bin/env python3
"""
sentiment_stream.py - Analiza de sentimente cu Spark Streaming + Spark RDD.

Preia stiri de la serverul TCP Kotlin (localhost:9999) si clasifica
fiecare stire ca POZITIV / NEGATIV / NEUTRU pe baza a doua fisiere text
cu cuvinte pozitive si negative, incarcate ca RDD-uri Spark.

Algoritm de clasificare:
  - se tokenizeaza titlul si summary-ul stirii (lowercase, doar litere)
  - se numara cate cuvinte apar in RDD-ul de cuvinte pozitive
  - se numara cate apar in RDD-ul de cuvinte negative
  - daca pos_count > neg_count  => POZITIV
  - daca neg_count > pos_count  => NEGATIV
  - altfel                      => NEUTRU

Rulare:
  python3 sentiment_stream.py
  (serverul TCP Kotlin trebuie sa ruleze inainte)
"""

import json
import os
import re
import sys
from pyspark import SparkContext
from pyspark.streaming import StreamingContext

TCP_HOST = "localhost"
TCP_PORT = 9999
BATCH_INTERVAL = 5   # secunde - sincronizat cu intervalul serverului


def tokenize(text: str):
    """Imparte textul in cuvinte lowercase, elimina caracterele non-alfabetice."""
    return set(re.findall(r"[a-z]+", text.lower()))


def make_classifier(pos_set, neg_set):
    """Returneaza o functie de clasificare care captureaza seturile de cuvinte."""
    def classify(article: dict) -> str:
        text = article.get("headline", "") + " " + article.get("summary", "")
        words = tokenize(text)

        pos_count = len(words & pos_set)
        neg_count = len(words & neg_set)

        if pos_count > neg_count:
            return "POZITIV"
        elif neg_count > pos_count:
            return "NEGATIV"
        else:
            return "NEUTRU"

    return classify


def make_rdd_processor(sc, pos_words_bc, neg_words_bc):
    """
    Returneaza functia foreachRDD care foloseste RDD-urile de cuvinte
    (accesate prin broadcast) pentru clasificarea stirilor.
    """
    def process_rdd(rdd):
        records = rdd.collect()
        if not records:
            return

        articles = []
        for line in records:
            line = line.strip()
            if not line:
                continue
            try:
                articles.append(json.loads(line))
            except json.JSONDecodeError as e:
                print(f"  [WARN] JSON invalid ignorat: {e}")

        if not articles:
            return

        pos_set = pos_words_bc.value
        neg_set = neg_words_bc.value
        classifier = make_classifier(pos_set, neg_set)

        print("=" * 65)
        print(f"Batch: {len(articles)} stire(i) primita(e)")
        print("=" * 65)

        for article in articles:
            sentiment = classifier(article)
            headline = article.get("headline", "(fara titlu)")[:60]
            source   = article.get("source", "?")
            symbol   = article.get("related", article.get("symbol", "?"))

            sentiment_label = {
                "POZITIV": "[+] POZITIV",
                "NEGATIV": "[-] NEGATIV",
                "NEUTRU":  "[~] NEUTRU ",
            }.get(sentiment, sentiment)

            print(f"  {sentiment_label} | {symbol:<6s} | {source:<15s} | {headline}")

        print("=" * 65)

    return process_rdd


def main():
    os.environ.setdefault("PYSPARK_PYTHON", sys.executable)

    sc = SparkContext("local[2]", "FinnhubSentimentStream")
    sc.setLogLevel("ERROR")

    root = os.path.abspath(os.path.dirname(__file__))
    pos_path = os.path.join(root, "resources", "positive_words.txt")
    neg_path = os.path.join(root, "resources", "negative_words.txt")

    # incarcam fisierele de cuvinte ca RDD-uri Spark
    positive_rdd = sc.textFile(pos_path).map(lambda w: w.strip().lower()).filter(bool)
    negative_rdd = sc.textFile(neg_path).map(lambda w: w.strip().lower()).filter(bool)

    # convertim RDD-urile in seturi Python si le difuzam (broadcast) catre workeri
    pos_set = set(positive_rdd.collect())
    neg_set = set(negative_rdd.collect())

    pos_words_bc = sc.broadcast(pos_set)
    neg_words_bc = sc.broadcast(neg_set)

    print(f"Cuvinte pozitive incarcate (RDD): {len(pos_set)}")
    print(f"Cuvinte negative incarcate (RDD): {len(neg_set)}")

    # StreamingContext cu batch interval de 5 secunde
    ssc = StreamingContext(sc, BATCH_INTERVAL)

    # flux de date direct (direct stream) de la serverul TCP Kotlin
    lines = ssc.socketTextStream(TCP_HOST, TCP_PORT)

    # aplica clasificarea pe fiecare RDD din stream
    lines.foreachRDD(make_rdd_processor(sc, pos_words_bc, neg_words_bc))

    ssc.start()
    print(f"Stream PySpark pornit. Astept stiri de la {TCP_HOST}:{TCP_PORT} ...")
    ssc.awaitTermination()


if __name__ == "__main__":
    main()
