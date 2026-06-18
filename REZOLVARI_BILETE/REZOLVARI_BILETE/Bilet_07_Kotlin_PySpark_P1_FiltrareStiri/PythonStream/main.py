import json
import os

from pyspark import SparkContext
from pyspark.streaming import StreamingContext

os.environ["PYSPARK_PYTHON"] = "python3"
os.environ["PYSPARK_DRIVER_PYTHON"] = "python3"
# Ajusteaza calea daca JDK-ul e in alta locatie pe Debian VM
os.environ["JAVA_HOME"] = "/usr/lib/jvm/jdk1.8.0_291"


def print_news(rdd):
    """Afiseaza URL, data si titlul pentru fiecare stire din RDD."""
    items = rdd.collect()
    if items:
        print("\n--- {} stiri filtrate in acest RDD ---".format(len(items)))
        for item in items:
            print("  URL:   {}".format(item.get("url", "N/A")))
            print("  Data:  {}".format(item.get("datetime", "N/A")))
            print("  Titlu: {}".format(item.get("headline", "N/A")))
            print()


def main():
    context = SparkContext(appName="FiltrareStiri", master="local[*]")
    context.setLogLevel("WARN")

    # Fereastra de 3 secunde (corespunde intervalului de trimitere din Kotlin)
    ssc = StreamingContext(context, 3)

    # Preia datele de la serverul Kotlin TCP
    raw_stream = ssc.socketTextStream("localhost", 8888)

    filtered_news = raw_stream \
        .map(lambda line: json.loads(line)) \
        \
        .filter(lambda news: news.get("image", "").lower().endswith(".png")) \
        \
        .filter(lambda news: len(news.get("url", "")) <= 80)

    # Afiseaza URL, data si titlul pentru fiecare RDD
    filtered_news.foreachRDD(lambda rdd: print_news(rdd))

    ssc.start()
    ssc.awaitTermination()


if __name__ == "__main__":
    main()
