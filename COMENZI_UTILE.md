# Comenzi utile SD

---

## JDK / Java

### Ce sunt SDK-urile din IntelliJ
La SDKs vezi toate JDK-urile instalate pe calculatorul tău — 1.8, 11, ms-17, openjdk-23, openjdk-24, temurin-1.8, temurin-21. Sunt toate instalate, dar proiectul folosește doar cel selectat la **Project → SDK**, adică openjdk-24. Celelalte sunt acolo doar disponibile, nu active.

### Cum îți dai seama că eroarea e de la JDK
Cauți în eroare cuvinte ca:
- `UnsupportedClassVersionError` → codul a fost compilat pentru un Java mai nou decât cel care rulează
- `release version XX not supported` → JDK-ul tău e prea vechi pentru ce cere proiectul
- `Cannot find JDK` sau `No JDK found` → IntelliJ nu găsește niciun JDK instalat

### Cum instalezi JDK pe Debian
```bash
sudo apt update
sudo apt install openjdk-17-jdk
```
Dacă vrei altă versiune înlocuiești `17` cu `11` sau `21`. Poți instala mai multe versiuni în același timp.

### java --version
Când ai mai multe JDK-uri instalate pe sistem, `java --version` îți arată care e activ în terminal în acel moment.
```bash
java --version
# openjdk 17.0.x ...
```

Dacă ai mai multe instalate și vrei să schimbi care e activ:
```bash
sudo update-alternatives --config java
```
Îți apare o listă cu toate JDK-urile instalate și alegi cu numărul corespunzător.

> **Important:** ce setezi în terminal cu `update-alternatives` și ce setezi în IntelliJ sunt **independente**. IntelliJ are propria sa setare de JDK (cea din Project Structure) și o ignoră pe cea din terminal.

### JAVA_HOME — rulezi cu un JDK specific fără să schimbi global
```bash
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 mvn spring-boot:run
```
Asta rulează comanda `mvn` folosind exact JDK-ul specificat, fără să schimbi setarea globală. E util când ai mai multe versiuni instalate și vrei să rulezi un proiect specific cu una anume, fără să afectezi restul.

```bash
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew bootRun
```

### Setezi JAVA_HOME permanent în sesiunea curentă
```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
java --version  # verifici că s-a schimbat
```

---

## Maven (mvn)

### Ce face mvn
E comanda Maven din terminal. Când rulezi `mvn spring-boot:run`, Maven citește `pom.xml`, descarcă librăriile lipsă și pornește aplicația. E echivalentul butonului Run din IntelliJ dar din terminal.

### Cum instalezi Maven pe Debian
```bash
sudo apt update
sudo apt install maven
mvn --version  # verifici că s-a instalat
```

### Comenzi Maven uzuale
```bash
mvn spring-boot:run          # pornești aplicația Spring Boot
mvn package                  # compilezi și creezi JAR/WAR (folosești când ai server)
mvn package -DskipTests      # compilezi fără să rulezi testele
mvn clean                    # ștergi folderul target/
mvn clean package            # curat + build complet
mvn clean install            # build + instalezi în repository-ul local (~/.m2)
./mvnw spring-boot:run       # la fel ca mvn dar folosește Maven Wrapper (nu necesită Maven instalat)
```

### Erorile de versiune Kotlin (pom.xml)
```
error: Failed to query the value of the 'jvmTarget' attribute
Kotlin: API version X is greater than language version
```
Cauza: versiunea de Kotlin din `pom.xml` e incompatibilă cu JDK-ul selectat.
- Kotlin 1.5.31 + JDK 24 → poate da erori
- Kotlin 1.9.x + JDK 17/21 → merge fără probleme

**Soluția:** schimbi în `pom.xml`:
```xml
<kotlin.version>1.9.25</kotlin.version>
```
Apoi dai **Maven → Reload** (iconița de refresh din panoul Maven din IntelliJ).

---

## Gradle (gradlew)

### Cum funcționează gradlew
Când rulezi `./gradlew bootRun` prima dată:
1. `gradlew` citește din `gradle/wrapper/gradle-wrapper.properties` ce versiune de Gradle e necesară
2. Dacă nu e descărcată deja, o descarcă automat de pe internet
3. Rulează build-ul cu exact acea versiune

`gradlew` vs `gradlew.bat`:
- `gradlew` → pentru Linux/Mac (bash script)
- `gradlew.bat` → pentru Windows (batch script)

IntelliJ le folosește automat în fundal când dai Run.

### Comenzi Gradle uzuale
```bash
./gradlew bootRun            # pornești aplicația Spring Boot
./gradlew build              # compilezi proiectul
./gradlew build -x test      # build fără teste
./gradlew clean              # ștergi folderul build/
./gradlew clean build        # curat + build complet
./gradlew tasks              # vezi toate task-urile disponibile
```

Dacă primești `Permission denied`:
```bash
chmod +x gradlew
./gradlew bootRun
```

### Echivalentul pom.xml → build.gradle.kts

| Ce info | Maven (pom.xml) | Gradle (build.gradle.kts) |
|---|---|---|
| Spring Boot version | `<version>2.1.9.RELEASE</version>` la parent | `id("org.springframework.boot") version "3.3.5"` |
| Kotlin version | `<kotlin.version>1.5.31</kotlin.version>` | `kotlin("jvm") version "1.9.25"` |
| Java version | `<java.version>` | `sourceCompatibility = JavaVersion.VERSION_17` și `jvmTarget = "17"` |
| Dependențe | `<dependency>` | `implementation(...)` |

### Cum rulezi pe Debian (fără IntelliJ)

