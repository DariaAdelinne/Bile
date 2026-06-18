#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
[[ -f dist/AuctioneerMicroservice.jar ]] || bash scripts/build.sh

mkdir -p logs
rm -f auction_errors.txt

cleanup() { jobs -p | xargs -r kill 2>/dev/null || true; }
trap cleanup EXIT

echo "Pornesc ErrorStatisticsProcessor..."
java -jar dist/ErrorStatisticsProcessorMicroservice.jar > logs/error-statistics.log 2>&1 &

echo "Pornesc MessageProcessor..."
java -jar dist/MessageProcessorMicroservice.jar > logs/message-processor.log 2>&1 &

echo "Pornesc BiddingProcessor..."
java -jar dist/BiddingProcessorMicroservice.jar > logs/bidding-processor.log 2>&1 &

sleep 2

echo "Pornesc Auctioneer..."
java -jar dist/AuctioneerMicroservice.jar > logs/auctioneer.log 2>&1 &

sleep 1

echo "Pornesc Bidders..."
for name in Ana Bogdan Carmen; do
    java -jar dist/BidderMicroservice.jar > "logs/bidder-$name.log" 2>&1 &
    sleep 0.5
done

wait

echo ""
echo "===== AUCTIONEER ====="
cat logs/auctioneer.log
echo ""
echo "===== STATISTICI ERORI ====="
cat logs/error-statistics.log
echo ""
echo "===== FISIER LOCAL ====="
cat auction_errors.txt 2>/dev/null || echo "(fisierul nu a fost generat)"
