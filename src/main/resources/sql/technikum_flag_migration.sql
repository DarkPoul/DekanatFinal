-- Позначка технікуму на рівні спеціальності
ALTER TABLE specialty
    ADD COLUMN IF NOT EXISTS is_technikum TINYINT(1) NOT NULL DEFAULT 0;

-- Первинне заповнення для наявних записів (наближене визначення)
UPDATE specialty
SET is_technikum = 1
WHERE LOWER(title) LIKE '%тех%' OR LOWER(abbreviation) LIKE '%тех%';
