source .venv/bin/activate
rm -f errors.txt result.txt
python setup_rabbitmq.py
python ErrorProcessor.py &
python BiddingProcessor.py &
python MessageProcessor.py &
python Auctioneer.py &
for i in 1 2 3 4 5; do python Bidder.py & done
wait
cat result.txt
cat errors.txt