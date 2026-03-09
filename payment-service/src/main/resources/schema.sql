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