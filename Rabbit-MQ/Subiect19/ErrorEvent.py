from datetime import datetime
from ErrorType import ErrorType


class ErrorEvent:
    """
    Reprezinta o eroare aparuta in timpul licitatiei.

    Principiul S (SRP): acest obiect doar incapsuleaza datele unei erori,
    nu stie nimic despre cum se salveaza sau se transmite.
    """

    def __init__(self, error_type: ErrorType, source: str, details: str):
        self.error_type = error_type
        self.source = source          # microserviciul care a raportat eroarea
        self.details = details        # descrierea erorii
        self.timestamp = datetime.now()

    def serialize(self) -> str:
        """Serialzeaza evenimentul ca string pentru transmitere prin RabbitMQ."""
        ts = self.timestamp.strftime("%Y-%m-%d %H:%M:%S")
        return f"{self.error_type.name}|{self.source}|{self.details}|{ts}"

    @staticmethod
    def deserialize(raw: str) -> 'ErrorEvent':
        """Deserializeaza un string primit prin RabbitMQ inapoi la ErrorEvent."""
        parts = raw.split("|", 3)
        if len(parts) < 4:
            return ErrorEvent(ErrorType.UNKNOWN, "unknown", raw)
        try:
            error_type = ErrorType[parts[0]]
        except KeyError:
            error_type = ErrorType.UNKNOWN
        event = ErrorEvent(error_type, parts[1], parts[2])
        try:
            event.timestamp = datetime.strptime(parts[3], "%Y-%m-%d %H:%M:%S")
        except ValueError:
            pass
        return event

    def __str__(self) -> str:
        ts = self.timestamp.strftime("%Y-%m-%d %H:%M:%S")
        return f"[{ts}] [{self.error_type.value}] ({self.source}) {self.details}"
