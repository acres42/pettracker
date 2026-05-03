UPDATE pets
SET gender = CASE
  WHEN MOD(id, 2) = 0 THEN 'female'
  ELSE 'male'
END
WHERE gender IS NULL;

ALTER TABLE pets
  MODIFY COLUMN gender ENUM('male', 'female') NOT NULL;
