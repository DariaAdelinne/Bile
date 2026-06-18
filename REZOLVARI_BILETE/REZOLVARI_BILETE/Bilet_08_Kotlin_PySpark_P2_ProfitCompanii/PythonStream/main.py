import json
import os

from pyspark import SparkContext
from pyspark.streaming import StreamingContext

os.environ["PYSPARK_PYTHON"] = "python3"
os.environ["PYSPARK_DRIVER_PYTHON"] = "python3"
os.environ["JAVA_HOME"] = "/usr/lib/jvm/jdk1.8.0_291"


def calculate_profit(data):
    """
    Calculeaza profitul mediu procentual posibil pe baza preturilor estimate:
      profit = (targetMean - targetLow) / targetLow * 100

    Returneaza un dict cu symbol si profit_pct, sau None daca datele lipsesc.
    """
    try:
        target_mean = float(data.get("targetMean", 0))
        target_low = float(data.get("targetLow", 0))
        symbol = data.get("symbol", "N/A")

        if target_low <= 0:
            return None

        profit_pct = ((target_mean - target_low) / target_low) * 100
        return {"symbol": symbol, "profit_pct": round(profit_pct, 2)}
    except Exception:
        return None


def print_results(rdd):
    items = rdd.collect()
    if items:
        print("\n--- {} companii cu profit > 40% in acest RDD ---".format(len(items)))
        for item in items:
            print("  Simbol: {:10s}  Profit mediu: {:.2f}%".format(
                item["symbol"], item["profit_pct"]
            ))


def main():
    context = SparkContext(appName="ProfitCompanii", master="local[*]")
    context.setLogLevel("WARN")

    ssc = StreamingContext(context, 3)
    raw_stream = ssc.socketTextStream("localhost", 8888)

    profit_stream = raw_stream \
        .map(lambda line: json.loads(line)) \
        \
        .map(lambda data: calculate_profit(data)) \
        \
        .filter(lambda result: result is not None) \
        \
        .filter(lambda result: result["profit_pct"] > 40.0)

    profit_stream.foreachRDD(lambda rdd: print_results(rdd))

    ssc.start()
    ssc.awaitTermination()


if __name__ == "__main__":
    main()
