-- 
-- Database structure for Exodus models
-- Version: v4.0
-- Last generated: 2022-02-18
--

--	Auth	----------

-- Represents an access right requirement and/or category.
-- name:    Technical name or identifier of this scope
-- created: Time when this scope was first created
CREATE TABLE `scope`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`name` VARCHAR(32) NOT NULL, 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- An enumeration for different types of authentication tokens available
-- name:               Name of this token type for identification. Not localized.
-- duration_minutes:   Duration that determines how long these tokens remain valid after issuing. None if these tokens don't expire automatically.
-- refreshed_type_id:  Id of the type of token that may be acquired by using this token type as a refresh token, if applicable
-- created:            Time when this token type was first created
-- is_single_use_only: Whether tokens of this type may only be used once (successfully)
CREATE TABLE `token_type`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
	`name` VARCHAR(32) NOT NULL,
	`duration_minutes` INT,
	`refreshed_type_id` INT,
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	`is_single_use_only` BOOLEAN NOT NULL DEFAULT FALSE,
	CONSTRAINT tt_tt_refreshed_type_ref_fk FOREIGN KEY tt_tt_refreshed_type_ref_idx (refreshed_type_id) REFERENCES `token_type`(`id`) ON DELETE SET NULL
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Tokens used for authenticating requests
-- type_id:            Id of the token type applicable to this token
-- hash:               A hashed version of this token
-- parent_token_id:    Id of the token that was used to acquire this token, if applicable & still known
-- owner_id:           Id of the user who owns this token, if applicable
-- model_style_id:     Model style preferred during this session
-- expires:            Time when this token expires, if applicable
-- created:            Time when this token was issued
-- deprecated_after:   Time when this token was revoked or replaced
-- is_single_use_only: Whether this token may only be used once (successfully)
CREATE TABLE `token`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
	`type_id` INT NOT NULL,
	`hash` VARCHAR(96) NOT NULL,
	`parent_token_id` INT,
	`owner_id` INT,
	`model_style_id` INT,
	`expires` DATETIME,
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	`deprecated_after` DATETIME,
	`is_single_use_only` BOOLEAN NOT NULL DEFAULT FALSE,
	INDEX t_created_idx (`created`),
	INDEX t_combo_1_idx (deprecated_after, expires, hash),
	CONSTRAINT t_tt_type_ref_fk FOREIGN KEY t_tt_type_ref_idx (type_id) REFERENCES `token_type`(`id`) ON DELETE CASCADE,
	CONSTRAINT t_t_parent_token_ref_fk FOREIGN KEY t_t_parent_token_ref_idx (parent_token_id) REFERENCES `token`(`id`) ON DELETE SET NULL,
	CONSTRAINT t_u_owner_ref_fk FOREIGN KEY t_u_owner_ref_idx (owner_id) REFERENCES `user`(`id`) ON DELETE SET NULL
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Represents an attempted email validation. Provides additional information to an authentication token.
-- token_id:          Id of the token sent via email
-- email_address:     Address to which the validation email was sent
-- resend_token_hash: Hashed token which may be used to send a copy of this email validation. None if resend is disabled.
-- send_count:        Number of times a validation email has been sent for this specific purpose up to this point.
CREATE TABLE `email_validation_attempt`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`token_id` INT NOT NULL, 
	`email_address` VARCHAR(64) NOT NULL, 
	`resend_token_hash` VARCHAR(96), 
	`send_count` INT NOT NULL DEFAULT 1, 
	INDEX eva_email_address_idx (`email_address`), 
	INDEX eva_resend_token_hash_idx (`resend_token_hash`), 
	CONSTRAINT eva_t_token_ref_fk FOREIGN KEY eva_t_token_ref_idx (token_id) REFERENCES `token`(`id`) ON DELETE CASCADE
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Used for linking scopes to tokens using many-to-many connections, describing what actions each token enables
-- token_id:               Id of the linked token
-- scope_id:               Id of the enabled scope
-- created:                Time when this token scope link was first created
-- is_directly_accessible: Whether the linked scope is directly accessible using the linked token
-- grants_forward:         Whether this scope is granted to tokens that are created using this token
CREATE TABLE `token_scope_link`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
	`token_id` INT NOT NULL,
	`scope_id` INT NOT NULL,
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	`is_directly_accessible` BOOLEAN NOT NULL DEFAULT FALSE,
	`grants_forward` BOOLEAN NOT NULL DEFAULT FALSE,
	INDEX tsl_combo_1_idx (token_id, is_directly_accessible),
	CONSTRAINT tsl_t_token_ref_fk FOREIGN KEY tsl_t_token_ref_idx (token_id) REFERENCES `token`(`id`) ON DELETE CASCADE, 
	CONSTRAINT tsl_s_scope_ref_fk FOREIGN KEY tsl_s_scope_ref_idx (scope_id) REFERENCES `scope`(`id`) ON DELETE CASCADE
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

--	User	----------

-- Represents a hashed user password
-- user_id: Id of the user who owns this password
-- hash:    User's hashed password, including salt
-- created: Time when this user password was first created
CREATE TABLE `user_password`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`user_id` INT NOT NULL, 
	`hash` VARCHAR(96) NOT NULL, 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	INDEX up_hash_idx (`hash`), 
	CONSTRAINT up_u_user_ref_fk FOREIGN KEY up_u_user_ref_idx (user_id) REFERENCES `user`(`id`) ON DELETE CASCADE
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

