#!/bin/bash
cd ~/Downloads/deLaTeo/wetransfer_docker_2026-06-18_1244/Rabbit-MQ/Subiect18

python3 setup_rabbitmq.py

python3 MessageProcessor.py &
python3 RatingProcessor.py &
python3 Auctioneer.py &
sleep 5

echo -e "alice\n4" | python3 Bidder.py &
sleep 1
echo -e "bob\n5" | python3 Bidder.py &
sleep 2
python3 BiddingProcessor.py
