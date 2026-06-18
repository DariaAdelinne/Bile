#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

# Kotlin 1.9.24 trebuie compilat cu JDK 17, nu cu Java 25.
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
cp chat-master/target/ChatMasterMicroservice.jar dist/
cp chat-stream-processor/target/ChatStreamProcessorMicroservice.jar dist/
cp user-communication/target/UserCommunicationMicroservice.jar dist/
echo "Compilare terminată. JAR-urile sunt în: $(pwd)/dist"
