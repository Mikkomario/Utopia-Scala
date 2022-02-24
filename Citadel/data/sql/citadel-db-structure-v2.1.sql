-- 
-- Database structure for Citadel models
-- Version: v2.1
-- Last generated: 2022-02-24
--

--	Organization	----------

-- Represents an organization or a user group
-- creator_id: Id of the user who created this organization (if still known)
-- created:    Time when this organization was first created
CREATE TABLE `organization`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`creator_id` INT, 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	CONSTRAINT o_u_creator_ref_fk FOREIGN KEY o_u_creator_ref_idx (creator_id) REFERENCES `user`(`id`) ON DELETE SET NULL
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Represents a type of task a user can perform (within an organization)
-- created: Time when this task was first created
CREATE TABLE `task`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- An enumeration for different roles a user may have within an organization
-- created: Time when this user role was first created
CREATE TABLE `user_role`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Represents an invitation to join an organization
-- organization_id:  Id of the organization which the recipient is invited to join
-- starting_role_id: The role the recipient will have in the organization initially if they join
-- expires:          Time when this invitation expires / becomes invalid
-- recipient_id:     Id of the invited user, if known
-- recipient_email:  Email address of the invited user / the email address where this invitation is sent to
-- message:          Message written by the sender to accompany this invitation
-- sender_id:        Id of the user who sent this invitation, if still known
-- created:          Time when this invitation was created / sent
CREATE TABLE `invitation`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`organization_id` INT NOT NULL, 
	`starting_role_id` INT NOT NULL, 
	`expires` DATETIME NOT NULL, 
	`recipient_id` INT, 
	`recipient_email` VARCHAR(128), 
	`message` VARCHAR(480), 
	`sender_id` INT, 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	INDEX i_created_idx (`created`), 
	INDEX i_combo_1_idx (expires, recipient_email), 
	CONSTRAINT i_o_organization_ref_fk FOREIGN KEY i_o_organization_ref_idx (organization_id) REFERENCES `organization`(`id`) ON DELETE CASCADE, 
	CONSTRAINT i_ur_starting_role_ref_fk FOREIGN KEY i_ur_starting_role_ref_idx (starting_role_id) REFERENCES `user_role`(`id`) ON DELETE CASCADE, 
	CONSTRAINT i_u_recipient_ref_fk FOREIGN KEY i_u_recipient_ref_idx (recipient_id) REFERENCES `user`(`id`) ON DELETE SET NULL, 
	CONSTRAINT i_u_sender_ref_fk FOREIGN KEY i_u_sender_ref_idx (sender_id) REFERENCES `user`(`id`) ON DELETE SET NULL
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Lists organization members, including membership history
-- organization_id: Id of the organization the referenced user is/was a member of
-- user_id:         Id of the user who is/was a member of the referenced organization
-- creator_id:      Id of the user who created/started this membership
-- started:         Time when this membership started
-- ended:           Time when this membership ended (if applicable)
CREATE TABLE `membership`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`organization_id` INT NOT NULL, 
	`user_id` INT NOT NULL, 
	`creator_id` INT, 
	`started` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	`ended` DATETIME, 
	INDEX m_started_idx (`started`), 
	INDEX m_ended_idx (`ended`), 
	CONSTRAINT m_o_organization_ref_fk FOREIGN KEY m_o_organization_ref_idx (organization_id) REFERENCES `organization`(`id`) ON DELETE CASCADE, 
	CONSTRAINT m_u_user_ref_fk FOREIGN KEY m_u_user_ref_idx (user_id) REFERENCES `user`(`id`) ON DELETE CASCADE, 
	CONSTRAINT m_u_creator_ref_fk FOREIGN KEY m_u_creator_ref_idx (creator_id) REFERENCES `user`(`id`) ON DELETE SET NULL
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Represents a request to delete an organization. There exists a time period between the request and its completion, during which other users may cancel the deletion.
-- organization_id: Id of the organization whose deletion was requested
-- actualization:   Time when this deletion is/was scheduled to actualize
-- creator_id:      Id of the user who requested organization deletion
-- created:         Time when this deletion was requested
CREATE TABLE `organization_deletion`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`organization_id` INT NOT NULL, 
	`actualization` DATETIME NOT NULL, 
	`creator_id` INT NOT NULL, 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	INDEX orgdel_actualization_idx (`actualization`), 
	INDEX orgdel_created_idx (`created`), 
	CONSTRAINT orgdel_o_organization_ref_fk FOREIGN KEY orgdel_o_organization_ref_idx (organization_id) REFERENCES `organization`(`id`) ON DELETE CASCADE, 
	CONSTRAINT orgdel_u_creator_ref_fk FOREIGN KEY orgdel_u_creator_ref_idx (creator_id) REFERENCES `user`(`id`) ON DELETE CASCADE
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Used for listing / linking, which tasks different organization membership roles allow
-- role_id: Id of the organization user role that has authorization to perform the referenced task
-- task_id: Id of the task the user's with referenced user role are allowed to perform
-- created: Time when this user role right was first created
CREATE TABLE `user_role_right`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`role_id` INT NOT NULL, 
	`task_id` INT NOT NULL, 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	CONSTRAINT urr_ur_role_ref_fk FOREIGN KEY urr_ur_role_ref_idx (role_id) REFERENCES `user_role`(`id`) ON DELETE CASCADE, 
	CONSTRAINT urr_t_task_ref_fk FOREIGN KEY urr_t_task_ref_idx (task_id) REFERENCES `task`(`id`) ON DELETE CASCADE
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Represents a response (yes|no) to an invitation to join an organization
-- invitation_id: Id of the invitation this response is for
-- message:       Attached written response
-- creator_id:    Id of the user who responded to the invitation, if still known
-- created:       Time when this invitation response was first created
-- accepted:      Whether the invitation was accepted (true) or rejected (false)
-- blocked:       Whether future invitations were blocked
CREATE TABLE `invitation_response`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`invitation_id` INT NOT NULL, 
	`message` VARCHAR(480), 
	`creator_id` INT, 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	`accepted` BOOLEAN NOT NULL DEFAULT FALSE, 
	`blocked` BOOLEAN NOT NULL DEFAULT FALSE, 
	INDEX ir_created_idx (`created`), 
	CONSTRAINT ir_i_invitation_ref_fk FOREIGN KEY ir_i_invitation_ref_idx (invitation_id) REFERENCES `invitation`(`id`) ON DELETE CASCADE, 
	CONSTRAINT ir_u_creator_ref_fk FOREIGN KEY ir_u_creator_ref_idx (creator_id) REFERENCES `user`(`id`) ON DELETE SET NULL
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Links an organization membership to the roles that member has within that organization
-- membership_id:    Id of the membership / member that has the referenced role
-- role_id:          Id of role the referenced member has
-- creator_id:       Id of the user who added this role to the membership, if known
-- created:          Time when this role was added for the organization member
-- deprecated_after: Time when this member role link became deprecated. None while this member role link is still valid.
CREATE TABLE `member_role`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`membership_id` INT NOT NULL, 
	`role_id` INT NOT NULL, 
	`creator_id` INT, 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	`deprecated_after` DATETIME, 
	INDEX mr_deprecated_after_idx (`deprecated_after`), 
	CONSTRAINT mr_m_membership_ref_fk FOREIGN KEY mr_m_membership_ref_idx (membership_id) REFERENCES `membership`(`id`) ON DELETE CASCADE, 
	CONSTRAINT mr_ur_role_ref_fk FOREIGN KEY mr_ur_role_ref_idx (role_id) REFERENCES `user_role`(`id`) ON DELETE CASCADE, 
	CONSTRAINT mr_u_creator_ref_fk FOREIGN KEY mr_u_creator_ref_idx (creator_id) REFERENCES `user`(`id`) ON DELETE SET NULL
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Records a cancellation for a pending organization deletion request
-- deletion_id: Id of the cancelled deletion
-- creator_id:  Id of the user who cancelled the referenced organization deletion, if still known
-- created:     Time when this organization deletion cancellation was first created
CREATE TABLE `organization_deletion_cancellation`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`deletion_id` INT NOT NULL, 
	`creator_id` INT, 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	INDEX odc_created_idx (`created`), 
	CONSTRAINT odc_orgdel_deletion_ref_fk FOREIGN KEY odc_orgdel_deletion_ref_idx (deletion_id) REFERENCES `organization_deletion`(`id`) ON DELETE CASCADE, 
	CONSTRAINT odc_u_creator_ref_fk FOREIGN KEY odc_u_creator_ref_idx (creator_id) REFERENCES `user`(`id`) ON DELETE SET NULL
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;


