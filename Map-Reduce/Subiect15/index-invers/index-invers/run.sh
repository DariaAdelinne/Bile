#!/bin/bash
# ============================================================
# Pipeline Map-Reduce pentru indexul invers
#
# Etape:
#   1. MAP    - mapper.py citeste urls.txt si emite <word, {url:1}>
#   2. SORT   - sort -k1,1 sorteaza dupa cuvant (obligatoriu
#               pentru reducer-ul secvential)
#   3. REDUCE - reducer.py grupeaza si agreg count-urile per URL
#   4. OUTPUT - rezultatul final se scrie in index_invers.txt
#               si primele 20 linii se afiseaza in terminal
# ============================================================

set -e

URLS_FILE="urls.txt"
OUTPUT_FILE="index_invers.txt"

echo "=== MAP-REDUCE: Index invers ==="
echo ""
echo "[1/3] MAP - parsare URL-uri si emitere perechi word->url:count ..."
echo "[2/3] SORT - sortare dupa cuvant ..."
echo "[3/3] REDUCE - agregare count-uri per URL ..."
echo ""

cat "$URLS_FILE" \
  | python3 mapper.py \
  | sort -k1,1 \
  | python3 reducer.py \
  > "$OUTPUT_FILE"

echo ""
echo "=== REZULTAT (primele 20 cuvinte) ==="
head -20 "$OUTPUT_FILE"
echo ""
echo "Index complet salvat in: $OUTPUT_FILE"
echo "Total cuvinte indexate: $(wc -l < "$OUTPUT_FILE")"
