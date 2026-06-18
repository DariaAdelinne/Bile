====================================================================
  BILET 04 - Lab 8: Chat P2P cu ActiveUsersManager + Corutine
====================================================================

CERINTA:
  Pornind de la chat-ul din Lab 8:
  - Se ELIMINA MessageManagerMicroservice (procesorul central)
  - Se ADAUGA ActiveUsersManagerMicroservice (registru utilizatori activi)
  - Comunicarea este DIRECTA (point-to-point) intre Teacher si Student
  - Se utilizeaza CORUTINE in loc de thread {}

--------------------------------------------------------------------
MODIFICARI FATA DE LABORATORUL 8:
--------------------------------------------------------------------

  ELIMINAT:
    - MessageManagerMicroservice (nu mai roteaza mesaje)

  ADAUGAT:
    - ActiveUsersManagerMicroservice (port 1500) - NOU
      Mentine HashMap<name -> (host, port)>
      Protocol: REGISTER / GETUSER / GETLIST / UNREGISTER

  MODIFICAT StudentMicroservice:
    - Alege port DINAMIC (ServerSocket(0)) la pornire
    - Se INREGISTREAZA la ActiveUsersManager: REGISTER {name} {port}
    - Asculta conexiuni DIRECTE de la teacheri pe portul ales
    - Foloseste launch(Dispatchers.IO) in loc de thread {}
    - La shutdown: UNREGISTER {name}

  MODIFICAT TeacherMicroservice:
    - Interogheaza ActiveUsersManager: GETUSER {name} -> host:port
    - Se conecteaza DIRECT la student (fara intermediar)
    - Foloseste suspend fun + withContext(Dispatchers.IO) in loc de thread {}

--------------------------------------------------------------------
ARHITECTURA:
--------------------------------------------------------------------

  [StudentMicroservice A]  <--- conexiune TCP directa --- [TeacherMicroservice]
         |                                                        |
         | REGISTER A portX                              GETUSER A -> localhost:portX
         v                                                        |
  [ActiveUsersManagerMicroservice :1500]  <-----GETUSER-----------+

  Flow complet:
  1. StudentA porneste, alege portX dinamic, trimite REGISTER A portX
  2. StudentB porneste, alege portY dinamic, trimite REGISTER B portY
  3. Teacher porneste, trimite GETLIST -> vede A si B
  4. Teacher face VERIFICARE A -> ActiveUsersManager raspunde "localhost:portX"
  5. Teacher se conecteaza DIRECT la localhost:portX
  6. Teacher trimite INTREBARE "Ce este Kotlin?" -> StudentA raspunde direct
  7. La inchidere, StudentA trimite UNREGISTER A

Principii SOLID:
  S - fiecare microserviciu are o singura responsabilitate
  O - ActiveUsersManager poate fi extins (ex: TTL pentru useri) fara modificari
  L - StudentMicroservice poate fi inlocuit cu orice implementare a protocolului
  I - protocolul ActiveUsersManager are comenzi clare si separate
  D - Teacher depinde de protocolul ActiveUsersManager, nu de implementare

--------------------------------------------------------------------
STRUCTURA FISIERE:
--------------------------------------------------------------------

  ActiveUsersManagerMicroservice/
    pom.xml
    src/main/kotlin/com/sd/laborator/
      ActiveUsersManagerMicroservice.kt

  StudentMicroservice/
    pom.xml
    questions_database.txt              <- baza de date intrebari/raspunsuri
    src/main/kotlin/com/sd/laborator/
      StudentMicroservice.kt

  TeacherMicroservice/
    pom.xml
    src/main/kotlin/com/sd/laborator/
      TeacherMicroservice.kt

--------------------------------------------------------------------
COMPILARE SI RULARE:
--------------------------------------------------------------------

  Din fiecare director (ActiveUsersManagerMicroservice/, StudentMicroservice/,
  TeacherMicroservice/), ruleaza:
    mvn clean package -DskipTests

  Ordinea de pornire:
  Terminal 1:  java -jar ActiveUsersManagerMicroservice/target/ActiveUsersManagerMicroservice-1.0-SNAPSHOT-jar-with-dependencies.jar
  Terminal 2:  java -jar StudentMicroservice/target/StudentMicroservice-1.0-SNAPSHOT-jar-with-dependencies.jar Popescu
  Terminal 3:  java -jar StudentMicroservice/target/StudentMicroservice-1.0-SNAPSHOT-jar-with-dependencies.jar Ionescu
  Terminal 4:  java -jar TeacherMicroservice/target/TeacherMicroservice-1.0-SNAPSHOT-jar-with-dependencies.jar

  NOTA: questions_database.txt trebuie sa fie in acelasi director de unde
  rulezi java pentru StudentMicroservice!
  Copiaza-l langa JAR inainte de rulare.

--------------------------------------------------------------------
SESIUNE DE COMENZI TEACHER (exemplu):
--------------------------------------------------------------------

  [neconectat] > GETLIST
  Studenti activi:
    Popescu localhost 54321
    Ionescu localhost 54322

  [neconectat] > VERIFICARE Popescu
  ActiveUsersManager: localhost:54321
  Conectat direct la Popescu @ localhost:54321

  [conectat la: Popescu] > INTREBARE Unde se da al 3-lea razboi mondial?
  Raspuns de la Popescu: Pe Facebook

  [conectat la: Popescu] > EXIT
  Deconectat de la Popescu.

  [neconectat] > QUIT
  Iesire...

====================================================================
