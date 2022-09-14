--
-- DB Structure for Utopia Exodus features
-- Intended to be inserted after database creation
-- Supports versions before v2.0
--

-- Various languages
CREATE TABLE `language`
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    iso_code VARCHAR(2) NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX (iso_code)

)Engine=InnoDB DEFAULT CHARSET=latin1;

-- 1 = English
INSERT INTO `language` (id, iso_code) VALUES (1, 'en');

-- Language familiarity levels (how good a user can be using a language)
-- Order index is from most preferred to least preferred (Eg. index 1 is more preferred than index 2)
CREATE TABLE language_familiarity
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    order_index INT NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP

)Engine=InnoDB DEFAULT CHARSET=latin1;

-- 1 = Primary language
-- 2 = Fluent and preferred
-- 3 = Fluent
-- 4 = OK
-- 5 = OK, less preferred
-- 6 = Knows a little (better than nothing)
INSERT INTO language_familiarity (id, order_index) VALUES
    (1, 1), (2, 2), (3, 3), (4, 4), (5, 5), (6, 6);

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

)Engine=InnoDB DEFAULT CHARSET=latin1;

-- Contains a list of purposes for email validation (server-side only)
-- This is to limit the possible misuse of these validation tokens to contexts for which they were created
CREATE TABLE email_validation_purpose
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    name_en VARCHAR(16) NOT NULL

)Engine=InnoDB DEFAULT CHARSET=latin1;

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

)Engine=InnoDB DEFAULT CHARSET=latin1;

-- Records resend attempts in order to limit possible spam
CREATE TABLE email_validation_resend
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    validation_id INT NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT evr_ev_validation_link_fb FOREIGN KEY evr_ev_validation_link_idk (validation_id)
        REFERENCES email_validation(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARSET=latin1;

-- Describes prompted new users which are waiting for an email validation & completion

-- Describes individual users
CREATE TABLE `user`
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP

)Engine=InnoDB DEFAULT CHARSET=latin1;

