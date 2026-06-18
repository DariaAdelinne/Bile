#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

echo "Building..."
./gradlew shadowJar

mkdir -p dist
cp error-statistics-processor/build/libs/error-statistics-processor-all.jar dist/ErrorStatisticsProcessorMicroservice.jar
cp message-processor/build/libs/message-processor-all.jar dist/MessageProcessorMicroservice.jar
cp bidding-processor/build/libs/bidding-processor-all.jar dist/BiddingProcessorMicroservice.jar
cp auctioneer/build/libs/auctioneer-all.jar dist/AuctioneerMicroservice.jar
cp bidder/build/libs/bidder-all.jar dist/BidderMicroservice.jar

echo "JAR-urile sunt in: $(pwd)/dist"
