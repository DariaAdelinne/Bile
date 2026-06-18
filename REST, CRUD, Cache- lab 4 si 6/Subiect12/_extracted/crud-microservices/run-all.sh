#!/bin/bash
# ============================================================
# Porneste toate cele 5 microservicii simultan
# Fiecare ruleaza in background, logurile merg in fisiere .log
# ============================================================

JAVA_HOME_PATH="${JAVA_HOME:-$HOME/jdk-11.0.23+9}"
export JAVA_HOME="$JAVA_HOME_PATH"

echo "=== CRUD Microservices - BeerApp ==="
echo "Folosesc JAVA_HOME=$JAVA_HOME"
echo ""

# Copiem beer.db catre toate serviciile CRUD (acelasi fisier SQLite)
touch beer.db
for svc in create-service read-service update-service delete-service; do
    cp beer.db $svc/beer.db 2>/dev/null || true
done

start_service() {
    local dir=$1
    local name=$2
    local port=$3
    echo "Pornesc $name pe portul $port ..."
    cd $dir
    mvn spring-boot:run > "../${name}.log" 2>&1 &
    echo $! > "../${name}.pid"
    cd ..
}

start_service "create-service" "CreateService" 8081
start_service "read-service"   "ReadService"   8082
start_service "update-service" "UpdateService" 8083
start_service "delete-service" "DeleteService" 8084

echo "Astept 20 secunde ca serviciile CRUD sa porneasca..."
sleep 20

start_service "api-gateway" "ApiGateway" 8080

echo ""
echo "Toate serviciile pornite. Loguri in: *.log"
echo ""
echo "Initializeaza tabela:"
echo "  curl -X POST http://localhost:8080/api/beer/init"
echo ""
echo "Adauga o bere:"
echo "  curl -X POST http://localhost:8080/api/beer -H 'Content-Type: application/json' -d '{\"name\":\"Corona\",\"price\":3.6}'"
echo ""
echo "Lista beri:"
echo "  curl http://localhost:8080/api/beer"
echo ""
echo "Opreste totul cu: kill \$(cat *.pid)"
