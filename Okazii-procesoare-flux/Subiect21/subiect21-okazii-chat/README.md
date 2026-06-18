# SUBIECTUL 21 — Okazii cu discuție privată între ofertanți

Proiect Kotlin/JVM bazat pe aplicația Okazii din Laboratorul 7. Păstrează fluxul licitației și adaugă:

- un procesor reactiv de flux pentru mesajele de chat;
- un procesor master pentru înregistrarea utilizatorilor și administrarea camerelor private;
- câte o replică `UserCommunicationMicroservice` pentru fiecare utilizator;
- verificarea apartenenței la camera privată înainte de livrare;
- eliminarea mesajelor duplicate în procesorul de flux.

## Microservicii

### Fluxul licitației

1. `BidderMicroservice`
2. `AuctioneerMicroservice`
3. `MessageProcessorMicroservice`
4. `BiddingProcessorMicroservice`

### Fluxul discuției private

1. `ChatMasterMicroservice` — port 1800
2. `ChatStreamProcessorMicroservice` — port 1810
3. `UserCommunicationMicroservice` — replicat pentru fiecare utilizator, de exemplu porturile 1901, 1902 și 1903

Utilizatorii care discută sunt aceiași utilizatori care au participat la licitație: Daria, Mihai și Ioana.

## Instalare Debian

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk maven graphviz
```

## Compilare

Toate comenzile de mai jos se execută din directorul proiectului.

```bash
chmod +x scripts/*.sh
./scripts/build.sh
```

În `dist/` vor apărea:

```text
AuctioneerMicroservice.jar
BidderMicroservice.jar
BiddingProcessorMicroservice.jar
MessageProcessorMicroservice.jar
ChatMasterMicroservice.jar
ChatStreamProcessorMicroservice.jar
UserCommunicationMicroservice.jar
```

# Varianta finală de rulare manuală

Se deschid terminale separate, toate în directorul proiectului.

## 1. ChatMasterMicroservice

```bash
java -jar dist/ChatMasterMicroservice.jar
```

Rezultat:

```text
[ChatMaster] ascultă pe portul 1800
```

## 2. ChatStreamProcessorMicroservice

```bash
java -jar dist/ChatStreamProcessorMicroservice.jar
```

Rezultat:

```text
[ChatStreamProcessor] ascultă pe portul 1810
```

## 3. MessageProcessorMicroservice

```bash
java -jar dist/MessageProcessorMicroservice.jar
```

## 4. BiddingProcessorMicroservice

```bash
java -jar dist/BiddingProcessorMicroservice.jar
```

## 5. AuctioneerMicroservice

```bash
java -jar dist/AuctioneerMicroservice.jar
```

## 6. Trei bidderi, porniți în maximum 15 secunde

```bash
java -jar dist/BidderMicroservice.jar --name Daria &
java -jar dist/BidderMicroservice.jar --name Mihai &
java -jar dist/BidderMicroservice.jar --name Ioana &
wait
```

La final, unul câștigă, iar ceilalți pierd.

## 7. Trei replici de comunicație

Aceste trei comenzi se pornesc aproape simultan într-un terminal nou:

```bash
java -jar dist/UserCommunicationMicroservice.jar \
  --name Daria \
  --port 1901 \
  --room privat1 \
  --members Daria,Mihai,Ioana \
  --create \
  --message "Salut, discutăm privat despre licitație." \
  --send-delay-ms 2000 \
  --lifetime-ms 12000 &

java -jar dist/UserCommunicationMicroservice.jar \
  --name Mihai \
  --port 1902 \
  --room privat1 \
  --message "Da, mesajul a ajuns numai la noi trei." \
  --send-delay-ms 4000 \
  --lifetime-ms 12000 &

java -jar dist/UserCommunicationMicroservice.jar \
  --name Ioana \
  --port 1903 \
  --room privat1 \
  --message "Confirm: conversația este privată." \
  --send-delay-ms 5500 \
  --lifetime-ms 12000 &

wait
```

Daria creează camera `privat1` cu membrii:

```text
Daria, Mihai, Ioana
```

Fiecare terminal trebuie să afișeze mesaje de forma:

```text
[Daria/PRIVATE privat1] Mihai: Da, mesajul a ajuns numai la noi trei.
[Mihai/PRIVATE privat1] Ioana: Confirm: conversația este privată.
[Ioana/PRIVATE privat1] Daria: Salut, discutăm privat despre licitație.
```

# Demo automat

După compilare:

```bash
./scripts/run-demo.sh
```

Scriptul rulează licitația cu Daria, Mihai și Ioana, apoi pornește camera privată și afișează jurnalele.

# Ce demonstrează proiectul

1. Cei trei utilizatori participă la licitație.
2. Fiecare utilizator are propria replică `UserCommunicationMicroservice`.
3. Replicile se înregistrează la `ChatMasterMicroservice`.
4. Daria creează camera privată.
5. Mesajele sunt trimise mai întâi la `ChatStreamProcessorMicroservice`.
6. Procesorul elimină duplicatele și validează mesajele.
7. Masterul verifică dacă expeditorul aparține camerei.
8. Masterul livrează mesajul numai membrilor camerei private.

# Clase importante

- `ChatMessage` — contractul mesajului privat;
- `UserEndpoint` — adresa unei replici de comunicație;
- `PrivateRoom` — camera și lista membrilor;
- `UserRegistry` — abstracție pentru registrul utilizatorilor;
- `RoomRepository` — abstracție pentru camere;
- `PrivateConversationService` — regulile de creare și livrare;
- `ChatMasterMicroservice` — protocolul de rețea al masterului;
- `ChatGateway` — abstracție folosită de procesorul de flux;
- `ChatStreamProcessorMicroservice` — flux reactiv și eliminare duplicate;
- `MasterClient` și `StreamChatClient` — interfețe mici folosite de replica utilizatorului;
- `UserCommunicationMicroservice` — comunicare replicată pentru fiecare utilizator.

# SOLID

## S — Single Responsibility

- Masterul gestionează utilizatorii, camerele și autorizarea.
- Procesorul de flux procesează mesajele și elimină duplicatele.
- Replica de comunicație primește și trimite mesaje pentru un singur utilizator.
- Repository-urile gestionează starea în memorie.

## O — Open/Closed

`UserRegistry`, `RoomRepository`, `ChatGateway`, `MasterClient` și `StreamChatClient` permit introducerea unor implementări noi fără schimbarea regulilor de business.

## L — Liskov Substitution

Implementările TCP sau în memorie pot fi înlocuite cu alte implementări care respectă aceleași interfețe.

## I — Interface Segregation

Interfețele sunt mici și orientate pe rol:

- `UserRegistry`;
- `RoomRepository`;
- `ChatGateway`;
- `MasterClient`;
- `StreamChatClient`.

## D — Dependency Inversion

Clasele de business depind de interfețe, nu direct de socket-uri sau de structuri concrete de stocare.

# Diagrame

- `diagrams/microservices.png`
- `diagrams/classes.png`
- sursele editabile sunt fișierele `.dot`

Regenerare:

```bash
dot -Tpng diagrams/microservices.dot -o diagrams/microservices.png
dot -Tpng diagrams/classes.dot -o diagrams/classes.png
```

# Formulare scurtă pentru prezentare

„Am păstrat fluxul Okazii din laborator și am adăugat un subsistem de conversație privată. Fiecare ofertant are propria replică de comunicație. Replicile se înregistrează la un master, iar mesajele trec printr-un procesor reactiv de flux care elimină duplicatele. Masterul păstrează camerele și verifică apartenența utilizatorului înainte de livrare. Mesajele sunt trimise numai membrilor camerei private.”
