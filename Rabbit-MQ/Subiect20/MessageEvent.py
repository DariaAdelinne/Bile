from datetime import datetime


class MessageEvent:
    """
    Reprezinta un eveniment de mesaj raportat de un microserviciu.

    Campuri:
      source    — microserviciul care a procesat mesajul
      direction — 'SENT' sau 'RECEIVED'
      details   — continut sumar al mesajului (pentru raport)
      timestamp — momentul procesarii

    Principiul S (SRP): encapsuleaza doar datele unui eveniment,
    nu stie nimic despre transport sau stocare.
    """

    def __init__(self, source: str, direction: str, details: str):
        self.source = source
        self.direction = direction   # 'SENT' sau 'RECEIVED'
        self.details = details
        self.timestamp = datetime.now()

    def serialize(self) -> str:
        ts = self.timestamp.strftime("%Y-%m-%d %H:%M:%S")
        return f"{self.source}|{self.direction}|{self.details}|{ts}"

    @staticmethod
    def deserialize(raw: str) -> 'MessageEvent':
        parts = raw.split("|", 3)
        if len(parts) < 4:
            return MessageEvent("unknown", "UNKNOWN", raw)
        event = MessageEvent(parts[0], parts[1], parts[2])
        try:
            event.timestamp = datetime.strptime(parts[3], "%Y-%m-%d %H:%M:%S")
        except ValueError:
            pass
        return event

    def __str__(self) -> str:
        ts = self.timestamp.strftime("%Y-%m-%d %H:%M:%S")
        return f"[{ts}] [{self.direction:<8s}] ({self.source}) {self.details}"
