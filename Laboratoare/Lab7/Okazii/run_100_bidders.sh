#!/bin/bash

for i in {1..100}
do
  java -jar out/artifacts/BidderMicroservice_jar/BidderMicroservice.jar &
done

wait
