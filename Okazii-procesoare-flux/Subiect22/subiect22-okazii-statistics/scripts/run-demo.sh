#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p logs data
rm -f data/auction-statistics.csv
pkill -f 'StatisticsStreamProcessorMicroservice.jar' 2>/dev/null || true
pkill -f 'MessageProcessorMicroservice.jar' 2>/dev/null || true
pkill -f 'BiddingProcessorMicroservice.jar' 2>/dev/null || true
pkill -f 'AuctioneerMicroservice.jar' 2>/dev/null || true
sleep 1
java -jar dist/StatisticsStreamProcessorMicroservice.jar > logs/statistics.log 2>&1 & STATS_PID=$!
java -jar dist/MessageProcessorMicroservice.jar > logs/message-processor.log 2>&1 & MP_PID=$!
java -jar dist/BiddingProcessorMicroservice.jar > logs/bidding-processor.log 2>&1 & BP_PID=$!
sleep 1
java -jar dist/AuctioneerMicroservice.jar > logs/auctioneer.log 2>&1 & AUC_PID=$!
sleep 1
for name in Daria Mihai Ioana Andrei Alex; do
  java -jar dist/BidderMicroservice.jar --name "$name" > "logs/bidder-$name.log" 2>&1 &
done
wait "$AUC_PID"
wait "$MP_PID" || true
wait "$BP_PID" || true
sleep 1
kill "$STATS_PID" 2>/dev/null || true
wait "$STATS_PID" 2>/dev/null || true
cat logs/auctioneer.log
echo
echo '--- STATISTICI ---'
cat logs/statistics.log
echo
echo '--- FIȘIER LOCAL ---'
cat data/auction-statistics.csv
