from abc import ABC, abstractmethod


class IRatingStorage(ABC):
    """
    Interfata pentru salvarea evaluarilor.

    Principiul I (ISP): interfata mica si specializata — o singura metoda.
    Principiul D (DIP): RatingProcessor depinde de aceasta abstractizare,
    nu de o implementare concreta (FileRatingStorage sau altele).
    """

    @abstractmethod
    def save(self, username: str, rating: int) -> None:
        """Salveaza evaluarea unui utilizator."""
        pass
