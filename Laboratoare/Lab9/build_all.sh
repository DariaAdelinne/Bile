#!/bin/bash
set -e
PROJECTS=(TimeSourceMicroservice TimeProcessorMicroservice TimeSinkMicroservice ClientSourceMicroservice ComandaProcessorMicroservice DepozitProcessorMicroservice FacturareProcessorMicroservice LivrareSinkMicroservice)
for p in "${PROJECTS[@]}"; do
  echo "========== Building $p =========="
  (cd "$p" && mvn -q -DskipTests package)
done
echo "Gata: toate JAR-urile sunt in target/. Acum ruleaza: docker compose up --build"
