--
-- DB Structure for Utopia Exodus features
-- Intended to be inserted after Utopia Citadel database structure
-- Supports versions v2.0 and above
--

-- OPTIONAL table
-- Contains API-keys used in authenticating requests for nodes which don't require a user session for authentication
-- (E.g. User creation or accessing the list of available languages)
-- Expected API-key format is that of a standard UUID. For example: "a2de1cbc-4ae9-4778-892c-4ad36875f38b"
CREATE TABLE api_key
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `key` VARCHAR(36) NOT NULL,
    name VARCHAR(64) NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX ak_api_key_idx (`key`)

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Contains a list of purposes for email validation (server-side only)
-- This is to limit the possible misuse of these validation tokens to contexts for which they were created
CREATE TABLE email_validation_purpose
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    name_en VARCHAR(16) NOT NULL

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

INSERT INTO email_validation_purpose(id, name_en) VALUES
    (1, 'User Creation'),
    (2, 'Password Reset'),
    (3, 'Email Change');

-- Contains email validation tokens
CREATE TABLE email_validation
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    purpose_id INT NOT NULL,
    email VARCHAR(128) NOT NULL,
    `key` VARCHAR(36) NOT NULL,
    resend_key VARCHAR(36) NOT NULL,
    owner_id INT,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_in DATETIME NOT NULL,
    actualized_in DATETIME,

    INDEX ev_email_idx (email),
    INDEX ev_key_idx (`key`),
    INDEX ev_resend_idx (resend_key),
    INDEX ev_validity_idx (expires_in, actualized_in),

    CONSTRAINT ev_evp_validation_purpose_link_fk FOREIGN KEY ev_evp_validation_purpose_link_idx (purpose_id)
        REFERENCES email_validation_purpose(id) ON DELETE CASCADE,

    CONSTRAINT ev_u_email_owner_link_fk FOREIGN KEY ev_u_email_owner_link_idx (owner_id)
            REFERENCES `user`(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Records resend attempts in order to limit possible spam
CREATE TABLE email_validation_resend
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    validation_id INT NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT evr_ev_validation_link_fb FOREIGN KEY evr_ev_validation_link_idk (validation_id)
        REFERENCES email_validation(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Contains hashed user passwords
CREATE TABLE user_authentication
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    hash VARCHAR(128) NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX ua_user_password_idx (hash, user_id),

    CONSTRAINT ua_u_link_to_owner_fk FOREIGN KEY ua_u_link_to_owner_idx (user_id)
        REFERENCES `user`(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Used for allowing a private device access to user account
CREATE TABLE device_authentication_key
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    device_id INT NOT NULL,
    `key` VARCHAR(36) NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deprecated_after DATETIME,

    INDEX dak_active_key (`key`, deprecated_after),

    CONSTRAINT dak_u_key_owner_fk FOREIGN KEY dak_u_key_owner_idx (user_id)
        REFERENCES `user`(id) ON DELETE CASCADE,

    CONSTRAINT dak_cd_connected_device_fk FOREIGN KEY dak_cd_connected_device_idx (device_id)
        REFERENCES client_device(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Represents a temporary login session
-- Model style preferences are: 1 = Full, 2 = Simple, NULL = undefined
CREATE TABLE user_session
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    device_id INT,
    `key` VARCHAR(36) NOT NULL,
    model_style_preference INT,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_in DATETIME NOT NULL,
    logout_time DATETIME,

    INDEX us_active_key (`key`, expires_in, logout_time),

    CONSTRAINT us_u_session_owner_fk FOREIGN KEY us_u_session_owner_idx (user_id)
        REFERENCES `user`(id) ON DELETE CASCADE,

    CONSTRAINT us_cd_session_device_fk FOREIGN KEY us_cd_session_device_idx (device_id)
        REFERENCES client_device(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;