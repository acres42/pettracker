CREATE TABLE pets (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  type VARCHAR(50) NOT NULL,
  breed VARCHAR(100),
  age INT,
  weight_lbs INT,
  description VARCHAR(500),
  image_url VARCHAR(255),
  INDEX idx_pets_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
