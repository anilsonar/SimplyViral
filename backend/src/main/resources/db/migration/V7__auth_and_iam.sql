-- V7: IAM and Identity expansions (Passwords, OTPs, OAuth Tokens)

ALTER TABLE users ADD COLUMN password_hash VARCHAR(255);
ALTER TABLE users ADD COLUMN mobile_number VARCHAR(20) UNIQUE;
ALTER TABLE users ADD COLUMN is_email_verified BOOLEAN DEFAULT false;
ALTER TABLE users ADD COLUMN is_mobile_verified BOOLEAN DEFAULT false;

ALTER TABLE auth_identities ADD COLUMN access_token TEXT;
ALTER TABLE auth_identities ADD COLUMN refresh_token TEXT;
ALTER TABLE auth_identities ADD COLUMN expires_at TIMESTAMP WITH TIME ZONE;

CREATE TABLE otps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255),
    mobile_number VARCHAR(20),
    otp_code VARCHAR(10) NOT NULL,
    type VARCHAR(20) NOT NULL, -- 'EMAIL' or 'MOBILE'
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_otps_email ON otps(email);
CREATE INDEX idx_otps_mobile ON otps(mobile_number);
