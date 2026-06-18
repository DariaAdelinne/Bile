# Bilet 10 — LibraryApp cu Cache pe Fisier Disc

**Cerinta:** Pornind de la exemplul 2 din lab 6 (LibraryApp), sa se utilizeze un fisier pe disc
drept memorie cache. Se foloseste NUMAI adaugare (append-only) si analiza pe baza de marcaje temporale.

---

## Arhitectura

```
[Browser / Client]
        |  HTTP GET /print?format=json
        |         /find?author=Jules Verne
        v
+------------------------------+
|  LibraryPrinterController    |  (orchestrator)
|  - verifica cache mai intai  |
+------------------------------+
        |                |
   CACHE HIT?          CACHE MISS
        |                |
   returneaza        delega la:
   din cache.log     ILibraryDAOService  +  ILibraryPrinterService
                         |                        |
                    LibraryDAOService       LibraryPrinterService
                    (date in memorie)       (formatare HTML/JSON/raw)
                                                  |
                                         scrie rezultat in:
                                         IFileCacheService
                                                  |
                                         FileCacheService
                                         (append la cache.log)
```

---

## Diagrama de clase (text)

```
<<interface>>                      <<interface>>
ILibraryDAOService                 IFileCacheService
+getBooks(): Set<Book>             +get(key: String): String?
+addBook(book: Book)               +put(key: String, response: String)
+findAllByAuthor(...): Set<Book>          |
+findAllByTitle(...): Set<Book>           | implements
+findAllByPublisher(...): Set<Book>       v
        |                          FileCacheService
        | implements               - cacheFilePath: String   (din application.properties)
        v                          - ttlMinutes: Long        (din application.properties)
  LibraryDAOService                - objectMapper: ObjectMapper
  - _books: MutableSet<Book>       + get(key): String?       <- citeste toate liniile, gaseste
                                                                 cea mai recenta intrare valida
<<interface>>                      + put(key, response)      <- append JSON la cache.log
ILibraryPrinterService
  extends: IHTMLPrinter
           IJSONPrinter            <<pojo>>
           IRawPrinter             CacheRecord
        |                          - key: String
        | implements               - timestamp: LocalDateTime
        v                          - response: String
  LibraryPrinterService
  +printHTML(books): String
  +printJSON(books): String
  +printRaw(books): String

  LibraryPrinterController
  @Autowired ILibraryDAOService
  @Autowired ILibraryPrinterService
  @Autowired IFileCacheService
  +customPrint(format): String     <- GET /print?format=
  +customFind(...): String         <- GET /find?author= / ?title= / ?publisher=
```

---

## Formatul fisierului cache.log

Fisierul este **append-only**: nu se sterge si nu se suprascrie niciodata.
Fiecare linie este un obiect JSON (CacheRecord):

```
{"key":"print_json","timestamp":"2026-06-16T22:30:00","response":"[...]"}
{"key":"find_author_Jules Verne","timestamp":"2026-06-16T22:31:00","response":"[...]"}
{"key":"print_json","timestamp":"2026-06-16T23:05:00","response":"[...]"}
```

La citire: se parcurg toate liniile, se filtreaza dupa `key`, se ia
**cea mai recenta** intrare si se verifica daca `timestamp + 30 min > acum`.

---

## Principii SOLID

- **S**: fiecare clasa face un singur lucru: DAO (date), Printer (formatare), FileCacheService (cache disc), Controller (orchestrare).
- **O**: poti adauga RedisCache / MemoryCache implementand IFileCacheService fara sa modifici controller-ul.
- **L**: FileCacheService substituie complet IFileCacheService; LibraryDAOService substituie ILibraryDAOService.
- **I**: interfete separate pentru DAO, printer (HTML/JSON/raw), si cache.
- **D**: Controller-ul depinde de ILibraryDAOService, ILibraryPrinterService, IFileCacheService — nu de implementari concrete.

---

## Cum rulez

### Cerinte
| Componenta | Necesar |
|---|---|
| Java JDK 8 sau 11 | da |
| Maven (inclus in IntelliJ) | da |
| Internet la prima compilare | da |

### Pas 1 — Deschide in IntelliJ
1. **File → Open** → selecteaza folderul `LibraryAppWithCache` (cel cu `pom.xml`)
2. Asteapta Maven sync; daca nu porneste: click dreapta `pom.xml` → **Maven → Reload Project**

### Pas 2 — Porneste serverul
1. Deschide `LibraryApp.kt` → click **▶** langa `main` → **Run 'LibraryApp'**
2. Consola afiseaza: `Tomcat started on port(s): 8080`

### Pas 3 — Testeaza in browser

```
# Toate cartile in format JSON:
http://localhost:8080/print?format=json

# Toate cartile in format HTML:
http://localhost:8080/print?format=html

# Toate cartile in format raw:
http://localhost:8080/print?format=raw

# Cauta dupa autor:
http://localhost:8080/find?author=Jules Verne

# Cauta dupa titlu:
http://localhost:8080/find?title=Insula Misterioasa

# Cauta dupa editura:
http://localhost:8080/find?publisher=Teora
```

### Pas 4 — Verifica cache-ul

**In consola IntelliJ** vei vedea:
```
[CACHE MISS] key='print_json' -> raspuns calculat si stocat in cache
[CACHE WRITE] key='print_json' -> scris in 'cache.log'

# La al doilea request identic (in < 30 min):
[CACHE HIT] key='print_json' (stocat la 2026-06-16T22:30:00, expira la 2026-06-16T23:00:00)
```

**Fisierul `cache.log`** apare in directorul de lucru al proiectului (langa `pom.xml`).
Poti sa il deschizi cu orice editor text pentru a vedea intrarile append-only.

---

## Erori frecvente

| Simptom | Cauza | Rezolvare |
|---|---|---|
| Port 8080 ocupat | alt server ruleaza | opreste-l sau schimba portul in `application.properties` |
| `ClassNotFoundException: JavaTimeModule` | dependenta jsr310 lipsa | e in pom.xml; fa Maven Reload |
| cache.log nu apare | serverul nu a primit niciun request | acceseaza un URL din Pas 3 |
