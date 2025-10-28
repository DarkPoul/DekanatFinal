#!/usr/bin/env bash
set -euo pipefail

###
# 0. Підтягуємо чутливі змінні з .env
#   (.env повинен лежати поруч зі скриптом або вкажи шлях)
###

if [ -f .env ]; then
  # експортуємо всі ключ=значення з .env в змінні середовища
  # (set -a вмикає "автоматично export", потім вимикаємо)
  set -a
  source .env
  set +a
else
  echo "Помилка: файл .env не знайдено поруч зі start-test.sh"
  exit 1
fi

###
# 1. Виймаємо дані про прод-БД з уже завантажених змінних
###

# Приклад DB_URL:
#   jdbc:mysql://dekanat-mysql:3306/dekanat
# Нам треба:
#   PROD_DB_CONTAINER = dekanat-mysql
#   PROD_DB_NAME      = dekanat

RAW_URL="${DB_URL#jdbc:mysql://}"        # обрізаємо 'jdbc:mysql://'
# тепер щось типу 'dekanat-mysql:3306/dekanat'

HOST_AND_DB="${RAW_URL#*/}"              # обрізаємо все до першого '/' включно -> лишається 'dekanat'
PROD_DB_NAME="${HOST_AND_DB%%\?*}"       # на випадок параметрів типу '?useSSL=false', зрізаємо їх
# але нам ще треба hostname контейнера:
HOST_AND_PORT="${RAW_URL%%/*}"           # беремо все до першого '/' -> 'dekanat-mysql:3306'
PROD_DB_CONTAINER="${HOST_AND_PORT%%:*}" # беремо все до ':' -> 'dekanat-mysql'

# юзер і пароль беремо прямо з .env
PROD_DB_USER="${MYSQL_ROOT_USER}"
PROD_DB_PASS="${MYSQL_ROOT_PASSWORD}"

###
# 2. Інші налаштування тестового стенду
###

TEST_MYSQL_IMAGE="mysql:8.0.36"
TEST_APP_PORT=18080

TIMESTAMP=$(date +%Y%m%d%H%M%S)
TEST_ID="test-${TIMESTAMP}"

TEST_NET="dekanat-${TEST_ID}-net"
TEST_DB_CONTAINER="mysql-${TEST_ID}"
TEST_APP_CONTAINER="dekanat-app-${TEST_ID}"
TEST_DB_NAME="${PROD_DB_NAME}_${TEST_ID}"

DUMP_FILE="dump-${TEST_ID}.sql"

echo "[1/8] Git pull..."
git pull || true

echo "[2/8] Docker build застосунку (профіль test)..."
# Тут ми будуємо образ за ОНОВЛЕНИМ Dockerfile вище
# Отримаємо короткий sha коміту для тегу
COMMIT_SHA=$(git rev-parse --short HEAD)
IMAGE_TAG="dekanat-app:${COMMIT_SHA}"

docker build -t "${IMAGE_TAG}" .

echo "[3/8] Створюємо дамп прод-БД у локальний файл ${DUMP_FILE}..."
docker exec -i "${PROD_DB_CONTAINER}" \
  mysqldump --single-transaction --routines --triggers \
  -u"${PROD_DB_USER}" -p"${PROD_DB_PASS}" "${PROD_DB_NAME}" > "${DUMP_FILE}"

echo "[4/8] Створюємо окрему docker network: ${TEST_NET}"
docker network create "${TEST_NET}"

echo "[5/8] Піднімаємо тестовий MySQL (${TEST_DB_CONTAINER})..."
docker run -d \
  --name "${TEST_DB_CONTAINER}" \
  --network "${TEST_NET}" \
  -e MYSQL_ROOT_PASSWORD="${PROD_DB_PASS}" \
  -e MYSQL_DATABASE="${TEST_DB_NAME}" \
  "${TEST_MYSQL_IMAGE}"

echo "    Чекаємо 10 секунд поки MySQL стане доступним..."
sleep 10

echo "[6/8] Заливаємо дамп у тестову базу ${TEST_DB_NAME}..."
docker exec -i "${TEST_DB_CONTAINER}" \
  mysql -u"${PROD_DB_USER}" -p"${PROD_DB_PASS}" "${TEST_DB_NAME}" < "${DUMP_FILE}"

# !!! ОПЦІЙНО: маскування даних, скидання паролів, і т.д.
# docker exec -i "${TEST_DB_CONTAINER}" \
#   mysql -u"${PROD_DB_USER}" -p"${PROD_DB_PASS}" "${TEST_DB_NAME}" <<'EOF'
# UPDATE user SET password='test123' WHERE role='TEACHER';
# EOF

echo "[7/8] Запускаємо тестовий застосунок (${TEST_APP_CONTAINER})..."
# Формуємо DB_URL як для application-test.yml
DB_URL="jdbc:mysql://${TEST_DB_CONTAINER}:3306/${TEST_DB_NAME}?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true"

docker run -d \
  --name "${TEST_APP_CONTAINER}" \
  --network "${TEST_NET}" \
  -p ${TEST_APP_PORT}:8080 \
  -e SPRING_PROFILES_ACTIVE="test" \
  -e DB_URL="${DB_URL}" \
  -e DB_USER="${PROD_DB_USER}" \
  -e DB_PASSWORD="${PROD_DB_PASS}" \
  "${IMAGE_TAG}"

echo "[8/8] Готово ✅"
echo
echo "Стенд ID:         ${TEST_ID}"
echo "App контейнер:    ${TEST_APP_CONTAINER}"
echo "DB контейнер:     ${TEST_DB_CONTAINER}"
echo "Docker network:   ${TEST_NET}"
echo "Локальний дамп:   ${DUMP_FILE}"
echo
echo "Зайти в тест:     http://<твій_сервер>:${TEST_APP_PORT}/test"
echo "                  (контекст /test задано в application-test.yaml)"
echo
echo "Щоб знести стенд:"
echo "   ./stop-test.sh ${TEST_ID}"
echo
