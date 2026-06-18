#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
[[ -f dist/AuctioneerMicroservice.jar ]] || ./scripts/build.sh
mkdir -p logs data
cleanup(){ jobs -p | xargs -r kill 2>/dev/null || true; }
trap cleanup EXIT
java -jar dist/RatingMicroservice.jar > logs/rating.log 2>&1 &
java -jar dist/MessageProcessorMicroservice.jar > logs/message-processor.log 2>&1 &
java -jar dist/BiddingProcessorMicroservice.jar > logs/bidding-processor.log 2>&1 &
sleep 1
java -jar dist/AuctioneerMicroservice.jar > logs/auctioneer.log 2>&1 &
sleep 1
for spec in "Ana:5" "Bogdan:4" "Carmen:3"; do
  name=${spec%:*}; rating=${spec#*:}
  java -jar dist/BidderMicroservice.jar --name "$name" --rating "$rating" > "logs/bidder-$name.log" 2>&1 &
done
wait
echo "Demo terminat. Vezi logs/ și data/ratings.csv"
