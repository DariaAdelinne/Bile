#!/bin/bash
# Simuleaza pipeline-ul Map-Reduce pentru indexul invers (fara Hadoop).
#
# Fiecare linie trimisa la mapper are formatul: "document_id\tcontentul liniei"
# Hadoop face acelasi lucru in mod distribuit; aici simulam local cu pipes Unix.

INPUT_DIR="./input"
MAPPER="./mapper.py"
REDUCER="./reducer.py"

echo "=== MAP-REDUCE: Index Invers ==="
echo ""

# Pasul 1 (MAP): pentru fiecare fisier din input/, prefixam fiecare linie cu
# numele fisierului (document_id), apoi trimitem totul la mapper.py
# Pasul 2: sortare dupa cheie (word) - Hadoop face asta automat
# Pasul 3 (REDUCE): reducer.py grupeaza si emite indexul invers
(
  for filepath in "$INPUT_DIR"/*.txt; do
    doc_id=$(basename "$filepath")
    while IFS= read -r line || [ -n "$line" ]; do
      printf "%s\t%s\n" "$doc_id" "$line"
    done < "$filepath"
  done
) | python3 "$MAPPER" | sort | python3 "$REDUCER"
