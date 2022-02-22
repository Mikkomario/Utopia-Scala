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
DELETE FROM `api_key`;

DROP TABLE `email_validated_session`;
DROP TABLE `email_validation_resend`;
DROP TABLE `email_validation_attempt`;
DROP TABLE `session_token`;
DROP TABLE `device_token`;
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
	`model_style_id` TINYINT,
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


-- Alters some tables

-- An enumeration for purposes an email validation may be used for
-- name:    Name of this email validation purpose. For identification (not localized).
-- created: Time when this email validation purpose was first created
ALTER TABLE `email_validation_purpose`
    CHANGE `name_en` `name` VARCHAR(32) NOT NULL;

-- Represents an attempted email validation. Provides additional information to an authentication token.
-- token_id:      Id of the token sent via email
-- email_address: Address to which the validation email was sent
-- purpose_id:    Id of the purpose this email validation is for
ALTER TABLE `email_validation_attempt`
    DROP FOREIGN KEY eva_evp_purpose_ref_fk,
    DROP FOREIGN KEY eva_u_user_ref_fk,
    DROP INDEX eva_evp_purpose_ref_idx,
    DROP INDEX eva_u_user_ref_idx,
    DROP INDEX eva_email_idx,
    DROP INDEX eva_token_idx,
    DROP INDEX eva_resend_token_idx,
    DROP INDEX eva_combo_1_idx,
    DROP `token`,
    DROP `resend_token`,
    DROP `expires`,
    DROP `user_id`,
    DROP `created`,
    DROP `completed`,
    CHANGE `email` `email_address` VARCHAR(64) NOT NULL,
    ADD `token_id` INT NOT NULL AFTER `id`,
    ADD INDEX eva_email_address_idx (`email_address`),
    ADD CONSTRAINT eva_t_token_ref_fk FOREIGN KEY eva_t_token_ref_idx (token_id) REFERENCES `token`(`id`) ON DELETE CASCADE;


-- Inserts initial data

--	Auth	----------

-- Inserts 4 email validation purposes
INSERT INTO `email_validation_purpose` (`id`, `name`) VALUES
	(1, 'user-creation'),
	(2, 'email-change'),
	(3, 'password-reset'),
	(4, 'organization-invitation');

-- Inserts 15 scopes
INSERT INTO `scope` (`id`, `name`) VALUES
	(1, 'read-general-data'),
	(2, 'create-user'),
	(3, 'read-personal-data'),
	(4, 'personal-actions'),
	(5, 'join-organization'),
	(6, 'create-organization'),
	(7, 'read-organization-data'),
	(8, 'organization-actions'),
	(9, 'request-password-reset'),
	(10, 'change-known-password'),
	(11, 'replace-forgotten-password'),
	(12, 'change-email'),
	(13, 'terminate-other-sessions'),
	(14, 'revoke-other-tokens'),
	(15, 'delete-account');

-- Inserts 5 token types
INSERT INTO `token_type` (`id`, `name`) VALUES
	(1, 'api-key');
INSERT INTO `token_type` (`id`, `duration`, `name`) VALUES
	(2, 1320, 'session-token'),
	(4, 15, 'email-validated-session');
INSERT INTO `token_type` (`id`, `name`, `refreshed_type_id`) VALUES
	(3, 'refresh-token', 2);
INSERT INTO `token_type` (`id`, `duration`, `is_single_use_only`, `name`, `refreshed_type_id`) VALUES
	(5, 4320, TRUE, 'email-validation-token', 4);