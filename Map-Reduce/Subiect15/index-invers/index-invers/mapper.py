#!/usr/bin/env python3
"""
MAPPER - Indexul invers (Map-Reduce cu stdin/stdout)

Citeste URL-uri din stdin (cate unul pe linie).
Pentru fiecare URL:
  - descarca pagina cu requests
  - extrage textul cu BeautifulSoup
  - tokenizeaza (cuvinte alfanumerice, lowercase, >= 3 caractere)
  - emite pe stdout perechi de forma:
        <word>\t<URL>:1
    conform specificatiei Map-Reduce.

Utilizare:
    cat urls.txt | python3 mapper.py
"""

import sys
import re
import json
import requests
from bs4 import BeautifulSoup


def fetch_text(url: str) -> str:
    """Descarca pagina si returneaza textul vizibil (fara taguri HTML)."""
    try:
        headers = {"User-Agent": "Mozilla/5.0 (MapReduceIndexer/1.0)"}
        response = requests.get(url, timeout=10, headers=headers)
        response.raise_for_status()
        soup = BeautifulSoup(response.text, "html.parser")

        # Eliminam scripturile si stilurile - nu contin text util
        for tag in soup(["script", "style", "meta", "noscript"]):
            tag.decompose()

        return soup.get_text(separator=" ")
    except Exception as e:
        print(f"[MAPPER] Eroare la {url}: {e}", file=sys.stderr)
        return ""


def tokenize(text: str) -> list[str]:
    """
    Extrage cuvintele din text:
      - doar caractere alfanumerice
      - lowercase
      - minim 3 caractere (eliminam articole, prepozitii scurte)
    """
    words = re.findall(r"[a-zA-Z]{3,}", text)
    return [w.lower() for w in words]


def map_url(url: str) -> None:
    """
    Functia de mapare pentru un URL.
    Emite perechi: <word>\t{"url": "<URL>", "count": 1}
    """
    url = url.strip()
    if not url:
        return

    print(f"[MAPPER] Procesez: {url}", file=sys.stderr)

    text = fetch_text(url)
    if not text:
        return

    words = tokenize(text)
    print(f"[MAPPER] {url} -> {len(words)} cuvinte extrase", file=sys.stderr)

    for word in words:
        # Emitem perechea <word TAB {url: 1}>
        # Formatul valorii e JSON pentru a fi usor de parsat de reducer
        value = json.dumps({"url": url, "count": 1})
        print(f"{word}\t{value}")


def main():
    """Citeste URL-urile din stdin si aplica map_url pentru fiecare."""
    for line in sys.stdin:
        map_url(line)


if __name__ == "__main__":
    main()
