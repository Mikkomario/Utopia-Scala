--
-- DB Structure for Utopia Ambassador features
-- Intended to be inserted after Utopia Exodus database structure
-- Supports versions v1.0 and above
--

-- Lists services that use OAuth, like Google and Zoom
CREATE TABLE oauth_service(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Lists settings used when accessing OAuth services.
-- It is recommended to specify one row per service provider.

-- Authentication Url is where the user is redirected to
--      accept / perform the authentication (OAuth service side)
-- Token url is where session and refresh tokens are acquired from / refreshed
-- Redirect url is an url pointing to your server to which the OAuth service
--      will redirect user after authentication (/api/version/serviceNode/result)
-- Client ID and Client Secret are specific to your application and given by the OAuth service (keep them secret)
-- Token Expirations determine how long generated authentication tokens may be used after creation
--      longer durations allow for user, client and service delay but incur security risk
-- Default Session Expiration is used to expire a service session token
--      when no expiration time is given by the service
-- Incomplete Auth Redirect Url (optional) is the client side url to which this server will redirect the
--      user if they initiate the authentication process on OAuth service side and therefore haven't
--      authenticated to this service yet. NULL if this use case doesn't apply to this service.
-- Default Completion Redirect Url (optional). Url where the user will be redirected upon the completion of
--      the authentication (success or failure). Used if no redirect urls were prepared by the client.
--      If NULL, the urls must be specified by the client.
CREATE TABLE oauth_service_settings(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    service_id INT NOT NULL,
    authentication_url VARCHAR(128) NOT NULL,
    token_url VARCHAR(128) NOT NULL,
    redirect_url VARCHAR(128) NOT NULL,
    client_id VARCHAR(128) NOT NULL,
    client_secret VARCHAR(128) NOT NULL,
    preparation_token_expiration_minutes INT NOT NULL DEFAULT 5,
    redirect_token_expiration_minutes INT NOT NULL DEFAULT 15,
    incomplete_auth_token_expiration_minutes INT NOT NULL DEFAULT 30,
    default_session_expiration_minutes INT NOT NULL DEFAULT 1320,
    incomplete_auth_redirect_url VARCHAR(255),
    default_completion_redirect_url VARCHAR(255),
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX oss_creation_idx (created),

    CONSTRAINT oss_os_described_service_ref_fk FOREIGN KEY oss_os_described_service_ref_idx (service_id)
        REFERENCES oauth_service(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Scopes are used by third party services to control the level of this service's access
-- to user's data and features. For example, some scopes allow this service to read certain kind of data
-- while other provide write access.
-- Authorization is always limited to some scope. If this service requires a feature outside of
-- the acquired scopes, a new authentication must be performed
CREATE TABLE scope(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    service_id INT NOT NULL,
    service_side_name VARCHAR(255) NOT NULL,
    client_side_name VARCHAR(64),
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX s_service_idx (service_side_name),

    CONSTRAINT s_target_service_fk FOREIGN KEY s_target_service_idx (service_id)
        REFERENCES oauth_service(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Links descriptions with scopes
CREATE TABLE scope_description
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    scope_id INT NOT NULL,
    description_id INT NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deprecated_after DATETIME,

    INDEX sd_deprecation_idx (deprecated_after),

    CONSTRAINT sd_s_described_scope_fk FOREIGN KEY sd_s_described_scope_idx (scope_id)
        REFERENCES scope(id) ON DELETE CASCADE,

    CONSTRAINT sd_d_description_for_scope_fk FOREIGN KEY sd_d_description_for_scope_idx (description_id)
        REFERENCES description(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Links tasks with the scopes that are required to fulfill the task
-- Is Required -parameter is TRUE when this scope is always required
--      and FALSE when this scope is one of the alternative required scopes.
--      If there are alternative scopes listed, one of them is always required to be available.
CREATE TABLE task_scope(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    task_id INT NOT NULL,
    scope_id INT NOT NULL,
    is_required BOOLEAN NOT NULL DEFAULT TRUE,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deprecated_after DATETIME,

    INDEX ts_deprecation_idx (deprecated_after),

    CONSTRAINT ts_t_described_task_fk FOREIGN KEY ts_t_described_task_idx (task_id)
        REFERENCES task(id) ON DELETE CASCADE,
    CONSTRAINT ts_s_required_scope_fk FOREIGN KEY ts_s_required_scope_idx (scope_id)
        REFERENCES scope(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- One of these will be created each time an OAuth process is being initiated
-- (in order to acquire access to new scopes)
-- Each preparation generates a temporary token for authenticating a single request
-- Client State (optional) is provided by the client service
--      and will be sent along with the completion redirect
CREATE TABLE oauth_preparation(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    token VARCHAR(48) NOT NULL,
    client_state VARCHAR(255),
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX op_validation_idx (created, token),

    CONSTRAINT op_u_authorized_user_fk FOREIGN KEY op_u_authorized_user_idx (user_id)
        REFERENCES `user`(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;
-- Lists scopes that are planned to be requested in the coming OAuth authentication attempt
CREATE TABLE scope_request_preparation(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    preparation_id INT NOT NULL,
    scope_id INT NO NULL,

    CONSTRAINT srp_open_preparation_fk FOREIGN KEY srp_open_preparation_idx (preparation_id)
        REFERENCES oauth_preparation(id) ON DELETE CASCADE,
    CONSTRAINT srp_s_requested_scope_fk FOREIGN KEY srp_s_requested_scope_idx (scope_id)
        REFERENCES scope(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;
-- Lists client-defined redirection targets for the prepared OAuth process
-- Redirection targets are client-side urls that are prepared for receiving the user after
-- the OAuth process completes. There may be different targets for different result states.
-- The most specific target available is used. If filters are left as NULLs,
--      those are treated as default values. NULL, NULL is the default for any result.
-- Result state is either success or failure, based on whether full authorization was acquired
-- Denied filter determines whether the failure was due to user denying access.
--      This filter is only used for the failure state.
-- Query parameters may be added to the specified urls
CREATE TABLE oauth_completion_redirect_target(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    preparation_id INT NOT NULL,
    url VARCHAR(255) NOT NULL,
    result_state_filter BOOLEAN,
    denied_filter BOOLEAN,

    ocrt_filter_idx (result_state_filter, interactive_filter),

    CONSTRAINT ocrt_op_linked_preparation_fk FOREIGN KEY ocrt_op_linked_preparation_idx (preparation_id)
        REFERENCES oauth_preparation(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Records every successful event where a user is redirected to an OAuth service
-- These rows close / invalidate preparation tokens
-- Each event generates a temporary authorization token for the expected OAuth service result
--      (when they redirect the user back to this service)
CREATE TABLE oauth_user_redirect(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    preparation_id INT NOT NULL,
    token VARCHAR(48) NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX our_validation_idx (created, token),

    CONSTRAINT our_op_used_preparation_fk FOREIGN KEY our_op_used_preparation_idx (preparation_id)
        REFERENCES oauth_preparation(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Records results of OAuth user redirects, whether they succeeded or not
-- These rows close / invalidate user redirect tokens
-- Did Receive Code reports whether the request contained authentication code, indicating potential success
-- Did Receive Token reports whether this service was able to acquire a refresh and/or session token
--      from the 3rd party service using the specified authentication code (at leas partial success if true)
-- Did Receive Full Scope reports whether the user provided all scopes that were requested by this service
--      If false, the user may not be able to perform all the actions they were about to initialize
CREATE TABLE oauth_user_redirect_result(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    redirect_id INT NOT NULL,
    did_receive_code BOOLEAN NOT NULL DEFAULT FALSE,
    did_receive_token BOOLEAN NOT NULL DEFAULT FALSE,
    did_receive_full_scope BOOLEAN NOT NULL DEFAULT FALSE,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ourr_our_finished_redirect_fk FOREIGN KEY ourr_our_finished_redirect_idx (redirect_id)
        REFERENCES oauth_user_redirect(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Lists all session and refresh tokens acquired through the OAuth process
-- Is Refresh Token is set to TRUE for refresh tokens and FALSE for access/session tokens
-- Expiration is set for token types that expire after some time (mainly session tokens)
-- Deprecated After is set when a token is revoked or replaced with a new one
CREATE TABLE oauth_token(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    token VARCHAR(2048) NOT NULL,
    is_refresh_token BOOLEAN NOT NULL DEFAULT FALSE,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expiration DATETIME,
    deprecated_after DATETIME,

    INDEX ot_valid_token_idx (deprecated_after, expiration, is_refresh_token),
    INDEX ot_creation_idx (created),

    CONSTRAINT ot_u_token_owner_fk FOREIGN KEY ot_u_token_owner_idx (user_id)
        REFERENCES `user`(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;
-- Lists all scopes each session token provides access to (many-to-many link)
CREATE TABLE oauth_token_scope(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    token_id INT NOT NULL,
    scope_id INT NOT NULL,

    CONSTRAINT ots_ot_described_token_fk FOREIGN KEY ots_ot_described_token_idx (token_id)
        REFERENCES oauth_token(id) ON DELETE CASCADE,
    CONSTRAINT ots_s_token_scope_fk FOREIGN KEY ots_s_token_scope_idx (scoped_id)
        REFERENCES scope(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Records cases where a user arrives to this server via a redirect url,
-- presumably from the third party service, with a potentially valid authentication code
-- but without being directed to the 3rd party service by this service
-- The user may redeem these by authenticating with this service (or by creating a new account)
CREATE TABLE incomplete_authentication(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    service_id INT NOT NULL,
    oauth_code VARCHAR(255) NOT NULL,
    token VARCHAR(48) NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX ia_validation_idx (created, token),

    CONSTRAINT ia_os_origin_service_fk FOREIGN KEY ia_os_origin_service_idx (service_id)
        REFERENCES oauth_service(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;
-- Records cases where the user authorizes the 3rd party -initiated OAuth process by logging in
-- Was Success records whether the OAuth process completed successfully and whether tokens were acquired
CREATE TABLE incomplete_authentication_login(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    authentication_id INT NOT NULL,
    user_id INT NOT NULL,
    was_success BOOLEAN NOT NULL DEFAULT FALSE,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ial_ia_closed_auth_fk FOREIGN KEY ial_ia_closed_auth_idx (authentication_id)
        REFERENCES incomplete_authentication(id) ON DELETE CASCADE,
    CONSTRAINT ia_u_authenticated_user_fk FOREIGN KEY ia_u_authenticated_user_idx (user_id)
        REFERENCES `user`(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;
-- Lists email validations that were sent based on a new user account creation as part of a
-- 3rd party -initiated OAuth process (only used if email validation is enabled)
CREATE TABLE incomplete_authentication_email_validation(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    authentication_id INT NOT NULL,
    validation_id INT NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT iaev_ia_ongoing_auth_fk FOREIGN KEY iaev_ia_ongoing_auth_idx (authentication_id)
        REFERENCES incomplete_authentication(id) ON DELETE CASCADE,
    CONSTRAINT iaev_ev_validation_attempt_fk FOREIGN KEY iaev_ev_validation_attempt_idx (validation_id)
        REFERENCES email_validation(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;
-- Lists user accounts that were generated as a result of a 3rd party -initiated OAuth process
-- Was Success indicates whether the 3rd party authorization process was successful
CREATE TABLE incomplete_authentication_registration(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    authentication_id INT NOT NULL,
    user_id INT NOT NULL,
    was_success BOOLEAN NOT NULL DEFAULT FALSE,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT iar_ia_closed_auth_fk FOREIGN KEY iar_ia_closed_auth_idx (authentication_id)
        REFERENCES incomplete_authentication(id) ON DELETE CASCADE,
    CONSTRAINT iar_u_registered_user_fk FOREIGN KEY iar_u_registered_user_idx (user_id)
        REFERENCES `user`(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;