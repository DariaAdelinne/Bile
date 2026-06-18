# Bilet 1 — PhoneAgenda cu Cache Gateway

**Cerinta:** Pornind de la agenda REST din lab 4, sa se implementeze un serviciu cu cache
ce raspunde daca aceeasi cerere a aparut in primele 30 de minute.

---

## Arhitectura

```
[Client (Postman / browser)]
        |
        | HTTP
        v
+----------------------------+
|   CacheGatewayMicroservice |  <- /cached/*  (Poarta API cu Cache)
|   (port 8080)              |
+----------------------------+
   |  CACHE HIT?             |
   |  DA  -> returneaza      |
   |         raspunsul       |
   |         din cache       |
   |                         |
   |  NU   -> delega la:     |
   v                         |
+----------------------------+
|   AgendaService            |  <- logica CRUD (in-memory, ConcurrentHashMap)
+----------------------------+
        ^
        |  (acces direct, fara cache)
+----------------------------+
|   AgendaMicroservice       |  <- /agenda/*
+----------------------------+
```

Ambele microservicii ruleaza in **acelasi proces Spring Boot** pe portul **8080**, ca in laborator.

---

## Diagrama de clase (text)

```
<<interface>>                    <<interface>>
IAgendaService                   ICacheService
+getPerson(id): Person?          +get(key: String): CacheEntry?
+createPerson(person)            +put(key: String, value: Any?)
+deletePerson(id)                +isValid(entry: CacheEntry): Boolean
+updatePerson(id, person)             |
+searchAgenda(...): List<Person>      |
        |                             |
        | implements                  | implements
        v                             v
  AgendaService                  CacheService
  - agenda: ConcurrentHashMap    - cache: ConcurrentHashMap<String, CacheEntry>
                                   (verifica timestamp < 30 minute)
        |                             |
        | @Autowired                  | @Autowired
        v                             v
  AgendaMicroservice           CacheGatewayMicroservice
  @RestController /agenda/*    @RestController /cached/*
                               (verifica cache inainte de a delega la AgendaService)

<<pojo>>                         <<pojo>>
Person                           CacheEntry
- id: Int                        - responseBody: Any?
- lastName: String               - timestamp: LocalDateTime
- firstName: String
- telephoneNumber: String
```

---

## Principii SOLID

- **S** (Single Responsibility): AgendaService stocheaza date, CacheService gestioneaza cache-ul,
  CacheGatewayMicroservice orchestreaza logica de caching, AgendaMicroservice ofera acces direct.
- **O** (Open/Closed): poti adauga o noua strategie de cache (ex: Redis) fara sa modifici AgendaService.
- **L** (Liskov): AgendaService substituie complet IAgendaService; CacheService substituie ICacheService.
- **I** (Interface Segregation): IAgendaService si ICacheService sunt interfete separate cu responsabilitati clare.
- **D** (Dependency Inversion): CacheGatewayMicroservice depinde de IAgendaService si ICacheService,
  nu de implementarile concrete.

---

## Cum rulez

### Cerinte
| Componenta | Necesar |
|---|---|
| Java JDK 8 sau 11 | da |
| Maven (inclus in IntelliJ) | da |
| Internet la prima compilare | da (descarca dependentele) |

### Pas 1 — Deschide in IntelliJ
1. **File → Open** → selecteaza folderul `PhoneAgendaWithCache` (cel cu `pom.xml`)
2. Asteapta sincronizarea Maven (bara jos-dreapta)
3. Daca nu porneste singura: click dreapta pe `pom.xml` → **Maven → Reload Project**

### Pas 2 — Porneste serverul
1. Deschide `src/main/kotlin/com/sd/laborator/PhoneAgendaApp.kt`
2. Click pe **▶ (sageata verde)** din dreptul functiei `main` → **Run 'PhoneAgendaApp'**
3. In consola trebuie sa apara: `Tomcat started on port(s): 8080`

### Pas 3 — Testeaza cu Postman sau browser

**Acces DIRECT (fara cache) — prefix `/agenda`:**
```
GET    http://localhost:8080/agenda/person/1
POST   http://localhost:8080/agenda/person          body: {"id":4,"lastName":"Test","firstName":"User","telephoneNumber":"0000"}
PUT    http://localhost:8080/agenda/person/1        body: {"id":1,"lastName":"Nou","firstName":"Prenume","telephoneNumber":"9999"}
DELETE http://localhost:8080/agenda/person/1
GET    http://localhost:8080/agenda/search?lastName=Hello
```

**Acces PRIN CACHE GATEWAY — prefix `/cached`:**
```
GET    http://localhost:8080/cached/person/1        <- prima cerere: CACHE MISS, stocheaza
GET    http://localhost:8080/cached/person/1        <- a doua cerere in <30 min: CACHE HIT
POST   http://localhost:8080/cached/person          body: {"id":4,...}
GET    http://localhost:8080/cached/search?lastName=Hello
```

**Verificare cache in consola IntelliJ:**
- `[CACHE MISS] getPerson_1 -> stocat in cache`   (prima cerere)
- `[CACHE HIT] getPerson_1`                        (cerere repetata in <30 min)

---

## Erori frecvente

| Simptom | Cauza | Rezolvare |
|---|---|---|
| `Port 8080 already in use` | alt server ruleaza | opreste-l sau schimba portul in `application.properties` |
| `mvn: command not found` | Maven nu e in PATH | ruleaza din IntelliJ (buton Run) |
| Compilare esuata la json-patch | internet indisponibil | conecteaza-te la internet si recompileaza |
