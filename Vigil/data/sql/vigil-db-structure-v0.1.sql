-- 
-- Database structure for vigil models
-- Version: v0.1
-- Last generated: 2026-05-01
--

--	Scope	----------

-- Used for limiting authorization to certain features or areas
-- key:       A key used for identifying this scope
-- parent_id: ID of the scope that contains this scope. None if this is a root-level scope.
CREATE TABLE `scope`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`key` VARCHAR(12) NOT NULL, 
	`parent_id` INT, 
	INDEX vg_s_key_idx (`key`), 
	CONSTRAINT vg_s_s_parent_ref_fk FOREIGN KEY vg_s_s_parent_ref_idx (parent_id) REFERENCES `scope`(`id`) ON DELETE SET NULL
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;


--	Token	----------

-- A template or a mold for creating new tokens
-- name:                Name of this template. May be empty.
-- scope_grant_type_id: Way the scope-granting functions in this template
-- 		References enumeration ScopeGrantType
-- 		Possible values are: 
-- duration_millis:     Duration of the created tokens. None if infinite.
-- created:             Time when this token template was added to the database
CREATE TABLE `token_template`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`name` VARCHAR(32), 
	`scope_grant_type_id` TINYINT NOT NULL, 
	`duration_millis` BIGINT, 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	INDEX vg_tt_name_idx (`name`)
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Represents a token that may be used for authorizing certain actions
-- template_id: ID of the template used when creating this token
-- hash:        Hashed version of this token
-- parent_id:   ID of the token that was used to generate this token
-- name:        Name of this token. May be empty.
-- created:     Time when this token was created
-- expires:     Time when this token automatically expires. None if this token doesn't expire automatically.
-- revoked:     Time when this token was revoked.
CREATE TABLE `token`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`template_id` INT NOT NULL, 
	`hash` VARCHAR(65) NOT NULL, 
	`parent_id` INT, 
	`name` VARCHAR(64), 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	`expires` DATETIME, 
	`revoked` DATETIME, 
	INDEX vg_t_hash_idx (`hash`), 
	INDEX vg_t_created_idx (`created`), 
	INDEX vg_t_combo_1_idx (revoked, expires), 
	CONSTRAINT vg_t_tt_template_ref_fk FOREIGN KEY vg_t_tt_template_ref_idx (template_id) REFERENCES `token_template`(`id`) ON DELETE CASCADE, 
	CONSTRAINT vg_t_t_parent_ref_fk FOREIGN KEY vg_t_t_parent_ref_idx (parent_id) REFERENCES `token`(`id`) ON DELETE SET NULL
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Used for allowing certain token types (templates) to generate new tokens of other types
-- owner_template_id:   ID of the token template that has been given the right to generate new tokens
-- granted_template_id: ID of the template applied to the generated tokens
-- revokes:             Whether generating a new token revokes the token used for authorizing that action
CREATE TABLE `token_grant_right`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`owner_template_id` INT NOT NULL, 
	`granted_template_id` INT NOT NULL, 
	`revokes` BOOLEAN NOT NULL DEFAULT FALSE, 
	CONSTRAINT vg_tgr_tt_owner_template_ref_fk FOREIGN KEY vg_tgr_tt_owner_template_ref_idx (owner_template_id) REFERENCES `token_template`(`id`) ON DELETE CASCADE, 
	CONSTRAINT vg_tgr_tt_granted_template_ref_fk FOREIGN KEY vg_tgr_tt_granted_template_ref_idx (granted_template_id) REFERENCES `token_template`(`id`) ON DELETE CASCADE
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Links a (granted) scope to a token template
-- scope_id:    ID of the granted or accessible scope
-- template_id: ID of the template that grants this scope
-- created:     Time when this scope right was added to the database
-- usable:      Whether the linked scope is directly accessible. 
-- 		False if the scope is only applied when granting access for other authentication methods.
CREATE TABLE `token_template_scope`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`scope_id` INT NOT NULL, 
	`template_id` INT NOT NULL, 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	`usable` BOOLEAN NOT NULL DEFAULT FALSE, 
	INDEX vg_tts_usable_idx (`usable`), 
	CONSTRAINT vg_tts_s_scope_ref_fk FOREIGN KEY vg_tts_s_scope_ref_idx (scope_id) REFERENCES `scope`(`id`) ON DELETE CASCADE, 
	CONSTRAINT vg_tts_tt_template_ref_fk FOREIGN KEY vg_tts_tt_template_ref_idx (template_id) REFERENCES `token_template`(`id`) ON DELETE CASCADE
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Allows a token to be used in some scope
-- scope_id: ID of the granted or accessible scope
-- token_id: ID of the token that grants or has access to the linked scope
-- created:  Time when this scope right was added to the database
-- usable:   Whether the linked scope is directly accessible. 
-- 		False if the scope is only applied when granting access for other authentication methods.
CREATE TABLE `token_scope`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`scope_id` INT NOT NULL, 
	`token_id` INT NOT NULL, 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	`usable` BOOLEAN NOT NULL DEFAULT FALSE, 
	INDEX vg_ts_usable_idx (`usable`), 
	CONSTRAINT vg_ts_s_scope_ref_fk FOREIGN KEY vg_ts_s_scope_ref_idx (scope_id) REFERENCES `scope`(`id`) ON DELETE CASCADE, 
	CONSTRAINT vg_ts_t_token_ref_fk FOREIGN KEY vg_ts_t_token_ref_idx (token_id) REFERENCES `token`(`id`) ON DELETE CASCADE
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

