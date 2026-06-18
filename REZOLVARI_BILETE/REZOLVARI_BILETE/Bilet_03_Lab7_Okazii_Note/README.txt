====================================================================
  BILET 03 - Lab 7: Okazii cu evaluari (nota 1-5) + fisier local
====================================================================

CERINTA:
  Pornind de la Okazii din Lab 7, sa se adauge posibilitatea ca
  bidderii sa trimita evaluari (nota 1-5) dupa licitatie, care sa
  fie salvate intr-un fisier local (result.txt).

--------------------------------------------------------------------
MODIFICARI FATA DE LABORATORUL 7:
--------------------------------------------------------------------

  1. BidderMicroservice.kt (MODIFICAT):
     - Bidderul primeste o identitate aleatoare: (Nume Prenume, telefon, email)
     - Dupa primirea rezultatului licitatiei, trimite o nota aleatoare
       (1-5) catre GradingMicroservice pe portul 2000

  2. GradingMicroservice/Grader.kt (NOU):
     - Microserviciu nou, asculta pe portul 2000
     - Accepta conexiuni multiple in paralel (un thread per bidder)
     - Scrie fiecare evaluare primita in fisierul "result.txt"
     - Se inchide automat cand toti bidderii s-au deconectat

  3. MessageProcessorMicroservice.kt (TODO-uri rezolvate):
     - Filtrare duplicate cu operatorul .distinct() din RxJava
     - Sortare dupa timestamp cu sortedBy { it.timestamp }

  4. Message.kt (MODIFICAT):
     - deserialize() suporta si formatul "(Nume Prenume, telefon, email)"
     - Adaugat equals() si hashCode() pentru deduplicare corecta

--------------------------------------------------------------------
ARHITECTURA:
--------------------------------------------------------------------

  [GradingMicroservice :2000]  <-- primeste evaluarile de la bidderi
         ^
         | TCP (nota 1-5)
         |
  [BidderMicroservice]  ---> [AuctioneerMicroservice :1500]
                                       |
                                       v
                             [MessageProcessorMicroservice :1600]
                             (filtreaza duplicate, sorteaza)
                                       |
                                       v
                             [BiddingProcessorMicroservice :1700]
                             (decide castigatorul: max(oferta))
                                       |
                                       v
                             [AuctioneerMicroservice :1500]
                             (anunta castigator -> toti bidderii)
                                       |
                                       v
                             [BidderMicroservice]
                             primeste rezultat -> trimite nota -> GradingMicroservice

Principii SOLID:
  S - fiecare microserviciu are o singura responsabilitate
  O - GradingMicroservice poate fi extins (ex: calcul medie) fara modificari
  L - toate microserviciile sunt interoperabile prin protocolul Message
  I - fiecare microserviciu expune o interfata clara (port + protocol)
  D - bidderii nu depind de implementarea graderului, doar de portul 2000

--------------------------------------------------------------------
SETUP IntelliJ IDEA:
--------------------------------------------------------------------

  1. Deschide folderul OkaziiNote/ ca proiect IntelliJ (File -> Open)
  2. Fiecare subfolder (AuctioneerMicroservice, BidderMicroservice etc.)
     este un modul IntelliJ separat
  3. Adauga dependentele pentru fiecare modul:
       - kotlin-stdlib
       - rxjava-3.x.x
       - rxkotlin-3.x.x
       - MessageLibrary.jar (compilat din MessageLibrary/)
  4. Seteaza MANIFEST.MF pentru fiecare modul (e deja in src/META-INF/)
  5. Build -> Build Artifacts -> All Artifacts

--------------------------------------------------------------------
ORDINEA DE PORNIRE (OBLIGATORIE):
--------------------------------------------------------------------

  Terminal 1:  java -jar GradingMicroservice.jar       (port 2000)
  Terminal 2:  java -jar BiddingProcessorMicroservice.jar  (port 1700)
  Terminal 3:  java -jar MessageProcessorMicroservice.jar  (port 1600)
  Terminal 4:  java -jar AuctioneerMicroservice.jar    (port 1500, 15s timeout)

  Dupa pornirea Auctioneer-ului, pornesti bidderii (cat mai multi, repede):
  Terminal 5:  java -jar BidderMicroservice.jar
  Terminal 6:  java -jar BidderMicroservice.jar
  Terminal 7:  java -jar BidderMicroservice.jar
  (etc.)

  Dupa 15 secunde, licitatia se incheie automat.
  Fisierul result.txt va contine evaluarile tuturor bidderilor.

--------------------------------------------------------------------
EXEMPLU result.txt:
--------------------------------------------------------------------

  Persoana (Popescu Cosmin, +40712345678, popescu_cosmin@gmail.com) a dat nota 4
  Persoana (Grosu Radu, +40798765432, grosu_radu@gmail.com) a dat nota 2
  Persoana (Dascalu Vlad, +40771016168, dascalu_vlad@gmail.com) a dat nota 5

====================================================================
