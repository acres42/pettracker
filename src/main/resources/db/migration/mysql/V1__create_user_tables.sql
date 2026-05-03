CREATE TABLE user_accounts (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(255) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT uk_user_accounts_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_preferences (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_account_id BIGINT NOT NULL,
  preferred_species VARCHAR(32),
  preferred_weight_band VARCHAR(32),
  preferred_breed VARCHAR(120),
  preferred_keywords VARCHAR(500),
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT uk_user_preferences_user UNIQUE (user_account_id),
  CONSTRAINT fk_user_preferences_user_account
    FOREIGN KEY (user_account_id)
    REFERENCES user_accounts (id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
