# Lab11 - Micronaut serverless computing

Proiect complet pentru Laborator 11. Codul este scris in Kotlin + Micronaut si include:

- tema de laborator: calcul recursiv pentru sirul `a_n = a_(n-1) + 2 * (a_(n-1) / n), a_0 = 1`;
- tema 1: ciurul lui Eratostene + coada RabbitMQ + filtrarea numerelor prime primite din fisier sau JSON;
- tema 2: contor de apasari buton, evenimente trimise in RabbitMQ si salvate in MySQL;
- tema 3: pagina web cu mai multe butoane, numele butonului si numarul de apasari salvate in MySQL;
- tema 4: producator-consumator pentru RSS-ul xkcd, cu extragere perechi `<TITLE, URL>`.

Comentariile din cod sunt in romana fara diacritice.

## Varianta cea mai simpla de rulare, cu Docker

### 1. Intra in folder

```bash
cd Lab11
```

### 2. Opreste containere vechi, daca exista

```bash
docker compose down --remove-orphans
```

### 3. Porneste tot proiectul

```bash
docker compose up --build
```

Asteapta pana apare ca aplicatia Micronaut a pornit pe portul `8080`.

## Testare tema de laborator

```bash
curl "http://localhost:8080/lab/sequence?n=10"
```

Rezultatul contine termenii calculati recursiv.

## Testare tema 1 - Eratostene + RabbitMQ

### Trimite direct lista de numere in coada

```bash
curl --location --request POST "http://localhost:8080/tema1/send" \
  --header "Content-Type: application/json" \
  --data-raw '{"maxNumber":50,"numbers":[1,2,3,4,5,8,11,12,17,19,20,23,29,30,31,50]}'
```

### Proceseaza coada si returneaza doar numerele prime

```bash
curl --location --request POST "http://localhost:8080/tema1/consume"
```

### Varianta cu fisier

Fisierul `numbers.txt` este inclus in proiect. Daca rulezi local in container, calea este relativa la folderul aplicatiei doar daca fisierul exista acolo. Pentru testare rapida foloseste varianta JSON de mai sus. Daca rulezi fara Docker, poti folosi:

```bash
curl --location --request POST "http://localhost:8080/tema1/send-from-file" \
  --header "Content-Type: application/json" \
  --data-raw '{"filePath":"numbers.txt","maxNumber":50}'
```

## Testare tema 2 si tema 3 - butoane, RabbitMQ, MySQL

Deschide in browser:

```text
http://localhost:8080/
```

sau:

```text
http://localhost:8080/tema3
```

Apasa butoanele, apoi apasa `Proceseaza coada`, apoi `Afiseaza contoare`.

Testare din terminal:

```bash
curl --location --request POST "http://localhost:8080/api/click/like"
curl --location --request POST "http://localhost:8080/api/click/subscribe"
curl --location --request POST "http://localhost:8080/api/process-clicks"
curl "http://localhost:8080/api/buttons"
```

RabbitMQ Management este disponibil la:

```text
http://localhost:15672
user: guest
parola: guest
```

## Testare tema 4 - xkcd RSS producator-consumator

```bash
curl "http://localhost:8080/tema4/run"
```

Aplicatia descarca `https://xkcd.com/atom.xml`, parseaza XML-ul si intoarce o lista de perechi `title` si `url`.

## Rulare fara Docker, daca ai Gradle instalat

Porneste separat MySQL si RabbitMQ, apoi seteaza variabilele de mediu:

```bash
export RABBIT_HOST=localhost
export RABBIT_PORT=5672
export RABBIT_USER=guest
export RABBIT_PASSWORD=guest
export MYSQL_URL='jdbc:mysql://localhost:3306/lab11?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true'
export MYSQL_USER=lab11
export MYSQL_PASSWORD=lab11
```

Ruleaza aplicatia:

```bash
gradle run
```

Ruleaza testele:

```bash
gradle test
```

## Structura importanta

```text
src/main/kotlin/com/sd/laborator/Application.kt
src/main/kotlin/com/sd/laborator/controllers/LabController.kt
src/main/kotlin/com/sd/laborator/controllers/PrimeHomeworkController.kt
src/main/kotlin/com/sd/laborator/controllers/ButtonController.kt
src/main/kotlin/com/sd/laborator/controllers/XkcdController.kt
src/main/kotlin/com/sd/laborator/functions/ServerlessFunctions.kt
src/main/kotlin/com/sd/laborator/services/EratosteneSieveService.kt
src/main/kotlin/com/sd/laborator/services/RabbitService.kt
src/main/kotlin/com/sd/laborator/services/DatabaseService.kt
src/main/kotlin/com/sd/laborator/services/RecursiveSequenceService.kt
```

## Observatie pentru profesor

Am pastrat ideea de functie serverless prin clase separate de tip functie/producator/consumator, dar am expus testarea prin controllere HTTP ca in laborator. Astfel proiectul poate fi verificat usor cu `curl`, browser, RabbitMQ si MySQL.
