from enum import Enum


class ErrorType(Enum):
    """
    Tipurile de erori posibile in sistemul de licitatie.

    Principiul O (OCP): se pot adauga tipuri noi fara a modifica
    clasele care folosesc aceasta enumerare.
    """
    COMMUNICATION_ERROR  = "Eroare de comunicare"      # timeout socket, conexiune refuzata
    QUEUE_ERROR          = "Eroare sistem de cozi"     # RabbitMQ indisponibil, coada plina
    DUPLICATE_BID        = "Oferta duplicata"          # acelasi bidder a trimis de doua ori
    INVALID_MESSAGE      = "Mesaj invalid"             # format gresit al mesajului
    UNKNOWN              = "Eroare necunoscuta"
