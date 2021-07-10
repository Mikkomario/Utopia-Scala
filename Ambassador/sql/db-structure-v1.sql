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

-- Lists settings used when accessing OAuth services
-- Authentication Url is where the user is redirected to
--      accept / perform the authentication (OAuth service side)
-- Token url is where session and refresh tokens are acquired from / refreshed
-- Redirect url is an url pointing to your server to which the OAuth service
--      will redirect user after authentication (/api/version/serviceNode/result)
-- Client ID and Client Secret are specific to your application and given by the OAuth service (keep them secret)
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
    service_side_name VARCHAR(255) NOT NULL,
    client_side_name VARCHAR(64),
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX s_service_idx (service_side_name),
    INDEX s_client_idx (client_side_name)

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
CREATE TABLE oauth_preparation(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    token VARCHAR(48) NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires DATETIME NOT NULL,

    INDEX op_validation_idx (expires, token),

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