--	Language	----------

-- Represents a language
-- iso_code: 2 letter ISO-standard code for this language
-- created:  Time when this language was first created
CREATE TABLE `language`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`iso_code` VARCHAR(2) NOT NULL, 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	INDEX l_iso_code_idx (`iso_code`)
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Represents a language skill level
-- order_index: Index used for ordering between language familiarities, where lower values mean higher familiarity
-- created:     Time when this language familiarity was first created
CREATE TABLE `language_familiarity`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`order_index` TINYINT NOT NULL, 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	INDEX lf_order_index_idx (`order_index`)
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;


--	Description	----------

-- An enumeration for different roles or purposes a description can serve
-- json_key_singular: Key used in json documents for a singular value (string) of this description role
-- json_key_plural:   Key used in json documents for multiple values (array) of this description role
-- created:           Time when this description role was first created
CREATE TABLE `description_role`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`json_key_singular` VARCHAR(32) NOT NULL, 
	`json_key_plural` VARCHAR(32) NOT NULL, 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Represents some description of some item in some language
-- role_id:          Id of the role of this description
-- language_id:      Id of the language this description is written in
-- text:             This description as text / written description
-- author_id:        Id of the user who wrote this description (if known and applicable)
-- created:          Time when this description was written
-- deprecated_after: Time when this description was removed or replaced with a new version
CREATE TABLE `description`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`role_id` INT NOT NULL, 
	`language_id` INT NOT NULL, 
	`text` VARCHAR(64) NOT NULL, 
	`author_id` INT, 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	`deprecated_after` DATETIME, 
	INDEX d_created_idx (`created`), 
	INDEX d_deprecated_after_idx (`deprecated_after`), 
	CONSTRAINT d_dr_role_ref_fk FOREIGN KEY d_dr_role_ref_idx (role_id) REFERENCES `description_role`(`id`) ON DELETE CASCADE, 
	CONSTRAINT d_l_language_ref_fk FOREIGN KEY d_l_language_ref_idx (language_id) REFERENCES `language`(`id`) ON DELETE CASCADE, 
	CONSTRAINT d_u_author_ref_fk FOREIGN KEY d_u_author_ref_idx (author_id) REFERENCES `user`(`id`) ON DELETE SET NULL
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Links DescriptionRoles with their descriptions
-- role_id:        Id of the described description role
-- description_id: Id of the linked description
CREATE TABLE `description_role_description`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`role_id` INT NOT NULL, 
	`description_id` INT NOT NULL, 
	CONSTRAINT drd_dr_role_ref_fk FOREIGN KEY drd_dr_role_ref_idx (role_id) REFERENCES `description_role`(`description_id`) ON DELETE CASCADE, 
	CONSTRAINT drd_d_description_ref_fk FOREIGN KEY drd_d_description_ref_idx (description_id) REFERENCES `description`(`id`) ON DELETE CASCADE
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Links Languages with their descriptions
-- language_id:    Id of the described language
-- description_id: Id of the linked description
CREATE TABLE `language_description`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`language_id` INT NOT NULL, 
	`description_id` INT NOT NULL, 
	CONSTRAINT ld_l_language_ref_fk FOREIGN KEY ld_l_language_ref_idx (language_id) REFERENCES `language`(`description_id`) ON DELETE CASCADE, 
	CONSTRAINT ld_d_description_ref_fk FOREIGN KEY ld_d_description_ref_idx (description_id) REFERENCES `description`(`id`) ON DELETE CASCADE
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Links LanguageFamiliarities with their descriptions
-- familiarity_id: Id of the described language familiarity
-- description_id: Id of the linked description
CREATE TABLE `language_familiarity_description`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`familiarity_id` INT NOT NULL, 
	`description_id` INT NOT NULL, 
	CONSTRAINT lfd_lf_familiarity_ref_fk FOREIGN KEY lfd_lf_familiarity_ref_idx (familiarity_id) REFERENCES `language_familiarity`(`description_id`) ON DELETE CASCADE, 
	CONSTRAINT lfd_d_description_ref_fk FOREIGN KEY lfd_d_description_ref_idx (description_id) REFERENCES `description`(`id`) ON DELETE CASCADE
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Links Organizations with their descriptions
-- organization_id: Id of the described organization
-- description_id:  Id of the linked description
CREATE TABLE `organization_description`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`organization_id` INT NOT NULL, 
	`description_id` INT NOT NULL, 
	CONSTRAINT orgdes_o_organization_ref_fk FOREIGN KEY orgdes_o_organization_ref_idx (organization_id) REFERENCES `organization`(`description_id`) ON DELETE CASCADE, 
	CONSTRAINT orgdes_d_description_ref_fk FOREIGN KEY orgdes_d_description_ref_idx (description_id) REFERENCES `description`(`id`) ON DELETE CASCADE
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Links Tasks with their descriptions
-- task_id:        Id of the described task
-- description_id: Id of the linked description
CREATE TABLE `task_description`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`task_id` INT NOT NULL, 
	`description_id` INT NOT NULL, 
	CONSTRAINT td_t_task_ref_fk FOREIGN KEY td_t_task_ref_idx (task_id) REFERENCES `task`(`description_id`) ON DELETE CASCADE, 
	CONSTRAINT td_d_description_ref_fk FOREIGN KEY td_d_description_ref_idx (description_id) REFERENCES `description`(`id`) ON DELETE CASCADE
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Links UserRoles with their descriptions
-- role_id:        Id of the described user role
-- description_id: Id of the linked description
CREATE TABLE `user_role_description`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`role_id` INT NOT NULL, 
	`description_id` INT NOT NULL, 
	CONSTRAINT urd_ur_role_ref_fk FOREIGN KEY urd_ur_role_ref_idx (role_id) REFERENCES `user_role`(`description_id`) ON DELETE CASCADE, 
	CONSTRAINT urd_d_description_ref_fk FOREIGN KEY urd_d_description_ref_idx (description_id) REFERENCES `description`(`id`) ON DELETE CASCADE
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;


