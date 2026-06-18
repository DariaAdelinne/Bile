====================================================================
  BILET 02 - Lab 11: Intersectie intre doua ADT-uri (Micronaut)
====================================================================

CERINTA:
  Pornind de la exemplul Eratostene din laboratorul 11, sa se
  calculeze intersectia A ∩ B unde A si B sunt doua ADT-uri cu
  cate 100 valori aleatoare. Rezultatele se depun intr-un ADT C.

--------------------------------------------------------------------
ARHITECTURA:
--------------------------------------------------------------------

  [Client HTTP / curl]
         |
         | POST / (JSON)
         v
  [Application.IntersectionController]   <- Controller Micronaut
         |
         v
  [IntersectionFunction]                 <- @FunctionBean("intersectie-adt")
         |
         | inject
         v
  [IntersectionService]                  <- @Singleton
         |
         +-- generateRandomSet() -> INumberSet (A)   <- @ADT
         +-- generateRandomSet() -> INumberSet (B)   <- @ADT
         +-- intersect(A, B)     -> INumberSet (C)   <- @ADT

  ADT:
    INumberSet (interfata)
      ^
      |
    NumberSet (implementare cu LinkedHashSet intern)

Principii SOLID respectate:
  S - fiecare clasa are o singura responsabilitate
  O - se pot adauga noi tipuri de INumberSet fara a modifica serviciul
  L - NumberSet substituie complet INumberSet
  I - INumberSet defineste clar contractul ADT-ului
  D - IntersectionService depinde de INumberSet (interfata), nu de NumberSet

--------------------------------------------------------------------
STRUCTURA FISIERE:
--------------------------------------------------------------------

  IntersectieADT/
    pom.xml
    src/main/kotlin/com/sd/laborator/
      Application.kt                   <- entry point + Controller
      IntersectionRequest.kt           <- cerere (optional: size)
      IntersectionResponse.kt          <- raspuns (setA, setB, setC, message)
      IntersectionFunction.kt          <- @FunctionBean - logica principala
      interfaces/
        INumberSet.kt                  <- interfata ADT
      model/
        NumberSet.kt                   <- implementare ADT
      services/
        IntersectionService.kt         <- generare aleatoare + intersectie
    src/main/resources/
      application.yml

--------------------------------------------------------------------
RULARE:
--------------------------------------------------------------------

  1. Compilare (din folderul IntersectieADT/):
       mvn clean package -DskipTests -Plocal

  2. Rulare:
       java -jar target/IntersectieADT-0.1.jar

     SAU deschide in IntelliJ si ruleaza Application.kt
     (cu profilul "local" activ in Run Configuration: -Plocal)

  3. Test cu curl:
       # Rulare cu dimensiune implicita (100 elemente per multime)
       curl -X POST http://localhost:8080 \
            -H "Content-Type: application/json" \
            -d '{"size": 100}'

       # Raspuns JSON:
       # {
       #   "setA": [12, 45, 78, ...],   <- 100 elemente
       #   "setB": [34, 12, 90, ...],   <- 100 elemente
       #   "setC": [12, ...],           <- elementele comune A ∩ B
       #   "message": "A(100 elemente) ∩ B(100 elemente) = C(X elemente comune)"
       # }

--------------------------------------------------------------------
NOTA IMPORTANTA (IntelliJ):
--------------------------------------------------------------------
  La Run Configuration pentru Application.kt, adauga in VM options:
    -Dmicronaut.environments=local
  sau seteaza Active profiles: local

  Alternativ, in pom.xml, profilul "local" este deja definit si
  adauga micronaut-http-server-netty necesar pentru a rula ca server HTTP.

====================================================================
