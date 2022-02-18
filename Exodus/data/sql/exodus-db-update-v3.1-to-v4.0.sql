-- 
-- Database structure for Exodus models
-- Version: v4.0
-- Type: Update
-- Origin: v3.1
-- Last generated: 2022-02-18
--

-- Drops the tables which are no longer used, including email_validation_attempt, which changed drastically

DELETE FROM `email_validated_session`;
DELETE FROM `email_validation_resend`;
DELETE FROM `email_validation_attempt`;
DELETE FROM `session_token`;
DELETE FROM `device_token`;
DELETE FROM `email_validation_purpose`;
DELETE FROM `api_key`;

DROP TABLE `email_validated_session`;
DROP TABLE `email_validation_resend`;
DROP TABLE `email_validation_attempt`;
DROP TABLE `session_token`;
DROP TABLE `device_token`;
DROP TABLE `email_validation_purpose`;
DROP TABLE `api_key`;

-- Introduces the new tables

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
-- name:             Name of this token type for identification. Not localized.
-- parent_type_id:   Id of the type of token used to acquire this token, if applicable
-- duration_minutes: Duration that determines how long these tokens remain valid after issuing. None if these tokens don't expire automatically.
-- created:          Time when this token type was first created
CREATE TABLE `token_type`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`name` VARCHAR(32) NOT NULL, 
	`parent_type_id` INT, 
	`duration_minutes` INT, 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	CONSTRAINT tt_tt_parent_type_ref_fk FOREIGN KEY tt_tt_parent_type_ref_idx (parent_type_id) REFERENCES `token_type`(`id`) ON DELETE SET NULL
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Tokens used for authenticating requests
-- type_id:          Id of the token type applicable to this token
-- hash:             A hashed version of this token
-- owner_id:         Id of the user who owns this token, if applicable
-- device_id:        Id of the device this token is tied to, if applicable
-- model_style_id:   Model style preferred during this session
-- expires:          Time when this token expires, if applicable
-- created:          Time when this token was issued
-- deprecated_after: Time when this token was revoked or replaced
CREATE TABLE `token`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`type_id` INT NOT NULL, 
	`hash` VARCHAR(96) NOT NULL, 
	`owner_id` INT, 
	`device_id` INT, 
	`model_style_id` INT, 
	`expires` DATETIME, 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	`deprecated_after` DATETIME, 
	INDEX t_expires_idx (`expires`), 
	INDEX t_created_idx (`created`), 
	INDEX t_combo_1_idx (deprecated_after, expires), 
	CONSTRAINT t_tt_type_ref_fk FOREIGN KEY t_tt_type_ref_idx (type_id) REFERENCES `token_type`(`id`) ON DELETE CASCADE, 
	CONSTRAINT t_u_owner_ref_fk FOREIGN KEY t_u_owner_ref_idx (owner_id) REFERENCES `user`(`id`) ON DELETE SET NULL, 
	CONSTRAINT t_cd_device_ref_fk FOREIGN KEY t_cd_device_ref_idx (device_id) REFERENCES `client_device`(`id`) ON DELETE SET NULL
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
-- token_id: Id of the linked token
-- scope_id: Id of the enabled scope
-- created:  Time when this token scope link was first created
CREATE TABLE `token_scope_link`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`token_id` INT NOT NULL, 
	`scope_id` INT NOT NULL, 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	CONSTRAINT tsl_t_token_ref_fk FOREIGN KEY tsl_t_token_ref_idx (token_id) REFERENCES `token`(`id`) ON DELETE CASCADE, 
	CONSTRAINT tsl_s_scope_ref_fk FOREIGN KEY tsl_s_scope_ref_idx (scope_id) REFERENCES `scope`(`id`) ON DELETE CASCADE
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Inserts initial data

-- Inserts 9 scopes
INSERT INTO `scope` (id, name) VALUES
	(1, 'General Data Read'),
	(2, 'User Creation'),
	(3, 'Personal Data Read'),
	(4, 'Personal Actions'),
	(5, 'Organization Data Read'),
	(6, 'Organization Actions'),
	(7, 'Password Reset'),
	(8, 'Email Change'),
	(9, 'Account Deletion');

-- Inserts 4 token types
INSERT INTO `token_type` (id, name) VALUES
	(1, 'Api Key'),
	(2, 'Refresh Token'),
	(3, 'Session Token'),
	(4, 'Email Validation Token');