--	User	----------

-- Represents a software user
-- created: Time when this user was first created
CREATE TABLE `user`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Links user with their language familiarity levels
-- user_id:        Id of the user who's being described
-- language_id:    Id of the language known to the user
-- familiarity_id: Id of the user's familiarity level in the referenced language
-- created:        Time when this user language link was first created
CREATE TABLE `user_language`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`user_id` INT NOT NULL, 
	`language_id` INT NOT NULL, 
	`familiarity_id` INT NOT NULL, 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	CONSTRAINT ul_u_user_ref_fk FOREIGN KEY ul_u_user_ref_idx (user_id) REFERENCES `user`(`id`) ON DELETE CASCADE, 
	CONSTRAINT ul_l_language_ref_fk FOREIGN KEY ul_l_language_ref_idx (language_id) REFERENCES `language`(`id`) ON DELETE CASCADE, 
	CONSTRAINT ul_lf_familiarity_ref_fk FOREIGN KEY ul_lf_familiarity_ref_idx (familiarity_id) REFERENCES `language_familiarity`(`id`) ON DELETE CASCADE
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Versioned user-specific settings
-- user_id:          Id of the described user
-- name:             Name used by this user
-- email:            Email address of this user
-- created:          Time when this user settings was first created
-- deprecated_after: Time when these settings were replaced with a more recent version (if applicable)
CREATE TABLE `user_settings`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`user_id` INT NOT NULL, 
	`name` VARCHAR(64) NOT NULL, 
	`email` VARCHAR(128), 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	`deprecated_after` DATETIME, 
	INDEX us_name_idx (`name`), 
	INDEX us_email_idx (`email`), 
	INDEX us_created_idx (`created`), 
	INDEX us_deprecated_after_idx (`deprecated_after`), 
	CONSTRAINT us_u_user_ref_fk FOREIGN KEY us_u_user_ref_idx (user_id) REFERENCES `user`(`id`) ON DELETE CASCADE
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

