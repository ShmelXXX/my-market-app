CREATE TABLE IF NOT EXISTS payments (
                                        id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    order_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    amount BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE,
                               payment_method VARCHAR(50),
    return_url VARCHAR(500)
    );

-- таблица балансов пользователей
CREATE TABLE IF NOT EXISTS user_balances (
                                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                             user_id VARCHAR(255) NOT NULL UNIQUE,
    balance BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'RUB',
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Инициализация тестовых балансов
MERGE INTO user_balances (user_id, balance, currency) KEY(user_id) VALUES ('user1', 100000, 'RUB');
MERGE INTO user_balances (user_id, balance, currency) KEY(user_id) VALUES ('user2', 50000, 'RUB');
MERGE INTO user_balances (user_id, balance, currency) KEY(user_id) VALUES ('user3', 1000, 'RUB');
MERGE INTO user_balances (user_id, balance, currency) KEY(user_id) VALUES ('admin', 200000, 'RUB');