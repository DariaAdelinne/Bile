#!/usr/bin/env python3
"""
REDUCER - Indexul invers (Map-Reduce cu stdin/stdout)

Citeste perechile <word\t{url, count}> din stdin (ieșirea sortatã a mapper-ului).
Deoarece intrarea este sortata dupa cuvant (sort -k1,1 in pipeline),
reducer-ul poate procesa grupurile secvential fara a incarca totul in memorie.

Pentru fiecare grup de perechi cu acelasi cuvant:
  - insumeaza aparitiile per URL
  - emite: <word>\t{URL_1: count_1, URL_2: count_2, ...}

Utilizare (pipeline complet):
    cat urls.txt | python3 mapper.py | sort -k1,1 | python3 reducer.py
"""

import sys
import json
from collections import defaultdict


def emit(word: str, url_counts: dict) -> None:
    """Emite rezultatul final pentru un cuvant."""
    # Sortam URL-urile dupa count descrescator pentru lizibilitate
    sorted_counts = dict(
        sorted(url_counts.items(), key=lambda x: x[1], reverse=True)
    )
    value = json.dumps(sorted_counts, ensure_ascii=False)
    print(f"{word}\t{value}")


def main():
    """
    Functia de reducere.

    Presupune ca intrarea este sortata dupa cuvant (primul camp).
    Proceseaza grupul curent, emite rezultatul si trece la urmatorul grup.
    """
    current_word = None
    url_counts = defaultdict(int)   # {url -> count_total}

    for line in sys.stdin:
        line = line.rstrip("\n")
        if not line:
            continue

        # Parsam linia: <word>\t<json_value>
        parts = line.split("\t", 1)
        if len(parts) != 2:
            print(f"[REDUCER] Linie invalida ignorata: {line!r}", file=sys.stderr)
            continue

        word, value_json = parts

        try:
            value = json.loads(value_json)
            url = value["url"]
            count = int(value["count"])
        except (json.JSONDecodeError, KeyError, ValueError) as e:
            print(f"[REDUCER] Eroare parsare valoare: {e}", file=sys.stderr)
            continue

        if word != current_word:
            # Cuvant nou: emitem rezultatul grupului anterior
            if current_word is not None:
                emit(current_word, dict(url_counts))

            # Resetam pentru noul grup
            current_word = word
            url_counts = defaultdict(int)

        url_counts[url] += count

    # Emitem ultimul grup ramas
    if current_word is not None:
        emit(current_word, dict(url_counts))


if __name__ == "__main__":
    main()
