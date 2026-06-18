from abc import ABC, abstractmethod


class IErrorStorage(ABC):
    """
    Interfata pentru persistarea statisticilor de erori.

    Principiul I (ISP): interfata mica si specializata.
    Principiul D (DIP): ErrorStatisticsProcessor depinde de aceasta
    abstractizare, nu de o implementare concreta.
    """

    @abstractmethod
    def write_statistics(self, errors: list) -> None:
        """Scrie statisticile de erori la finalul licitatiei."""
        pass
