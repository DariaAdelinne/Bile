#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mvn -q -DskipTests clean package
mkdir -p dist
cp message-processor/target/MessageProcessorMicroservice.jar dist/
cp bidding-processor/target/BiddingProcessorMicroservice.jar dist/
cp auctioneer/target/AuctioneerMicroservice.jar dist/
cp rating-service/target/RatingMicroservice.jar dist/
cp bidder/target/BidderMicroservice.jar dist/
echo "JAR-urile sunt în: $(pwd)/dist"
