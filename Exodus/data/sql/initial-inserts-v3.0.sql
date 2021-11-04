--
-- Contains initial data inserts for the Exodus project
-- Intended to be imported after the Exodus SQL document
--

INSERT INTO api_key(token, name) VALUES ('d643a487-0628-426c-99cc-9d031d38c1d9', 'Postman Test Key');

-- Inserts email validation purposes
-- 1: New user creation
-- 2: Password reset
-- 3: Email change (by modifying user settings)
INSERT INTO email_validation_purpose(id, name_en) VALUES
    (1, 'User Creation'),
    (2, 'Password Reset'),
    (3, 'Email Change');