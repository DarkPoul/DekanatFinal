#!/usr/bin/env bash
set -euo pipefail

#############################################
# 0. Налаштування доступу до ПРОД бази (тільки читати, щоб зняти дамп)
#    Тут ми беремо користувача, якому дозволено підключення не лише локально.
#    Ти дав ці значення:
#      host/ip:
#      user:
#      pass:
#      db:
#############################################

FETCH_DB_HOST=
FETCH_DB_PORT=
FETCH_DB_NAME=
FETCH_DB_USER=
FETCH_DB_PASS=

# Цю ж базу ми будемо клонувати, тому логічна "оригінальна" назва:
PROD_DB_NAME="${FETCH_DB_NAME}"

# Пароль, який ми задаємо root у ТИМЧАСОВІЙ тестовій БД
# (просто візьмемо той самий, щоб не вигадувати інший)
TEST_DB_ROOT_PASS="${FETCH_DB_PASS}"

#############################################
# 1. Параметри тестового стенду
#############################################

TEST_MYSQL_IMAGE="mysql:8.0.36"
TEST_APP_PORT=8081   # порт, на якому ти дивишся тест (назовні хоста)

TIMESTAMP=$(date +%Y%m%d%H%M%S)
TEST_ID="test-${TIMESTAMP}"

TEST_NET="dekanat-${TEST_ID}-net"
TEST_DB_CONTAINER="mysql-${TEST_ID}"
TEST_APP_CONTAINER="dekanat-app-${TEST_ID}"

# Нова назва бази в тестовому контейнері, щоб точно не перетнутися
TEST_DB_NAME="${PROD_DB_NAME}_${TEST_ID}"

DUMP_FILE="dump-${TEST_ID}.sql"

echo "[INFO] Будемо робити клон бази '${FETCH_DB_NAME}' → нова тестова '${TEST_DB_NAME}'"
echo "[INFO] Тестовий стенд ID: ${TEST_ID}"
echo

#############################################
# 2. Оновити код і зібрати docker-образ застосунку
#    ВАЖЛИВО:
#    Dockerfile повинен бути тим варіантом з java -jar і SPRING_PROFILES_ACTIVE=test,
#    який ми вже обговорювали.
#############################################

echo "[1/8] Git pull..."
git pull || true

echo "[2/8] Docker build застосунку (профіль test)..."
COMMIT_SHA=$(git rev-parse --short HEAD)
IMAGE_TAG="dekanat-app:${COMMIT_SHA}"

docker build -t "${IMAGE_TAG}" .

#############################################
# 3. Зняти дамп з продакшн-бази
#    Тепер ми використовуємо root_user@212.111.203.173,
#    тобто користувача, який має право підключатися не лише як localhost.
#############################################

echo "[3/8] Створюємо дамп прод-БД (${DUMP_FILE})..."
mysqldump \
  --single-transaction \
  --routines \
  --triggers \
  --no-tablespaces \
  -h "${FETCH_DB_HOST}" \
  -P "${FETCH_DB_PORT}" \
  -u "${FETCH_DB_USER}" \
  -p"${FETCH_DB_PASS}" \
  "${FETCH_DB_NAME}" > "${DUMP_FILE}"


#############################################
# 4. Підняти окрему docker network для тестового стенду
#############################################

echo "[4/8] Створюємо окрему docker network: ${TEST_NET}"
docker network create "${TEST_NET}"

echo "[5/8] Піднімаємо тестовий MySQL контейнер ${TEST_DB_CONTAINER}..."
docker run -d \
  --name "${TEST_DB_CONTAINER}" \
  --network "${TEST_NET}" \
  -p "${TEST_DB_PUBLISH_PORT}":3306 \        # або 127.0.0.1:${TEST_DB_PUBLISH_PORT}:3306
  # shellcheck disable=SC2215
  -e MYSQL_ROOT_PASSWORD="${TEST_DB_ROOT_PASS}" \
  -e MYSQL_DATABASE="${TEST_DB_NAME}" \
  "${TEST_MYSQL_IMAGE}"


echo "    Чекаємо, поки MySQL всередині контейнера повністю готовий приймати root + пароль..."

# 1. Чекаємо не просто "живий", а "логін працює і БД існує"
for i in {1..30}; do
  if docker exec "${TEST_DB_CONTAINER}" \
      mysql \
        -h 127.0.0.1 \
        -u root \
        -p"${TEST_DB_ROOT_PASS}" \
        -e "SELECT 1 FROM DUAL;" "${TEST_DB_NAME}" >/dev/null 2>&1; then
    echo "    MySQL повністю готовий ✅"
    DB_READY=1
    break
  fi
  echo "    ...ще ініціалізується (${i}), чекаю 2с"
  sleep 2
done

if [ "${DB_READY:-0}" != "1" ]; then
  echo "❌ MySQL так і не готовий приймати підключення root до ${TEST_DB_NAME}"
  echo "   (можливо контейнер падає або пароль не застосувався)"
  exit 1
fi

echo "[6/8] Імпортуємо дамп у тестову БД ${TEST_DB_NAME}..."
docker exec -i "${TEST_DB_CONTAINER}" \
  mysql \
    -h 127.0.0.1 \
    -u root \
    -p"${TEST_DB_ROOT_PASS}" \
    "${TEST_DB_NAME}" < "${DUMP_FILE}"

#############################################
# 7. Запустити тестовий застосунок
#
# ВАЖЛИВО:
# - Застосунок і тестова БД знаходяться в одній мережі ${TEST_NET},
#   тому застосунок може звертатися до БД по імені контейнера ${TEST_DB_CONTAINER}
# - Ми встановлюємо SPRING_PROFILES_ACTIVE=test,
#   а в application-test.yaml datasource читається з env:
#     DB_URL, DB_USER, DB_PASSWORD
#############################################

DB_URL_TEST="jdbc:mysql://${TEST_DB_CONTAINER}:3306/${TEST_DB_NAME}?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"

echo "[7/8] Запускаємо тестовий застосунок ${TEST_APP_CONTAINER} на порт ${TEST_APP_PORT}..."
docker run -d \
  --name "${TEST_APP_CONTAINER}" \
  --network "${TEST_NET}" \
  -p ${TEST_APP_PORT}:8080 \
  -e SPRING_PROFILES_ACTIVE="test" \
  -e DB_URL="${DB_URL_TEST}" \
  -e DB_USER="root" \
  -e DB_PASSWORD="${TEST_DB_ROOT_PASS}" \
  "${IMAGE_TAG}"

#############################################
# 8. Фінальна інформація
#############################################

echo "[8/8] Готово ✅"
echo
echo "Стенд ID:         ${TEST_ID}"
echo "App контейнер:    ${TEST_APP_CONTAINER}"
echo "DB контейнер:     ${TEST_DB_CONTAINER}"
echo "Docker network:   ${TEST_NET}"
echo "Локальний дамп:   ${DUMP_FILE}"
echo
echo "Зайти у тест:"
echo "  http://<твоє_ім'я_сервера>:${TEST_APP_PORT}/test"
echo
echo "Щоб прибрати стенд після тесту:"
echo "  ./stop-test.sh ${TEST_ID}"
echo