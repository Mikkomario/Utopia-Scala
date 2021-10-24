-- Used for authenticating requests before session-based authentication is available
-- token: The textual representation of this api key
-- name: Name given to identify this api key
-- created: Time when this ApiKey was first created
CREATE TABLE `api_key`(
	id INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`token` VARCHAR(64) NOT NULL, 
	`name` VARCHAR(64) NOT NULL, 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	INDEX ak_token_idx (`token`)
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- An enumeration for purposes an email validation may be used for
-- created: Time when this EmailValidationPurpose was first created
CREATE TABLE `email_validation_purpose`(
	id INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`name_en` VARCHAR(32) NOT NULL, 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Represents an attempted email validation, and the possible response / success
-- purpose_id: Id of the purpose this email validation is used for
-- email: Email address being validated
-- token: Token sent with the email, which is also used for validating the email address
-- resend_token: Token used for authenticating an email resend attempt
-- user_id: Id of the user who claims to own this email address
-- expires: Time when this EmailValidationAttempt expires / becomes invalid
-- created: Time when this EmailValidationAttempt was first created
-- completed: Time when this attempt was finished successfully. None while not completed.
CREATE TABLE `email_validation_attempt`(
	id INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`purpose_id` INT NOT NULL, 
	`email` VARCHAR(128) NOT NULL, 
	`token` VARCHAR(64) NOT NULL, 
	`resend_token` VARCHAR(64) NOT NULL, 
	`user_id` INT NOT NULL, 
	`expires` DATETIME NOT NULL, 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	`completed` DATETIME, 
	INDEX eva_email_idx (`email`), 
	INDEX eva_token_idx (`token`), 
	INDEX eva_resend_token_idx (`resend_token`), 
	INDEX eva_combo_1_idx (expires, completed), 
	CONSTRAINT eva_evp_purpose_ref_fk FOREIGN KEY eva_evp_purpose_ref_idx (purpose_id) REFERENCES `email_validation_purpose`(id) ON DELETE CASCADE, 
	CONSTRAINT eva_u_user_ref_fk FOREIGN KEY eva_u_user_ref_idx (user_id) REFERENCES `user`(id) ON DELETE CASCADE
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Represents a time when an email validation was sent again
-- validation_id: Id of the email_validation_attempt linked with this EmailValidationResend
-- created: Time when this EmailValidationResend was first created
CREATE TABLE `email_validation_resend`(
	id INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`validation_id` INT NOT NULL, 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	CONSTRAINT evr_eva_validation_ref_fk FOREIGN KEY evr_eva_validation_ref_idx (validation_id) REFERENCES `email_validation_attempt`(id) ON DELETE CASCADE
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Used as a refresh token to generate device-specific session tokens on private devices
-- device_id: Id of the device this token provides access to
-- user_id: Id of the user who owns this token and presumably the linked device, also
-- token: Textual representation of this token
-- created: Time when this device use was started / authenticated
-- deprecated_after: Time when this token was invalidated, if applicable
CREATE TABLE `device_token`(
	id INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`device_id` INT NOT NULL, 
	`user_id` INT NOT NULL, 
	`token` VARCHAR(64) NOT NULL, 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	`deprecated_after` DATETIME, 
	INDEX dt_token_idx (`token`), 
	INDEX dt_created_idx (`created`), 
	INDEX dt_deprecated_after_idx (`deprecated_after`), 
	CONSTRAINT dt_cd_device_ref_fk FOREIGN KEY dt_cd_device_ref_idx (device_id) REFERENCES `client_device`(id) ON DELETE CASCADE, 
	CONSTRAINT dt_u_user_ref_fk FOREIGN KEY dt_u_user_ref_idx (user_id) REFERENCES `user`(id) ON DELETE CASCADE
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Used for authenticating temporary user sessions
-- user_id: Id of the user who owns this token
-- token: Textual representation of this token
-- expires: Time when this token expires
-- device_id: Id of the device on which this session is, if applicable
-- model_style_id: Model style preferred during this session
-- created: Time when this session was started
-- logged_out: Time when this session was ended due to the user logging out. None if not logged out.
CREATE TABLE `session_token`(
	id INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`user_id` INT NOT NULL, 
	`token` VARCHAR(64) NOT NULL, 
	`expires` DATETIME NOT NULL, 
	`device_id` INT, 
	`model_style_id` INT, 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	`logged_out` DATETIME, 
	INDEX st_token_idx (`token`), 
	INDEX st_expires_idx (`expires`), 
	INDEX st_created_idx (`created`), 
	INDEX st_logged_out_idx (`logged_out`), 
	CONSTRAINT st_u_user_ref_fk FOREIGN KEY st_u_user_ref_idx (user_id) REFERENCES `user`(id) ON DELETE CASCADE, 
	CONSTRAINT st_cd_device_ref_fk FOREIGN KEY st_cd_device_ref_idx (device_id) REFERENCES `client_device`(id) ON DELETE SET NULL
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Represents a hashed user password
-- user_id: Id of the user who owns this password
-- hash: User's hashed password
-- created: Time when this UserPassword was first created
CREATE TABLE `user_password`(
	id INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`user_id` INT NOT NULL, 
	`hash` VARCHAR(128) NOT NULL, 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	INDEX up_hash_idx (`hash`), 
	CONSTRAINT up_u_user_ref_fk FOREIGN KEY up_u_user_ref_idx (user_id) REFERENCES `user`(id) ON DELETE CASCADE
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

