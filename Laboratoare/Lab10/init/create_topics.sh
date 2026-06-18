#!/usr/bin/env bash
set -euo pipefail
BOOTSTRAP="kafka:9092"
TOPICS=(topic_oferte topic_rezultat topic_oferte_procesate topic_notificare_procesor_mesaje topic_metrici)

echo "Sterg topic-urile vechi, daca exista..."
for t in "${TOPICS[@]}"; do
  /opt/kafka/bin/kafka-topics.sh --bootstrap-server "$BOOTSTRAP" --delete --topic "$t" >/dev/null 2>&1 || true
done
sleep 4

echo "Creez topic-urile pentru Lab 10..."
/opt/kafka/bin/kafka-topics.sh --bootstrap-server "$BOOTSTRAP" --create --if-not-exists --topic topic_oferte --partitions 4 --replication-factor 1
/opt/kafka/bin/kafka-topics.sh --bootstrap-server "$BOOTSTRAP" --create --if-not-exists --topic topic_rezultat --partitions 1 --replication-factor 1
/opt/kafka/bin/kafka-topics.sh --bootstrap-server "$BOOTSTRAP" --create --if-not-exists --topic topic_oferte_procesate --partitions 1 --replication-factor 1
/opt/kafka/bin/kafka-topics.sh --bootstrap-server "$BOOTSTRAP" --create --if-not-exists --topic topic_notificare_procesor_mesaje --partitions 1 --replication-factor 1
/opt/kafka/bin/kafka-topics.sh --bootstrap-server "$BOOTSTRAP" --create --if-not-exists --topic topic_metrici --partitions 1 --replication-factor 1
/opt/kafka/bin/kafka-topics.sh --bootstrap-server "$BOOTSTRAP" --list
