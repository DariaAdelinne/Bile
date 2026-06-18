#!/usr/bin/env python3
"""
finnhub_profile_server.py - Server TCP Python.

Preia simbolurile bursiere de pe bursa US (max 20),
apoi pentru fiecare simbol preia profilul companiei via /stock/profile2
si trimite datele prin socket, cate un profil la 3 secunde.

Serializare: json.dumps (o linie JSON per profil)

Rulare:
  python3 finnhub_profile_server.py
"""

import json
import socket
import time
import urllib.request
import urllib.parse

API_TOKEN = "brmr7v7rh5rcss140lq0"
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


def fetch_profile(symbol: str) -> dict:
    params = urllib.parse.urlencode({"symbol": symbol, "token": API_TOKEN})
    url = f"{FINNHUB_BASE}/stock/profile2?{params}"
    try:
        with urllib.request.urlopen(url) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except Exception as e:
        print(f"  Eroare la preluarea profilului pentru {symbol}: {e}")
        return {}


def handle_client(conn: socket.socket, addr, profiles: list):
    print(f"Client conectat: {addr[0]}:{addr[1]}")
    try:
        for profile in profiles:
            line = json.dumps(profile) + "\n"
            conn.sendall(line.encode("utf-8"))
            print(f"  Trimis: {profile.get('name', '?')} ({profile.get('ticker', '?')})")
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

    profiles = []
    for symbol in symbols:
        profile = fetch_profile(symbol)
        # ignoram profilurile goale (unele simboluri nu au date)
        if profile and profile.get("name"):
            profiles.append(profile)
            print(f"  {symbol}: {profile.get('name', '?')}")

    print(f"\nTotal profiluri de trimis: {len(profiles)}")
    if not profiles:
        print("Nu s-au gasit profiluri valide.")
        return

    srv = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    srv.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    srv.bind((TCP_HOST, TCP_PORT))
    srv.listen(1)
    print(f"\nServer pornit pe portul {TCP_PORT}. Astept conexiuni...")

    while True:
        conn, addr = srv.accept()
        handle_client(conn, addr, profiles)


if __name__ == "__main__":
    main()
