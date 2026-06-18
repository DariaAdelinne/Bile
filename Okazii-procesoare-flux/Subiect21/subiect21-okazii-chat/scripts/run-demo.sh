#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
[[ -f dist/ChatMasterMicroservice.jar ]] || ./scripts/build.sh
rm -rf logs && mkdir -p logs

cleanup(){ jobs -p | xargs -r kill 2>/dev/null || true; }
trap cleanup EXIT

java -jar dist/ChatMasterMicroservice.jar > logs/chat-master.log 2>&1 &
java -jar dist/ChatStreamProcessorMicroservice.jar > logs/chat-stream.log 2>&1 &
java -jar dist/MessageProcessorMicroservice.jar > logs/message-processor.log 2>&1 &
java -jar dist/BiddingProcessorMicroservice.jar > logs/bidding-processor.log 2>&1 &
sleep 1
java -jar dist/AuctioneerMicroservice.jar > logs/auctioneer.log 2>&1 &
sleep 1

java -jar dist/BidderMicroservice.jar --name Daria > logs/bidder-Daria.log 2>&1 & P1=$!
java -jar dist/BidderMicroservice.jar --name Mihai > logs/bidder-Mihai.log 2>&1 & P2=$!
java -jar dist/BidderMicroservice.jar --name Ioana > logs/bidder-Ioana.log 2>&1 & P3=$!
wait "$P1" "$P2" "$P3"

java -jar dist/UserCommunicationMicroservice.jar --name Daria --port 1901 --room privat1 --members Daria,Mihai,Ioana --create --message "Salut, discutăm privat despre licitație." --send-delay-ms 2000 --lifetime-ms 9000 > logs/chat-Daria.log 2>&1 & C1=$!
java -jar dist/UserCommunicationMicroservice.jar --name Mihai --port 1902 --room privat1 --message "Da, mesajul a ajuns numai la noi trei." --send-delay-ms 4000 --lifetime-ms 9000 > logs/chat-Mihai.log 2>&1 & C2=$!
java -jar dist/UserCommunicationMicroservice.jar --name Ioana --port 1903 --room privat1 --message "Confirm: conversația este privată." --send-delay-ms 5500 --lifetime-ms 9000 > logs/chat-Ioana.log 2>&1 & C3=$!
wait "$C1" "$C2" "$C3"

echo "===== LICITAȚIE ====="
cat logs/auctioneer.log
echo "===== MASTER CHAT ====="
cat logs/chat-master.log
echo "===== PROCESOR FLUX ====="
cat logs/chat-stream.log
echo "===== DARIA ====="
cat logs/chat-Daria.log
echo "===== MIHAI ====="
cat logs/chat-Mihai.log
echo "===== IOANA ====="
cat logs/chat-Ioana.log
