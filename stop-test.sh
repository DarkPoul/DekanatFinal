#!/usr/bin/env bash
set -euo pipefail

if [ $# -ne 1 ]; then
  echo "Використання: $0 <TEST_ID>"
  echo "Приклад:     $0 test-20251028123045"
  exit 1
fi

TEST_ID="$1"

TEST_NET="dekanat-${TEST_ID}-net"
TEST_DB_CONTAINER="mysql-${TEST_ID}"
TEST_APP_CONTAINER="dekanat-app-${TEST_ID}"

echo "[1/4] Гасимо застосунок ${TEST_APP_CONTAINER}..."
if docker ps -a --format '{{.Names}}' | grep -q "^${TEST_APP_CONTAINER}$"; then
  docker rm -f "${TEST_APP_CONTAINER}"
fi

echo "[2/4] Гасимо БД ${TEST_DB_CONTAINER}..."
if docker ps -a --format '{{.Names}}' | grep -q "^${TEST_DB_CONTAINER}$"; then
  docker rm -f "${TEST_DB_CONTAINER}"
fi

echo "[3/4] Видаляємо мережу ${TEST_NET}..."
if docker network ls --format '{{.Name}}' | grep -q "^${TEST_NET}$"; then
  docker network rm "${TEST_NET}"
fi

echo "[4/4] Стенд ${TEST_ID} знищений ✅"
echo "Дамп БД (dump-${TEST_ID}.sql) залишився як локальний файл у каталозі."
echo "Якщо не треба — видали руками."
