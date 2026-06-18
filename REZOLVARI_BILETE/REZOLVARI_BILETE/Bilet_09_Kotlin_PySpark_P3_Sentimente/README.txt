====================================================================
  P3: Analiza sentimente stiri (Kotlin TCP Server + PySpark Direct Stream)
====================================================================

LOGICA PYSPARK: clasificare pozitiv/negativ/neutru cu word lists
API TOKEN: brmu7ovrh5r90ebn6jrg
INTERVAL TRIMITERE: 5s

--------------------------------------------------------------------
STRUCTURA:
  KotlinServer/         <- proiect Maven Kotlin
    pom.xml
    src/main/kotlin/org/example/
      Server.kt         <- main, porneste serverul TCP
      Socket3rdParty.kt <- apeluri API Finnhub (khttp)
      SocketLocal.kt    <- ServerSocket TCP pentru PySpark
  PythonStream/
    main.py             <- PySpark direct stream

--------------------------------------------------------------------
COMPILARE SI RULARE:

  1. Compileaza Kotlin (din KotlinServer/): DACA NU E INSTALAT MAVEN: sudo apt update  /n  sudo apt install -y maven
       mvn clean package -DskipTests

  2. Porneste PySpark PRIMUL (asteapta conexiunea Kotlin):
       cd PythonStream/
       spark-submit main.py
     SAU:
       python3 main.py

  3. Porneste serverul Kotlin:
       java -jar target/KotlinServer-1.0-SNAPSHOT-jar-with-dependencies.jar
     SAU ruleaza Server.kt direct din IntelliJ

  NOTA: PySpark trebuie pornit INAINTE de Kotlin
  (SocketLocal face accept() si asteapta conexiunea)

--------------------------------------------------------------------
DEPENDENTE PYTHON:
  pip install pyspark
  Apache Spark instalat si SPARK_HOME configurat

NOTA JAVA_HOME:
  In main.py: os.environ["JAVA_HOME"] = "/usr/lib/jvm/jdk1.8.0_291"
  Ajusteaza calea daca JDK-ul e in alta locatie pe VM-ul Debian.
====================================================================
