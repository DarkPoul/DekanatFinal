-- Міграція для розділення зв'язку груп і навчальних планів та синхронізації студентів.

-- Крок 1. Створення проміжної таблиці відповідності груп до планів.
CREATE TABLE IF NOT EXISTS group_plans (
    group_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    PRIMARY KEY (group_id, plan_id),
    CONSTRAINT fk_group_plans_group FOREIGN KEY (group_id) REFERENCES student_group(id),
    CONSTRAINT fk_group_plans_plan FOREIGN KEY (plan_id) REFERENCES plans(id)
) ENGINE = InnoDB;

-- Крок 2. Первинне наповнення group_plans на основі поточного поля plans.group_id.
INSERT IGNORE INTO group_plans (group_id, plan_id)
SELECT DISTINCT p.group_id, p.id
FROM plans p
WHERE p.group_id IS NOT NULL;

-- Крок 3. Робимо plans.group_id необов'язковим (legacy-поле, залишаємо для зворотної сумісності).
ALTER TABLE plans MODIFY COLUMN group_id BIGINT NULL;

-- Крок 4. Синхронізуємо студентів із їхніми фактичними планами.
INSERT IGNORE INTO student_plans (plan_id, student_id)
SELECT gp.plan_id, s.id
FROM student s
JOIN group_plans gp ON gp.group_id = s.group_id
WHERE s.group_id IS NOT NULL;

-- Нова бізнес-логіка перенесення студента між групами:
--   крок 1: update student set group_id = :new_group_id where id = :student_id;
--   крок 2: insert ignore into student_plans (plan_id, student_id)
--           select gp.plan_id, :student_id from group_plans gp where gp.group_id = :new_group_id;
--   Старі оцінки в таблиці marks залишаються валідними, оскільки вони прив'язані до пари (student_id, plan_id), а не до групи.

-- (Опціонально) Для аудиту переходів можна створити журнал:
--   CREATE TABLE IF NOT EXISTS student_group_history (
--       id BIGINT PRIMARY KEY AUTO_INCREMENT,
--       student_id BIGINT NOT NULL,
--       group_id BIGINT NOT NULL,
--       from_date DATETIME NOT NULL,
--       to_date DATETIME NULL,
--       CONSTRAINT fk_sgh_student FOREIGN KEY (student_id) REFERENCES student(id),
--       CONSTRAINT fk_sgh_group FOREIGN KEY (group_id) REFERENCES student_group(id)
--   ) ENGINE = InnoDB;
--   При оновленні student.group_id слід закривати поточний запис (оновлювати to_date) і створювати новий запис із новою group_id.
