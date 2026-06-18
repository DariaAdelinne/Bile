#!/usr/bin/env bash
set -e
AUCTION_TYPE=english docker compose up --build --scale auctioneer=4 --scale bidder=100
