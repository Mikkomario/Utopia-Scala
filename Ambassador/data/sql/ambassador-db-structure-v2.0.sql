-- 
-- Database structure for Ambassador models
-- Version: v2.0
-- Last generated: 2022-02-22
--

--	Service	----------

-- Represents a service that provides an OAuth interface (e.g. Google)
-- name:    Name of this service (from the customer's perspective)
-- created: Time when this auth service was first created
CREATE TABLE `oauth_service`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`name` VARCHAR(64) NOT NULL, 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	INDEX oase_name_idx (`name`)
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Specifies service-specific settings. It is recommended to have only one instance per service.
-- service_id:                             Id of the described service
-- client_id:                              Id of this client in the referenced service
-- client_secret:                          This application's password to the referenced service
-- authentication_url:                     Url to the endpoint that receives users for the OAuth process
-- token_url:                              Url to the endpoint that provides refresh and session tokens
-- redirect_url:                           Url to the endpoint in this application which receives the user after they've completed the OAuth process
-- incomplete_auth_redirect_url:           Url on the client side (front) that receives the user when they arrive from an OAuth process that was not initiated in this application. None if this use case is not supported.
-- default_completion_redirect_url:        Url on the client side (front) where the user will be redirected upon authentication completion. Used if no redirect urls were prepared by the client.
-- preparation_token_duration_minutes:     Duration how long preparation tokens can be used after they're issued before they expire
-- redirect_token_duration_minutes:        Duration how long redirect tokens can be used after they're issued before they expire
-- incomplete_auth_token_duration_minutes: Duration how long incomplete authentication tokens can be used after they're issued before they expire
-- default_session_duration_minutes:       Duration of this auth service settings
-- created:                                Time when this auth service settings was first created
CREATE TABLE `oauth_service_settings`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`service_id` INT NOT NULL, 
	`client_id` VARCHAR(128) NOT NULL, 
	`client_secret` VARCHAR(128) NOT NULL, 
	`authentication_url` VARCHAR(128) NOT NULL, 
	`token_url` VARCHAR(128) NOT NULL, 
	`redirect_url` VARCHAR(128) NOT NULL, 
	`incomplete_auth_redirect_url` VARCHAR(255), 
	`default_completion_redirect_url` VARCHAR(255), 
	`preparation_token_duration_minutes` INT NOT NULL DEFAULT 5, 
	`redirect_token_duration_minutes` INT NOT NULL DEFAULT 15, 
	`incomplete_auth_token_duration_minutes` INT NOT NULL DEFAULT 30, 
	`default_session_duration_minutes` INT NOT NULL DEFAULT 1320, 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	INDEX oss_created_idx (`created`), 
	CONSTRAINT oss_oase_service_ref_fk FOREIGN KEY oss_oase_service_ref_idx (service_id) REFERENCES `oauth_service`(`id`) ON DELETE CASCADE
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;


--	Process	----------

-- Represents a case where a user arrives from a 3rd party service without first preparing an authentication on this side
-- service_id: Id of the service from which the user arrived
-- code:       Authentication code provided by the 3rd party service
-- token:      Token used for authentication the completion of this authentication
-- expires:    Time after which the generated authentication token is no longer valid
-- created:    Time when this incomplete auth was first created
CREATE TABLE `incomplete_oauth`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`service_id` INT NOT NULL, 
	`code` VARCHAR(255) NOT NULL, 
	`token` VARCHAR(64) NOT NULL, 
	`expires` DATETIME NOT NULL, 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	INDEX io_token_idx (`token`), 
	INDEX io_expires_idx (`expires`), 
	INDEX io_created_idx (`created`), 
	CONSTRAINT io_oase_service_ref_fk FOREIGN KEY io_oase_service_ref_idx (service_id) REFERENCES `oauth_service`(`id`) ON DELETE CASCADE
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Used for preparing and authenticating an OAuth process that follows
-- user_id:      Id of the user who initiated this process
-- token:        Token used for authenticating the OAuth redirect
-- expires:      Time when this authentication (token) expires
-- client_state: Custom state given by the client and sent back upon user redirect
-- created:      Time when this auth preparation was first created
CREATE TABLE `oauth_preparation`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`user_id` INT NOT NULL, 
	`token` VARCHAR(64) NOT NULL, 
	`expires` DATETIME NOT NULL, 
	`client_state` VARCHAR(2048), 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	INDEX op_created_idx (`created`), 
	INDEX op_combo_1_idx (expires, token), 
	CONSTRAINT op_u_user_ref_fk FOREIGN KEY op_u_user_ref_idx (user_id) REFERENCES `user`(`id`) ON DELETE CASCADE
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Records cases where incomplete authentications are completed with the user logging in
-- auth_id:     Id of the incomplete authentication this login completes
-- user_id:     Id of the user who logged in
-- created:     Time when this incomplete auth login was first created
-- was_success: Whether authentication tokens were successfully acquired from the 3rd party service
CREATE TABLE `incomplete_oauth_login`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`auth_id` INT NOT NULL, 
	`user_id` INT NOT NULL, 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	`was_success` BOOLEAN NOT NULL DEFAULT FALSE, 
	INDEX iol_created_idx (`created`), 
	CONSTRAINT iol_io_auth_ref_fk FOREIGN KEY iol_io_auth_ref_idx (auth_id) REFERENCES `incomplete_oauth`(`id`) ON DELETE CASCADE, 
	CONSTRAINT iol_u_user_ref_fk FOREIGN KEY iol_u_user_ref_idx (user_id) REFERENCES `user`(`id`) ON DELETE CASCADE
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Used for storing client-given rules for redirecting the user after the OAuth process completion. Given during the OAuth preparation.
-- preparation_id:        Id of the preparation during which these targets were specified
-- url:                   Url where the user will be redirected
-- result_state_filter:   True when only successes are accepted. False when only failures are accepted. None when both are accepted.
-- is_limited_to_denials: Whether this target is only used for denial of access -cases
CREATE TABLE `oauth_completion_redirect_target`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`preparation_id` INT NOT NULL, 
	`url` VARCHAR(255) NOT NULL, 
	`result_state_filter` BOOLEAN, 
	`is_limited_to_denials` BOOLEAN NOT NULL DEFAULT FALSE, 
	INDEX ocrt_combo_1_idx (result_state_filter, is_limited_to_denials), 
	CONSTRAINT ocrt_op_preparation_ref_fk FOREIGN KEY ocrt_op_preparation_ref_idx (preparation_id) REFERENCES `oauth_preparation`(`id`) ON DELETE CASCADE
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Links a requested scope to an OAuth preparation
-- preparation_id: Id of the described OAuth preparation
-- scope_id:       Id of the requested scope
CREATE TABLE `oauth_preparation_scope`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`preparation_id` INT NOT NULL, 
	`scope_id` INT NOT NULL, 
	CONSTRAINT ops_op_preparation_ref_fk FOREIGN KEY ops_op_preparation_ref_idx (preparation_id) REFERENCES `oauth_preparation`(`id`) ON DELETE CASCADE, 
	CONSTRAINT ops_oasc_scope_ref_fk FOREIGN KEY ops_oasc_scope_ref_idx (scope_id) REFERENCES `oauth_scope`(`id`) ON DELETE CASCADE
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Records each event when a user is directed to the 3rd party OAuth service. These close the linked preparations.
-- preparation_id: Id of the preparation event for this redirection
-- expires:        Time when the linked redirect token expires
-- created:        Time when this auth redirect was first created
CREATE TABLE `oauth_redirect`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`preparation_id` INT NOT NULL, 
	`token` VARCHAR(64) NOT NULL, 
	`expires` DATETIME NOT NULL, 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	INDEX or_token_idx (`token`), 
	INDEX or_expires_idx (`expires`), 
	INDEX or_created_idx (`created`), 
	CONSTRAINT or_op_preparation_ref_fk FOREIGN KEY or_op_preparation_ref_idx (preparation_id) REFERENCES `oauth_preparation`(`id`) ON DELETE CASCADE
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Records the cases when the user arrives back from the 3rd party OAuth service, whether the authentication succeeded or not.
-- redirect_id:       Id of the redirection event this result completes
-- did_receive_code:  Whether an authentication code was included in the request (implies success)
-- did_receive_token: Whether authentication tokens were successfully acquired
-- created:           Time when this auth redirect result was first created
CREATE TABLE `oauth_redirect_result`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`redirect_id` INT NOT NULL, 
	`did_receive_code` BOOLEAN NOT NULL DEFAULT FALSE, 
	`did_receive_token` BOOLEAN NOT NULL DEFAULT FALSE, 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	INDEX orr_created_idx (`created`), 
	CONSTRAINT orr_or_redirect_ref_fk FOREIGN KEY orr_or_redirect_ref_idx (redirect_id) REFERENCES `oauth_redirect`(`id`) ON DELETE CASCADE
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;


--	Token	----------

-- Tokens (both access and refresh) used for authenticating 3rd party requests
-- user_id:          Id of the user who owns this token / to whom this token is linked
-- token:            Textual representation of this token
-- expires:          Time when this token can no longer be used, if applicable
-- created:          Time when this token was acquired / issued
-- deprecated_after: Time when this token was cancelled, revoked or replaced
-- is_refresh_token: Whether this is a refresh token which can be used for acquiring access tokens
CREATE TABLE `oauth_token`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`user_id` INT NOT NULL, 
	`token` VARCHAR(2048) NOT NULL, 
	`expires` DATETIME, 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	`deprecated_after` DATETIME, 
	`is_refresh_token` BOOLEAN NOT NULL DEFAULT FALSE, 
	INDEX ot_created_idx (`created`), 
	INDEX ot_combo_1_idx (deprecated_after, expires, is_refresh_token), 
	CONSTRAINT ot_u_user_ref_fk FOREIGN KEY ot_u_user_ref_idx (user_id) REFERENCES `user`(`id`) ON DELETE CASCADE
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Used for listing, which scopes are available based on which authentication token
-- token_id: Id of the token that provides access to the linked scope
-- scope_id: Id of the scope that is accessible by using the linked token
CREATE TABLE `oauth_token_scope`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`token_id` INT NOT NULL, 
	`scope_id` INT NOT NULL, 
	CONSTRAINT ots_ot_token_ref_fk FOREIGN KEY ots_ot_token_ref_idx (token_id) REFERENCES `oauth_token`(`id`) ON DELETE CASCADE, 
	CONSTRAINT ots_oasc_scope_ref_fk FOREIGN KEY ots_oasc_scope_ref_idx (scope_id) REFERENCES `oauth_scope`(`id`) ON DELETE CASCADE
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;


--	Scope	----------

-- Scopes are like access rights which can be requested from 3rd party services. They determine what the application is allowed to do in behalf of the user.
-- service_id: Id of the service this scope is part of / which uses this scope
-- name:       Name of this scope in the 3rd party service
-- priority:   Priority assigned for this scope where higher values mean higher priority. Used when multiple scopes can be chosen from.
-- created:    Time when this scope was first created
CREATE TABLE `oauth_scope`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`service_id` INT NOT NULL, 
	`name` VARCHAR(255) NOT NULL, 
	`priority` INT, 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	INDEX oasc_name_idx (`name`), 
	CONSTRAINT oasc_oase_service_ref_fk FOREIGN KEY oasc_oase_service_ref_idx (service_id) REFERENCES `oauth_service`(`id`) ON DELETE CASCADE
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Links tasks with the scopes that are required to perform them
-- task_id:     Id of the linked task
-- scope_id:    Id of the scope required to perform the task
-- is_required: True whether this scope is always required to perform the linked task. False whether this scope can be replaced with another optional scope.
-- created:     Time when this task scope link was first created
CREATE TABLE `task_scope`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`task_id` INT NOT NULL, 
	`scope_id` INT NOT NULL, 
	`is_required` BOOLEAN NOT NULL DEFAULT FALSE, 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	CONSTRAINT ts_t_task_ref_fk FOREIGN KEY ts_t_task_ref_idx (task_id) REFERENCES `task`(`id`) ON DELETE CASCADE, 
	CONSTRAINT ts_oasc_scope_ref_fk FOREIGN KEY ts_oasc_scope_ref_idx (scope_id) REFERENCES `oauth_scope`(`id`) ON DELETE CASCADE
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;


--	Description	----------

-- Links Scopes with their descriptions
-- scope_id:       Id of the described scope
-- description_id: Id of the linked description
CREATE TABLE `scope_description`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`scope_id` INT NOT NULL, 
	`description_id` INT NOT NULL, 
	CONSTRAINT sd_oasc_scope_ref_fk FOREIGN KEY sd_oasc_scope_ref_idx (scope_id) REFERENCES `oauth_scope`(`description_id`) ON DELETE CASCADE, 
	CONSTRAINT sd_d_description_ref_fk FOREIGN KEY sd_d_description_ref_idx (description_id) REFERENCES `description`(`id`) ON DELETE CASCADE
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

