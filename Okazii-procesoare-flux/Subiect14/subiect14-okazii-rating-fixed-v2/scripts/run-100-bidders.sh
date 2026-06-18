#!/usr/bin/env bash
set -euo pipefail
JAR=${1:-dist/BidderMicroservice.jar}
for i in $(seq 1 100); do
  java -jar "$JAR" --name "user-$i" --rating "$((1 + i % 5))" > "logs/bidder-$i.log" 2>&1 &
done
wait
