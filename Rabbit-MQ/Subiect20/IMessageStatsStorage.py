from abc import ABC, abstractmethod


class IMessageStatsStorage(ABC):
    """
    Interfata pentru persistarea statisticilor de mesaje.

    Principiul I (ISP): interfata mica, o singura metoda.
    Principiul D (DIP): MessageStatisticsProcessor depinde de aceasta
    abstractizare, nu de o implementare concreta.
    """

    @abstractmethod
    def write_statistics(self, events: list) -> None:
        """Scrie raportul de statistici la finalul licitatiei."""
        pass
