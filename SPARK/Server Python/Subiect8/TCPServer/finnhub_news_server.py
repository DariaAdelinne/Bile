#!/usr/bin/env python3
"""
finnhub_news_server.py - Server TCP Python.

Preia simbolurile bursiere de pe bursa US (max 20),
apoi pentru fiecare simbol preia stirile din ziua curenta
si le trimite prin socket, cate o stire la 3 secunde.

Serializare: json.dumps (o linie JSON per stire)

Rulare:
  python3 finnhub_news_server.py
"""

import json
import socket
import time
import urllib.request
import urllib.parse
from datetime import datetime

API_TOKEN = "brmr2kfrh5rcss140jmg"
FINNHUB_BASE = "https://finnhub.io/api/v1"
TCP_HOST = "0.0.0.0"
TCP_PORT = 9999
DELAY_SEC = 3
MAX_SYMBOLS = 20


def fetch_symbols(exchange: str = "US") -> list:
    params = urllib.parse.urlencode({"exchange": exchange, "token": API_TOKEN})
    url = f"{FINNHUB_BASE}/stock/symbol?{params}"
    print(f"Se preiau simbolurile de pe bursa {exchange} ...")
    with urllib.request.urlopen(url) as resp:
        data = json.loads(resp.read().decode("utf-8"))
    symbols = [item["symbol"] for item in data[:MAX_SYMBOLS]]
    print(f"Simboluri preluate: {len(symbols)}")
    return symbols


def fetch_today_news(symbol: str) -> list:
    from datetime import timedelta
    today = datetime.now()
    week_ago = today - timedelta(days=7)
    date_from = week_ago.strftime("%Y-%m-%d")
    date_to = today.strftime("%Y-%m-%d")

    params = urllib.parse.urlencode({
        "symbol": symbol,
        "from": date_from,
        "to": date_to,
        "token": API_TOKEN
    })
    url = f"{FINNHUB_BASE}/company-news?{params}"
    try:
        with urllib.request.urlopen(url) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except Exception as e:
        print(f"  Eroare la preluarea stirilor pentru {symbol}: {e}")
        return []


def handle_client(conn: socket.socket, addr, all_articles: list):
    print(f"Client conectat: {addr[0]}:{addr[1]}")
    try:
        for article in all_articles:
            line = json.dumps(article) + "\n"
            conn.sendall(line.encode("utf-8"))
            print(f"  Trimis: {article.get('headline', '')[:60]}...")
            time.sleep(DELAY_SEC)
    except (BrokenPipeError, ConnectionResetError):
        print("Clientul s-a deconectat prematur.")
    finally:
        conn.close()
        print("Conexiune inchisa.")


def main():
    symbols = fetch_symbols()
    if not symbols:
        print("Nu s-au putut prelua simboluri. Verifica token-ul API.")
        return

    # preluam toate stirile de azi pentru toate simbolurile
    all_articles = []
    for symbol in symbols:
        articles = fetch_today_news(symbol)
        if articles:
            print(f"  {symbol}: {len(articles)} stire(i)")
            all_articles.extend(articles)

    print(f"\nTotal stiri de trimis: {len(all_articles)}")
    if not all_articles:
        print("Nu s-au gasit stiri pentru ziua de azi. Incearca cu o zi anterioara.")
        return

    srv = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    srv.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    srv.bind((TCP_HOST, TCP_PORT))
    srv.listen(1)
    print(f"\nServer pornit pe portul {TCP_PORT}. Astept conexiuni...")

    while True:
        conn, addr = srv.accept()
        handle_client(conn, addr, all_articles)


if __name__ == "__main__":
    main()
