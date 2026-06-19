# Cum rulez — Subiect 39

## Cerinte

- Java 17 instalat
- Conexiune la internet (pentru a accesa xkcd.com/atom.xml)

## Rulare din IntelliJ

1. Deschide folderul `Subiect39` ca proiect Gradle in IntelliJ
2. Asteapta sa se incarce dependentele
3. Ruleaza `Application.kt` (click dreapta → Run)

## Rulare din terminal

```bash
cd Subiect39
chmod +x gradlew
./gradlew bootRun
```

Sau build + run:

```bash
./gradlew build
java -jar build/libs/subiect39-xkcd-rss-1.0.0.jar
```

## Ce face aplicatia

1. `XkcdRssProducer` (Supplier) — face request HTTP la `https://xkcd.com/atom.xml` si returneaza XML-ul
2. `XkcdRssConsumer` (Consumer) — parseaza XML-ul si afiseaza la consola lista de perechi:

```
<Titlu comic, https://xkcd.com/NNN/>
```

## Output asteptat

```
[XkcdRssProducer] Preiau fluxul RSS de la https://xkcd.com/atom.xml ...
[XkcdRssProducer] XML primit: XXXX caractere
[XkcdRssConsumer] Procesez XML primit...

======================================================================
XKCD RSS — 4 intrari gasite
======================================================================
1. <Titlu Comic 1, https://xkcd.com/NNN/>
2. <Titlu Comic 2, https://xkcd.com/MMM/>
...
======================================================================
```
