from abc import ABC, abstractmethod


class IChatStorage(ABC):
    """
    Interfata pentru salvarea istoricului conversatiei.

    Principiul I (ISP): interfata mica si specializata.
    Principiul D (DIP): ChatMasterProcessor depinde de aceasta abstractizare.
    """

    @abstractmethod
    def save_history(self, session_id: str, participants: list, messages: list) -> None:
        """Salveaza istoricul complet al unei conversatii private."""
        pass
