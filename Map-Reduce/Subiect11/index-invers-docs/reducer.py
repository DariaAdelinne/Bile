#!/usr/bin/env python3
"""
reducer.py - Functia de reducere pentru indexul invers.

Primeste de la STDIN (deja sortat dupa cheie) perechi de forma:
  <word>\t<document_id>:1

Emite la STDOUT perechi de forma:
  <word>\t{document_id_1: count_1, document_id_2: count_2, ...}

Hadoop sorteaza outputul mapper-ului dupa cheie (word) inainte de reducer,
deci toate aparitiile aceluiasi cuvant ajung consecutive.
"""

import sys
import json

current_word = None
# dict: document_id -> numar aparitii in documentul respectiv
doc_counts = {}

for line in sys.stdin:
    line = line.strip()
    if not line:
        continue

    parts = line.split("\t", 1)
    if len(parts) < 2:
        continue

    word = parts[0]
    # valoarea are forma "document_id:1"
    value_parts = parts[1].split(":", 1)
    if len(value_parts) < 2:
        continue

    document_id = value_parts[0]
    try:
        count = int(value_parts[1])
    except ValueError:
        continue

    if current_word != word:
        # emit rezultatul pentru cuvantul anterior
        if current_word is not None:
            print(f"{current_word}\t{json.dumps(doc_counts, sort_keys=True)}")
        # resetare pentru cuvantul nou
        current_word = word
        doc_counts = {}

    doc_counts[document_id] = doc_counts.get(document_id, 0) + count

# emit ultimul cuvant
if current_word is not None:
    print(f"{current_word}\t{json.dumps(doc_counts, sort_keys=True)}")
