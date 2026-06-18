#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ -x /usr/lib/jvm/java-17-openjdk-amd64/bin/java ]]; then
  export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
  export PATH="$JAVA_HOME/bin:/usr/bin:/bin:$PATH"
fi
echo "Java folosită la compilare:"
java -version
mvn -q -DskipTests clean package
mkdir -p dist
cp message-processor/target/MessageProcessorMicroservice.jar dist/
cp bidding-processor/target/BiddingProcessorMicroservice.jar dist/
cp auctioneer/target/AuctioneerMicroservice.jar dist/
cp bidder/target/BidderMicroservice.jar dist/
cp statistics-stream-processor/target/StatisticsStreamProcessorMicroservice.jar dist/
echo "Compilare terminată. JAR-urile sunt în: $(pwd)/dist"