| Proiect | Ce instalezi | Cum rulezi |
|---|---|---|
| Gradle | doar JDK | `./gradlew bootRun` |
| Maven | doar JDK | `./mvnw spring-boot:run` |

Gradle și Maven Wrapper se descarcă singure — nu trebuie instalate separat.

---

## Spring Boot — cum îți dai seama cum se pornește proiectul

1. **Are `pom.xml`?** → Da → e Maven
2. **Are `Dockerfile`?** → Dacă nu → poate fi pornit direct fără Docker
3. **Are un singur modul?** → Un singur `pom.xml` la rădăcină → nu trebuie pornite mai multe microservicii
4. **Are `@SpringBootApplication` și funcție `main`?** → Da → dai Run direct, click pe săgeata verde de lângă `fun main`

### Porturi Spring Boot
```bash
# schimbi portul din application.properties
server.port=8081

# sau din terminal
java -jar app.jar --server.port=8081
JAVA_HOME=... mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

---

## Docker

### Comenzi de bază
```bash
docker ps                        # containere care rulează acum
docker ps -a                     # toate containerele (inclusiv oprite)
docker images                    # imagini descărcate local
docker stop <container_id>       # oprești un container
docker rm <container_id>         # ștergi un container oprit
docker rmi <image_id>            # ștergi o imagine
docker logs <container_id>       # vezi log-urile unui container
docker logs -f <container_id>    # urmărești log-urile în timp real
docker exec -it <container_id> bash   # intri în container cu bash
```

### docker-compose vs docker compose
```bash
# varianta veche (docker-compose separat instalat)
docker-compose up
docker-compose up -d             # în background (detached)
docker-compose down              # oprești și ștergi containerele
docker-compose down -v           # oprești + ștergi și volumele (date)
docker-compose logs -f           # urmărești log-urile
docker-compose build             # rebuild imagini

# varianta nouă (integrată în Docker, fără cratimă)
docker compose up
docker compose up -d
docker compose down
docker compose down -v
docker compose logs -f
docker compose build
```

### Rebuild complet (când ai schimbat cod)
```bash
docker-compose down
docker-compose build
docker-compose up
# sau într-o singură comandă:
docker-compose up --build
```

---

## Porturi — cum vezi ce rulează și cum omori procesul

### Pe Linux/Debian
```bash
# vezi ce proces ascultă pe un port (ex: 8080)
sudo lsof -i :8080
sudo ss -tulnp | grep 8080
sudo netstat -tulnp | grep 8080

# omori procesul după PID
kill -9 <PID>

# sau direct fără să cauți PID-ul
sudo fuser -k 8080/tcp
```

### Pe Windows (PowerShell)
```powershell
# vezi ce rulează pe portul 8080
netstat -ano | findstr :8080

# omori procesul după PID (coloana din dreapta din netstat)
taskkill /PID <PID> /F

# sau după nume
taskkill /IM java.exe /F
```

### Porturi comune
| Serviciu | Port implicit |
|---|---|
| Spring Boot | 8080 |
| Kafka | 9092 |
| Zookeeper | 2181 |
| RabbitMQ | 5672 (AMQP), 15672 (management UI) |
| PostgreSQL | 5432 |
| MySQL | 3306 |
| Redis | 6379 |

---

## Kafka

### Pornire Kafka (cu Docker Compose)
De obicei proiectele au un `docker-compose.yml` cu Kafka + Zookeeper. Rulezi:
```bash
docker-compose up -d
```

### Comenzi Kafka din terminal (în container sau cu Kafka instalat)
```bash
# listezi topic-urile existente
kafka-topics.sh --list --bootstrap-server localhost:9092

# creezi un topic
kafka-topics.sh --create --topic nume-topic --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1

# ștergi un topic
kafka-topics.sh --delete --topic nume-topic --bootstrap-server localhost:9092

# detalii despre un topic
kafka-topics.sh --describe --topic nume-topic --bootstrap-server localhost:9092

# trimiți un mesaj (producer)
kafka-console-producer.sh --topic nume-topic --bootstrap-server localhost:9092
# după ce rulezi comanda, scrii mesajele și dai Enter

# citești mesaje (consumer)
kafka-console-consumer.sh --topic nume-topic --bootstrap-server localhost:9092 --from-beginning

# citești doar mesajele noi (fără --from-beginning)
kafka-console-consumer.sh --topic nume-topic --bootstrap-server localhost:9092
```

### Rulezi comenzile Kafka direct în containerul Docker
```bash
# intri în containerul Kafka
docker exec -it <kafka_container_name> bash

# sau rulezi direct fără să intri
docker exec <kafka_container_name> kafka-topics.sh --list --bootstrap-server localhost:9092
```

---

## RabbitMQ

### Pornire RabbitMQ (cu Docker)
```bash
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:management
```

### Management UI
Deschizi în browser: `http://localhost:15672`
- User: `guest`
- Parolă: `guest`

### Comenzi utile RabbitMQ
```bash
# listezi queue-urile
rabbitmqctl list_queues

# listezi exchange-urile
rabbitmqctl list_exchanges

# listezi binding-urile
rabbitmqctl list_bindings

# status RabbitMQ
rabbitmqctl status
```

---

## Diverse

### Verifici ce ai instalat
```bash
java --version
mvn --version
gradle --version
docker --version
docker-compose --version
python3 --version
```

### Omori orice proces Java blocat
```bash
# Linux
pkill -f java
kill -9 $(pgrep java)

# Windows
taskkill /IM java.exe /F
```

### Curăță cache Maven (dacă ai dependențe corupte)
```bash
rm -rf ~/.m2/repository
# sau doar pentru un artifact specific
rm -rf ~/.m2/repository/org/springframework
```

### Curăță cache Gradle
```bash
rm -rf ~/.gradle/caches
```
