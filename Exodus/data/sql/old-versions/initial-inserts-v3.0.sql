--
-- Contains initial data inserts for the Exodus project
-- Intended to be imported after the Exodus SQL document
--

-- Insert your own generated API key here if you want to use one
-- INSERT INTO api_key(token, name) VALUES ('*******-****-****-****-************', 'Test API Key');

-- Inserts email validation purposes
-- 1: New user creation
-- 2: Password reset
-- 3: Email change (by modifying user settings)
INSERT INTO email_validation_purpose(id, name_en) VALUES
    (1, 'User Creation'),
    (2, 'Password Reset'),
    (3, 'Email Change');