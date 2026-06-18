#!/usr/bin/env python3
"""
mapper.py - Functia de mapare pentru indexul invers.

Citeste de la STDIN linii de forma:
  <document_id>\t<continut linie document>

Emite la STDOUT perechi de forma:
  <word>\t<document_id>:1

Fiecare aparitie a unui cuvant intr-un document genereaza o pereche separata.
"""

import sys
import re

for line in sys.stdin:
    line = line.strip()
    if not line:
        continue

    # formatul inputului: "document_id\tcontentul liniei"
    parts = line.split("\t", 1)
    if len(parts) < 2:
        continue

    document_id = parts[0]
    content = parts[1]

    # tokenizare: doar cuvinte din litere, lowercase
    words = re.findall(r"[a-zA-Z]+", content)

    for word in words:
        word = word.lower()
        # emite perechea <word, {document_id: 1}>
        print(f"{word}\t{document_id}:1")
