#!/usr/bin/env bash
set -e
for d in HeartbeatMicroservice RegistryMicroservice MonitorMicroservice GradesMicroservice AssistantMicroservice MessageManagerMicroservice TeacherMicroservice StudentMicroservice; do
  echo "========== Building $d =========="
  (cd "$d" && mvn -q clean package)
done
echo "Gata: toate JAR-urile sunt in target/. Acum ruleaza: docker compose up --build"