-- Versioned settings for users
CREATE TABLE user_settings
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    name VARCHAR(64) NOT NULL,
    email VARCHAR(128) NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deprecated_after DATETIME,

    INDEX (name),
    INDEX (email),
    INDEX (deprecated_after),

    FOREIGN KEY us_u_described_user (user_id)
        REFERENCES `user`(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARSET=latin1;

-- Contains hashed user passwords
CREATE TABLE user_authentication
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    hash VARCHAR(128) NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX ua_user_password_idx (hash, user_id),

    FOREIGN KEY ua_u_link_to_owner (user_id)
        REFERENCES `user`(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARSET=latin1;

-- Links user's known languages to the user
CREATE TABLE user_language
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    language_id INT NOT NULL,
    familiarity_id INT NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY ul_u_described_user (user_id)
        REFERENCES `user`(id) ON DELETE CASCADE,

    FOREIGN KEY ul_l_known_language (language_id)
        REFERENCES `language`(id) ON DELETE CASCADE,

    FOREIGN KEY ul_lf_language_proficiency (familiarity_id)
        REFERENCES language_familiarity(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARSET=latin1;

-- Enumeration for various description roles (label, use documentation etc.)
CREATE TABLE description_role
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    json_key_singular VARCHAR(32) NOT NULL,
    json_key_plural VARCHAR(32) NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP

)Engine=InnoDB DEFAULT CHARSET=latin1;

-- 1 = Name
INSERT INTO description_role (id, json_key_singular, json_key_plural) VALUES (1, 'name', 'names');

-- Descriptions describe various things.
-- Descriptions can be overwritten and are written in a specific language.
-- Usually there is only up to one description available for each item, per language.
CREATE TABLE description
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    role_id INT NOT NULL,
    language_id INT NOT NULL,
    `text` VARCHAR(255) NOT NULL,
    author_id INT,
    created TIMESTAMP NOT NULl DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY d_dr_description_purpose (role_id)
        REFERENCES description_role(id) ON DELETE CASCADE,

    FOREIGN KEY d_l_used_language (language_id)
        REFERENCES `language`(id) ON DELETE CASCADE,

    FOREIGN KEY d_u_description_writer (author_id)
        REFERENCES `user`(id) ON DELETE SET NULL

)Engine=InnoDB DEFAULT CHARSET=latin1;

-- Links descriptions with description roles
CREATE TABLE description_role_description
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    role_id INT NOT NULL,
    description_id INT NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deprecated_after DATETIME,

    INDEX (deprecated_after),

    FOREIGN KEY drd_dr_described_role (role_id)
        REFERENCES description_role(id) ON DELETE CASCADE,

    FOREIGN KEY drd_d_description_for_role (description_id)
        REFERENCES description(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARSET=latin1;

-- Description #1 = Name description role name
INSERT INTO description (id, role_id, language_id, `text`) VALUES
    (1, 1, 1, 'Name');
INSERT INTO description_role_description (role_id, description_id) VALUES (1, 1);

-- Links descriptions with languages
CREATE TABLE language_description
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    language_id INT NOT NULL,
    description_id INT NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deprecated_after DATETIME,

    INDEX (deprecated_after),

    FOREIGN KEY ld_l_described_language (language_id)
        REFERENCES `language`(id) ON DELETE CASCADE,

    FOREIGN KEY ld_d_description_for_language (description_id)
        REFERENCES description(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARSET=latin1;

-- Description #2 = English language name
INSERT INTO description (id, role_id, language_id, `text`) VALUES
    (2, 1, 1, 'English');
INSERT INTO language_description (language_id, description_id) VALUES (1, 2);

-- Links descriptions with language familiarity levels
CREATE TABLE language_familiarity_description
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    familiarity_id INT NOT NULL,
    description_id INT NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deprecated_after DATETIME,

    INDEX (deprecated_after),

    FOREIGN KEY lfd_lf_described_language_familiarity (familiarity_id)
        REFERENCES language_familiarity(id) ON DELETE CASCADE,

    FOREIGN KEY lfd_d_description_for_language_familiarity (description_id)
        REFERENCES description(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARSET=latin1;

-- Descriptions #3-8 = Language familiarity level names
INSERT INTO description (id, role_id, language_id, `text`) VALUES
    (3, 1, 1, 'Primary Language'),
    (4, 1, 1, 'Fluent and Preferred'),
    (5, 1, 1, 'Fluent'),
    (6, 1, 1, 'OK'),
    (7, 1, 1, 'OK, less preferred'),
    (8, 1, 1, 'Better than Nothing');
INSERT INTO language_familiarity_description (familiarity_id, description_id) VALUES
    (1, 3), (2, 4), (3, 5), (4, 6), (5, 7), (6, 8);

-- Organizations represent user groups (Eg. company)
CREATE TABLE organization
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    creator_id INT,

    FOREIGN KEY o_u_organization_founder (creator_id)
        REFERENCES `user`(id) ON DELETE SET NULL

)Engine=InnoDB DEFAULT CHARSET=latin1;

-- Names & descriptions for organizations
CREATE TABLE organization_description
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    organization_id INT NOT NULL,
    description_id INT NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deprecated_after DATETIME,

    INDEX (deprecated_after),

    FOREIGN KEY od_o_described_organization (organization_id)
        REFERENCES organization(id) ON DELETE CASCADE,

    FOREIGN KEY od_d_description_for_organization (description_id)
        REFERENCES description(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARSET=latin1;

-- Represent various tasks or features organization members can perform
CREATE TABLE task
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP

)Engine=InnoDB DEFAULT CHARSET=latin1;

-- 1 = Delete organization
-- 2 = Change user roles (to similar or lower)
-- 3 = Invite new users to organization (with similar or lower role)
-- 4 = Edit organization description (including name)
-- 5 = Remove users (of lower role) from the organization
-- 6 = Cancel organization deletion
INSERT INTO task (id) VALUES (1), (2), (3), (4), (5), (6);

-- Names and descriptions of various tasks
CREATE TABLE task_description
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    task_id INT NOT NULL,
    description_id INT NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deprecated_after DATETIME,

    INDEX (deprecated_after),

    FOREIGN KEY td_t_described_task (task_id)
        REFERENCES task(id) ON DELETE CASCADE,

    FOREIGN KEY td_d_description_for_task (description_id)
        REFERENCES description(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARSET=latin1;

-- Descriptions #9-14 = Task description names
INSERT INTO description (id, role_id, language_id, `text`) VALUES
    (9, 1, 1, 'Delete Organization'),
    (10, 1, 1, 'Change User Roles'),
    (11, 1, 1, 'Invite Users'),
    (12, 1, 1, 'Edit Organization Description'),
    (13, 1, 1, 'Remove Users'),
    (14, 1, 1, 'Cancel Organization Deletion');
INSERT INTO task_description (task_id, description_id) VALUES
    (1, 9), (2, 10), (3, 11), (4, 12), (5, 13), (6, 14);

-- An enumeration for various roles within an organization. One user may have multiple roles within an organization.
CREATE TABLE organization_user_role
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP

)Engine=InnoDB DEFAULT CHARSET=latin1;

-- 1 = Owner (all rights)
-- 2 = Admin/Steward (all rights except owner-specific rights)
-- 3 = Manager (rights to modify users)
-- 4 = Developer (rights to create & edit resources and to publish)
-- 5 = Publisher (Read access to data + publish rights)
-- 5 = Reader (Read only access to data)
INSERT INTO organization_user_role (id) VALUES (1), (2);

-- Links to descriptions of user roles
CREATE TABLE user_role_description
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    role_id INT NOT NULL,
    description_id INT NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deprecated_after DATETIME,

    INDEX (deprecated_after),

    FOREIGN KEY urd_our_described_role (role_id)
        REFERENCES organization_user_role(id) ON DELETE CASCADE,

    FOREIGN KEY urd_d_description_for_role (description_id)
        REFERENCES description(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARSET=latin1;

-- Descriptions #15-16 = User role names
INSERT INTO description (id, role_id, language_id, `text`) VALUES
    (15, 1, 1, 'Owner'),
    (16, 1, 1, 'Admin');
INSERT INTO user_role_description (role_id, description_id) VALUES
    (1, 15), (2, 16);

-- Links user roles to one or more tasks the users in that role are allowed to perform
CREATE TABLE user_role_right
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    role_id INT NOT NULL,
    task_id INT NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY urr_our_owner_role (role_id)
        REFERENCES organization_user_role(id) ON DELETE CASCADE,

    FOREIGN KEY urr_t_right (task_id)
        REFERENCES task(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARSET=latin1;

INSERT INTO user_role_right (role_id, task_id) VALUES
    (1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6),
    (2, 2), (2, 3), (2, 4), (2, 5);

-- Contains links between users and organizations (many-to-many)
-- One user may belong to multiple organizations and one organization may contain multiple users
CREATE TABLE organization_membership
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    organization_id INT NOT NULL,
    user_id INT NOT NULL,
    started TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended DATETIME,
    creator_id INT,

    INDEX (ended),

    FOREIGN KEY om_o_parent_organization (organization_id)
        REFERENCES organization(id) ON DELETE CASCADE,

    FOREIGN KEY om_u_member (user_id)
        REFERENCES `user`(id) ON DELETE CASCADE,

    FOREIGN KEY om_u_membership_adder (creator_id)
        REFERENCES `user`(id) ON DELETE SET NULL

)Engine=InnoDB DEFAULT CHARSET=latin1;

-- Links organization members with their roles in the organizations
CREATE TABLE organization_member_role
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    membership_id INT NOT NULL,
    role_id INT NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deprecated_after DATETIME,
    creator_id INT,

    INDEX (deprecated_after),

    FOREIGN KEY omr_described_membership (membership_id)
        REFERENCES organization_membership(id) ON DELETE CASCADE,

    FOREIGN KEY omr_our_member_role (role_id)
        REFERENCES organization_user_role(id) ON DELETE CASCADE,

    FOREIGN KEY omr_u_role_adder (creator_id)
        REFERENCES `user`(id) ON DELETE SET NULL

)Engine=InnoDB DEFAULT CHARSET=latin1;


-- Contains invitations for joining an organization
CREATE TABLE organization_invitation
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    organization_id INT NOT NULL,
    recipient_id INT,
    recipient_email VARCHAR(128),
    starting_role_id INT NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_in DATETIME NOT NULL,
    creator_id INT,

    INDEX oi_active_invitations_idx (expires_in, recipient_email),

    FOREIGN KEY oi_o_target_organization (organization_id)
        REFERENCES organization(id) ON DELETE CASCADE,

    FOREIGN KEY oi_u_invited_user (recipient_id)
        REFERENCES `user`(id) ON DELETE CASCADE,

    FOREIGN KEY oi_our_initial_role (starting_role_id)
        REFERENCES organization_user_role(id) ON DELETE CASCADE,

    FOREIGN KEY oi_u_inviter (creator_id)
        REFERENCES `user`(id) ON DELETE SET NULL

)Engine=InnoDB DEFAULT CHARSET=latin1;

-- Registered responses (yes|no) to organization invitations
CREATE TABLE invitation_response
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    invitation_id INT NOT NULL,
    was_accepted BOOLEAN NOT NULL DEFAULT FALSE,
    was_blocked BOOLEAN NOT NULL DEFAULT FALSE,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    creator_id INT NOT NULL,

    FOREIGN KEY ir_oi_opened_invitation (invitation_id)
        REFERENCES organization_invitation(id) ON DELETE CASCADE,

    FOREIGN KEY ir_u_recipient (creator_id)
        REFERENCES `user`(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARSET=latin1;


-- Requested deletions for an organization
-- There is a period of time during which organization owners may cancel the deletion
CREATE TABLE organization_deletion
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    organization_id INT NOT NULL,
    creator_id INT NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualization DATETIME NOT NULL,

    INDEX (actualization),

    FOREIGN KEY od_o_deletion_target (organization_id)
        REFERENCES organization(id) ON DELETE CASCADE,

    FOREIGN KEY od_u_deletion_proposer (creator_id)
        REFERENCES `user`(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE organization_deletion_cancellation
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    deletion_id INT NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    creator_id INT,

    FOREIGN KEY odc_od_cancelled_deletion (deletion_id)
        REFERENCES organization_deletion(id) ON DELETE CASCADE,

    FOREIGN KEY odc_u_cancel_author (creator_id)
        REFERENCES `user`(id) ON DELETE SET NULL

)Engine=InnoDB DEFAULT CHARSET=latin1;


-- Devices the users use to log in and use this service
CREATE TABLE client_device
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    creator_id INT,

    FOREIGN KEY cd_u_first_device_user (creator_id)
        REFERENCES `user`(id) ON DELETE SET NULL

)Engine=InnoDB DEFAULT CHARSET=latin1;

-- Names and descriptions of client devices
CREATE TABLE client_device_description
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    device_id INT NOT NULL,
    description_id INT NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deprecated_after DATETIME,

    INDEX cdd_timeline_idx (deprecated_after, created),

    FOREIGN KEY cdd_cd_described_device (device_id)
        REFERENCES client_device(id) ON DELETE CASCADE,

    FOREIGN KEY cdd_d_description_for_device (description_id)
        REFERENCES description(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARSET=latin1;

-- Links users with the devices they have used
CREATE TABLE client_device_user
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    device_id INT NOT NULL,
    user_id INT NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deprecated_after DATETIME,

    INDEX cdu_timeline_idx (deprecated_after, created),

    FOREIGN KEY cdu_cd_used_client_device (device_id)
        REFERENCES client_device(id) ON DELETE CASCADE,

    FOREIGN KEY cdu_u_device_user (user_id)
        REFERENCES `user`(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARSET=latin1;

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

    FOREIGN KEY dak_u_key_owner (user_id)
        REFERENCES `user`(id) ON DELETE CASCADE,

    FOREIGN KEY dak_cd_connected_device (device_id)
        REFERENCES client_device(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARSET=latin1;

-- Represents a temporary login session
CREATE TABLE user_session
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    device_id INT,
    `key` VARCHAR(36) NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_in DATETIME NOT NULL,
    logout_time DATETIME,

    INDEX us_active_key (`key`, expires_in, logout_time),

    FOREIGN KEY us_u_session_owner (user_id)
        REFERENCES `user`(id) ON DELETE CASCADE,

    FOREIGN KEY us_cd_session_device (device_id)
        REFERENCES client_device(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARSET=latin1;