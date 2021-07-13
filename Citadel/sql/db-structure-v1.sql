--
-- DB Structure for Utopia Citadel features
-- Intended to be inserted after database creation
-- Supports versions v1.0 and above
--

-- Various languages
CREATE TABLE `language`
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    iso_code VARCHAR(2) NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX l_iso_code_idx (iso_code)

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- 1 = English
INSERT INTO `language` (id, iso_code) VALUES (1, 'en');

-- Language familiarity levels (how good a user can be using a language)
-- Order index is from most preferred to least preferred (Eg. index 1 is more preferred than index 2)
CREATE TABLE language_familiarity
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    order_index INT NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- 1 = Primary language
-- 2 = Fluent and preferred
-- 3 = Fluent
-- 4 = OK
-- 5 = OK, less preferred
-- 6 = Knows a little (better than nothing)
INSERT INTO language_familiarity (id, order_index) VALUES
    (1, 1), (2, 2), (3, 3), (4, 4), (5, 5), (6, 6);

-- Describes individual users
CREATE TABLE `user`
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Versioned settings for users
CREATE TABLE user_settings
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    name VARCHAR(64) NOT NULL,
    email VARCHAR(128) NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deprecated_after DATETIME,

    INDEX us_email_idx (email),
    INDEX us_deprecation_idx (deprecated_after),

    CONSTRAINT us_u_described_user_fk FOREIGN KEY us_u_described_user_idx (user_id)
        REFERENCES `user`(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Links user's known languages to the user
CREATE TABLE user_language
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    language_id INT NOT NULL,
    familiarity_id INT NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ul_u_described_user_fk FOREIGN KEY ul_u_described_user_idx (user_id)
        REFERENCES `user`(id) ON DELETE CASCADE,
    CONSTRAINT ul_l_known_language_fk FOREIGN KEY ul_l_known_language_idx (language_id)
        REFERENCES `language`(id) ON DELETE CASCADE,
    CONSTRAINT ul_lf_language_proficiency_fk FOREIGN KEY ul_lf_language_proficiency_idx (familiarity_id)
        REFERENCES language_familiarity(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Enumeration for various description roles (label, use documentation etc.)
CREATE TABLE description_role
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    json_key_singular VARCHAR(32) NOT NULL,
    json_key_plural VARCHAR(32) NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

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

    CONSTRAINT d_dr_description_purpose_fk FOREIGN KEY d_dr_description_purpose_idx (role_id)
        REFERENCES description_role(id) ON DELETE CASCADE,
    CONSTRAINT d_l_used_language_fk FOREIGN KEY d_l_used_language_idx (language_id)
        REFERENCES `language`(id) ON DELETE CASCADE,
    CONSTRAINT d_u_description_writer_fk FOREIGN KEY d_u_description_writer_idx (author_id)
        REFERENCES `user`(id) ON DELETE SET NULL

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Links descriptions with description roles
CREATE TABLE description_role_description
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    role_id INT NOT NULL,
    description_id INT NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deprecated_after DATETIME,

    INDEX drd_deprecation_idx (deprecated_after),

    CONSTRAINT drd_dr_described_role_fk FOREIGN KEY drd_dr_described_role_idx (role_id)
        REFERENCES description_role(id) ON DELETE CASCADE,
    CONSTRAINT drd_d_description_for_role_fk FOREIGN KEY drd_d_description_for_role_idx (description_id)
        REFERENCES description(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Links descriptions with languages
CREATE TABLE language_description
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    language_id INT NOT NULL,
    description_id INT NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deprecated_after DATETIME,

    INDEX ld_deprecation_idx (deprecated_after),

    CONSTRAINT ld_l_described_language_fk FOREIGN KEY ld_l_described_language_idx (language_id)
        REFERENCES `language`(id) ON DELETE CASCADE,
    CONSTRAINT ld_d_description_for_language_fk FOREIGN KEY ld_d_description_for_language_idx (description_id)
        REFERENCES description(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Links descriptions with language familiarity levels
CREATE TABLE language_familiarity_description
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    familiarity_id INT NOT NULL,
    description_id INT NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deprecated_after DATETIME,

    INDEX lfd_deprecation_idx (deprecated_after),

    CONSTRAINT lfd_lf_described_familiarity_fk FOREIGN KEY lfd_lf_described_familiarity_idx (familiarity_id)
        REFERENCES language_familiarity(id) ON DELETE CASCADE,
    CONSTRAINT lfd_d_familiarity_description_fk FOREIGN KEY lfd_d_familiarity_description_idx (description_id)
        REFERENCES description(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Organizations represent user groups (Eg. company)
CREATE TABLE organization
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    creator_id INT,

    CONSTRAINT o_u_organization_founder_fk FOREIGN KEY o_u_organization_founder_idx (creator_id)
        REFERENCES `user`(id) ON DELETE SET NULL

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Names & descriptions for organizations
CREATE TABLE organization_description
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    organization_id INT NOT NULL,
    description_id INT NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deprecated_after DATETIME,

    INDEX od_creation_idx (created),
    INDEX od_deprecation_idx (deprecated_after),

    CONSTRAINT od_o_described_organization_fk FOREIGN KEY od_o_described_organization_idx (organization_id)
        REFERENCES organization(id) ON DELETE CASCADE,
    CONSTRAINT od_d_organization_description_fk FOREIGN KEY od_d_organization_description_idx (description_id)
        REFERENCES description(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Represent various tasks or features organization members can perform
CREATE TABLE task
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

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

    INDEX td_deprecation_idx (deprecated_after),

    CONSTRAINT td_t_described_task_fk FOREIGN KEY td_t_described_task_idx (task_id)
        REFERENCES task(id) ON DELETE CASCADE,
    CONSTRAINT td_d_task_description_fk FOREIGN KEY td_d_task_description_idx (description_id)
        REFERENCES description(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- An enumeration for various roles within an organization. One user may have multiple roles within an organization.
CREATE TABLE organization_user_role
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

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

    INDEX urd_deprecation_idx (deprecated_after),

    CONSTRAINT urd_our_described_role_fk FOREIGN KEY urd_our_described_role_idx (role_id)
        REFERENCES organization_user_role(id) ON DELETE CASCADE,
    CONSTRAINT urd_d_role_description_fk FOREIGN KEY urd_d_role_description_idx (description_id)
        REFERENCES description(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Links user roles to one or more tasks the users in that role are allowed to perform
CREATE TABLE user_role_right
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    role_id INT NOT NULL,
    task_id INT NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT urr_our_owner_role_fk FOREIGN KEY urr_our_owner_role_idx (role_id)
        REFERENCES organization_user_role(id) ON DELETE CASCADE,
    CONSTRAINT urr_t_right_fk FOREIGN KEY urr_t_right_idx (task_id)
        REFERENCES task(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

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

    INDEX om_starting_idx (started),
    INDEX om_ending_idx (ended),

    CONSTRAINT om_o_parent_organization_fk FOREIGN KEY om_o_parent_organization_idx (organization_id)
        REFERENCES organization(id) ON DELETE CASCADE,
    CONSTRAINT om_u_member_fk FOREIGN KEY om_u_member_idx (user_id)
        REFERENCES `user`(id) ON DELETE CASCADE,
    CONSTRAINT om_u_membership_adder_fk FOREIGN KEY om_u_membership_adder_idx (creator_id)
        REFERENCES `user`(id) ON DELETE SET NULL

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Links organization members with their roles in the organizations
CREATE TABLE organization_member_role
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    membership_id INT NOT NULL,
    role_id INT NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deprecated_after DATETIME,
    creator_id INT,

    INDEX omr_ending_idx (deprecated_after),

    CONSTRAINT omr_described_membership_fk FOREIGN KEY omr_described_membership_idx (membership_id)
        REFERENCES organization_membership(id) ON DELETE CASCADE,
    CONSTRAINT omr_our_member_role_fk FOREIGN KEY omr_our_member_role_idx (role_id)
        REFERENCES organization_user_role(id) ON DELETE CASCADE,
    CONSTRAINT omr_u_role_adder_fk FOREIGN KEY omr_u_role_adder_idx (creator_id)
        REFERENCES `user`(id) ON DELETE SET NULL

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;


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

    CONSTRAINT oi_o_target_organization_fk FOREIGN KEY oi_o_target_organization_idx (organization_id)
        REFERENCES organization(id) ON DELETE CASCADE,
    CONSTRAINT oi_u_invited_user_fk FOREIGN KEY oi_u_invited_user_idx (recipient_id)
        REFERENCES `user`(id) ON DELETE CASCADE,
    CONSTRAINT oi_our_initial_role_fk FOREIGN KEY oi_our_initial_role_idx (starting_role_id)
        REFERENCES organization_user_role(id) ON DELETE CASCADE,
    CONSTRAINT oi_u_inviter_fk FOREIGN KEY oi_u_inviter_idx (creator_id)
        REFERENCES `user`(id) ON DELETE SET NULL

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Registered responses (yes|no) to organization invitations
CREATE TABLE invitation_response
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    invitation_id INT NOT NULL,
    was_accepted BOOLEAN NOT NULL DEFAULT FALSE,
    was_blocked BOOLEAN NOT NULL DEFAULT FALSE,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    creator_id INT NOT NULL,

    CONSTRAINT ir_oi_opened_invitation_fk FOREIGN KEY ir_oi_opened_invitation_idx (invitation_id)
        REFERENCES organization_invitation(id) ON DELETE CASCADE,
    CONSTRAINT ir_u_recipient_fk FOREIGN KEY ir_u_recipient_idx (creator_id)
        REFERENCES `user`(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;


-- Requested deletions for an organization
-- There is a period of time during which organization owners may cancel the deletion
CREATE TABLE organization_deletion
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    organization_id INT NOT NULL,
    creator_id INT NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualization DATETIME NOT NULL,

    INDEX od_actualization_idx (actualization),

    CONSTRAINT od_o_deletion_target_fk FOREIGN KEY od_o_deletion_target_idx (organization_id)
        REFERENCES organization(id) ON DELETE CASCADE,
    CONSTRAINT od_u_deletion_proposer_fk FOREIGN KEY od_u_deletion_proposer_idx (creator_id)
        REFERENCES `user`(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

CREATE TABLE organization_deletion_cancellation
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    deletion_id INT NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    creator_id INT,

    INDEX odc_cancel_time_idx (created),

    CONSTRAINT odc_od_cancelled_deletion_fk FOREIGN KEY odc_od_cancelled_deletion_idx (deletion_id)
        REFERENCES organization_deletion(id) ON DELETE CASCADE,
    CONSTRAINT odc_u_cancel_author_fk FOREIGN KEY odc_u_cancel_author_idx (creator_id)
        REFERENCES `user`(id) ON DELETE SET NULL

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;


-- Devices the users use to log in and use this service
CREATE TABLE client_device
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    creator_id INT,

    CONSTRAINT cd_u_first_device_user_fk FOREIGN KEY cd_u_first_device_user_idx (creator_id)
        REFERENCES `user`(id) ON DELETE SET NULL

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Names and descriptions of client devices
CREATE TABLE client_device_description
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    device_id INT NOT NULL,
    description_id INT NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deprecated_after DATETIME,

    INDEX cdd_timeline_idx (deprecated_after, created),

    CONSTRAINT cdd_cd_described_device_fk FOREIGN KEY cdd_cd_described_device_idx (device_id)
        REFERENCES client_device(id) ON DELETE CASCADE,
    CONSTRAINT cdd_d_device_description_fk FOREIGN KEY cdd_d_device_description_idx (description_id)
        REFERENCES description(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Links users with the devices they have used
CREATE TABLE client_device_user
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    device_id INT NOT NULL,
    user_id INT NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deprecated_after DATETIME,

    INDEX cdu_timeline_idx (deprecated_after, created),

    CONSTRAINT cdu_cd_used_client_device_fk FOREIGN KEY cdu_cd_used_client_device_idx (device_id)
        REFERENCES client_device(id) ON DELETE CASCADE,
    CONSTRAINT cdu_u_device_user_fk FOREIGN KEY cdu_u_device_user_idx (user_id)
        REFERENCES `user`(id) ON DELETE CASCADE

)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;