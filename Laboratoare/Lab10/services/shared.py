# Importuri comune pentru JSON, variabile de mediu, timp, tipuri si Kafka.
import json
import os
import time
from typing import Dict, Iterable, List, Tuple
from kafka import KafkaProducer, KafkaConsumer
from kafka.errors import NoBrokersAvailable

# Adresa brokerului Kafka; in Docker Compose numele serviciului este de obicei kafka
BOOTSTRAP = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "kafka:9092")
# Sesiunea separa o rulare de alta, ca mesajele vechi sa poata fi ignorate
SESSION_ID = os.getenv("SESSION_ID", "lab10-session")
# Tipul de licitatie decide regula dupa care se alege castigatorul
AUCTION_TYPE = os.getenv("AUCTION_TYPE", "english").lower()

# Numele topicurilor Kafka folosite de serviciile proiectului
TOPIC_BIDS = os.getenv("TOPIC_BIDS", "topic_oferte")
TOPIC_FINISHED = os.getenv("TOPIC_FINISHED", "topic_notificare_procesor_mesaje")
TOPIC_PROCESSED = os.getenv("TOPIC_PROCESSED", "topic_oferte_procesate")
TOPIC_RESULT = os.getenv("TOPIC_RESULT", "topic_rezultat")
TOPIC_METRICS = os.getenv("TOPIC_METRICS", "topic_metrici")


def wait_for_kafka(timeout: int = 90) -> None:
    # Functia asteapta pana cand brokerul Kafka devine disponibil
    start = time.time()
    while True:
        try:
            # Incercam sa cream un consumer simplu si sa citim lista de topicuri
            c = KafkaConsumer(bootstrap_servers=BOOTSTRAP, consumer_timeout_ms=1000)
            c.topics()
            c.close()
            return
        except Exception as exc:
            # Daca timpul maxim a expirat, oprim programul cu o eroare clara
            if time.time() - start > timeout:
                raise RuntimeError(f"Kafka indisponibil dupa {timeout}s: {exc}")
            print("Astept Kafka...", flush=True)
            time.sleep(2)


def producer() -> KafkaProducer:
    # Ne asiguram ca Kafka este pornit inainte de a crea producerul
    wait_for_kafka()
    return KafkaProducer(
        bootstrap_servers=BOOTSTRAP,
        # Valorile mesajelor sunt dictionare Python transformate in JSON bytes
        value_serializer=lambda v: json.dumps(v).encode("utf-8"),
        # Cheia este convertita in string si apoi in bytes; poate fi None
        key_serializer=lambda v: str(v).encode("utf-8") if v is not None else None,
        linger_ms=5,
        retries=5,
    )


def consumer(topic: str, group_id: str | None, offset: str = "earliest", timeout_ms: int | None = None) -> KafkaConsumer:
    # Cream un consumer Kafka configurat la fel pentru toate serviciile
    wait_for_kafka()
    kwargs = dict(
        bootstrap_servers=BOOTSTRAP,
        auto_offset_reset=offset,
        enable_auto_commit=True,
        # Mesajele JSON din Kafka sunt transformate inapoi in dictionare Python
        value_deserializer=lambda v: json.loads(v.decode("utf-8")),
        key_deserializer=lambda v: v.decode("utf-8") if v else None,
    )
    # Daca exista group_id, Kafka imparte mesajele intre consumatorii din acelasi grup
    if group_id:
        kwargs["group_id"] = group_id
    # Timeout-ul controleaza cand se opreste bucla de consum daca nu mai vin mesaje.
    if timeout_ms is not None:
        kwargs["consumer_timeout_ms"] = timeout_ms
    return KafkaConsumer(topic, **kwargs)


def metric(event: str, **payload) -> None:
    # Trimite un eveniment de monitorizare in topicul de metrici
    try:
        p = producer()
        payload.update({"event": event, "session_id": SESSION_ID, "ts": time.time()})
        p.send(TOPIC_METRICS, key=SESSION_ID, value=payload)
        p.flush()
        p.close()
    except Exception as exc:
        # Metricile nu trebuie sa opreasca aplicatia
        print(f"Metricile nu au putut fi trimise: {exc}", flush=True)


def choose_winner(bids: List[Dict], auction_type: str) -> Dict:
    # Daca nu exista oferte, intoarcem un rezultat gol, dar valid
    if not bids:
        return {"winner_id": None, "winner_amount": 0, "paid_amount": 0, "rule": "Nu exista oferte"}

    # Sortare dupa suma descrescator; la egalitate se prefera oferta mai veche prin timestamp
    by_amount = sorted(bids, key=lambda b: (int(b["amount"]), -float(b["ts"])), reverse=True)
    # Sortare cronologica, folosita de regulile unde conteaza primul ofertant
    by_time = sorted(bids, key=lambda b: float(b["ts"]))

    if auction_type == "dutch":
        # Licitatie olandeza: pretul scade; primul ofertant care accepta castiga
        w = by_time[0]
        return {"winner_id": w["identity"], "winner_amount": w["amount"], "paid_amount": w["amount"], "rule": "olandeza: prima oferta primita castiga"}

    if auction_type == "swedish":
        # Interpretare uzuala pentru lucrare: oferta sigilata, castigatorul plateste a doua oferta
        w = by_amount[0]
        paid = by_amount[1]["amount"] if len(by_amount) > 1 else w["amount"]
        return {"winner_id": w["identity"], "winner_amount": w["amount"], "paid_amount": paid, "rule": "suedeza: oferta maxima castiga, plata = a doua oferta"}

    if auction_type == "candle":
        # Licitatie cu lumanarea: inchidere aleatoare; castiga cea mai mare oferta pana la momentul stingerii
        close_index = max(0, int(len(by_time) * 0.70) - 1)
        eligible = by_time[: close_index + 1]
        w = sorted(eligible, key=lambda b: int(b["amount"]), reverse=True)[0]
        return {"winner_id": w["identity"], "winner_amount": w["amount"], "paid_amount": w["amount"], "rule": f"cu lumanarea: castiga oferta maxima pana la indexul aleator {close_index}"}

    # English: oferta cea mai mare castiga
    w = by_amount[0]
    return {"winner_id": w["identity"], "winner_amount": w["amount"], "paid_amount": w["amount"], "rule": "engleza: oferta maxima castiga"}
