import json
import os

from pyspark import SparkContext
from pyspark.streaming import StreamingContext

os.environ["PYSSPARK_PYTHON"] = "python3"
os.environ["PYSPARK_DRIVER_PYTHON"] = "python3"
os.environ["JAVA_HOME"] = "/usr/lib/jvm/java-11-openjdk-amd64/bin/javac"

# Prag de toleranta pentru clasificare neutra
TOLERANCE = 0.05


def classify_sentiment(news_item, positive_words_bc, negative_words_bc):
    """
    Clasifica o stire ca pozitiva/negativa/neutra pe baza cuvintelor
    din summary/headline, folosind RDD-urile de cuvinte incarcate.

    Returneaza un dict cu headline, summary si sentiment.
    """
    positive_words = positive_words_bc.value
    negative_words = negative_words_bc.value

    # Preia textul stirii (summary sau headline)
    text = news_item.get("summary", news_item.get("headline", "")).lower()
    words = text.split()

    if not words:
        return {
            "headline": news_item.get("headline", "N/A"),
            "sentiment": "NEUTRU (text gol)"
        }

    pos_count = sum(1 for w in words if w in positive_words)
    neg_count = sum(1 for w in words if w in negative_words)

    pos_pct = pos_count / len(words)
    neg_pct = neg_count / len(words)

    if abs(pos_pct - neg_pct) < TOLERANCE:
        sentiment = "NEUTRU"
    elif pos_pct > neg_pct:
        sentiment = "POZITIV"
    else:
        sentiment = "NEGATIV"

    return {
        "headline": news_item.get("headline", "N/A"),
        "summary": text[:100] + "..." if len(text) > 100 else text,
        "pos_words": pos_count,
        "neg_words": neg_count,
        "sentiment": sentiment
    }


def print_results(rdd):
    items = rdd.collect()
    if items:
        print("\n--- {} stiri clasificate in acest RDD ---".format(len(items)))
        for item in items:
            print("  Titlu:    {}".format(item["headline"]))
            print("  Sentiment: {}  (pos={}, neg={})".format(
                item["sentiment"],
                item.get("pos_words", 0),
                item.get("neg_words", 0)
            ))
            print()


def main():
    context = SparkContext(appName="AnalizaSentimente", master="local[*]")
    context.setLogLevel("WARN")

    # Incarca cuvintele pozitive/negative ca RDD-uri (conform cerintei - Lab 14)
    positive_rdd = context.textFile("positive-words.txt")
    negative_rdd = context.textFile("negative-words.txt")

    # Filtreaza liniile de comentarii (incep cu ';' sau '#') si spatiile goale
    positive_words = set(
        positive_rdd
        .filter(lambda line: line.strip() and not line.startswith(";") and not line.startswith("#"))
        .map(lambda w: w.strip().lower())
        .collect()
    )
    negative_words = set(
        negative_rdd
        .filter(lambda line: line.strip() and not line.startswith(";") and not line.startswith("#"))
        .map(lambda w: w.strip().lower())
        .collect()
    )

    print("Cuvinte pozitive incarcate: {}".format(len(positive_words)))
    print("Cuvinte negative incarcate: {}".format(len(negative_words)))

    # Broadcast catre toti workerii Spark
    positive_words_bc = context.broadcast(positive_words)
    negative_words_bc = context.broadcast(negative_words)

    ssc = StreamingContext(context, 5)
    raw_stream = ssc.socketTextStream("localhost", 8888)

    classified_stream = raw_stream \
        .map(lambda line: json.loads(line)) \
        .map(lambda news: classify_sentiment(news, positive_words_bc, negative_words_bc))

    classified_stream.foreachRDD(lambda rdd: print_results(rdd))

    ssc.start()
    ssc.awaitTermination()


if __name__ == "__main__":
    main()
