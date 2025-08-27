--
-- Updates the Scribe database structure from < v1.2 to v1.2
--


-- Assigns a more human-readable name to an issue. May also be used to adjust issue severity.
-- issue_id:           ID of the described issue
-- alias:              Alias given to the issue. Empty if no alias is given.
-- new_severity_level: New severity level assigned for the issue. None if severity is not modified.
-- created:            Time when this alias was given
CREATE TABLE `issue_alias`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
	`issue_id` INT NOT NULL,
	`alias` VARCHAR(32),
	`new_severity_level` TINYINT,
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT ia_i_issue_ref_fk FOREIGN KEY ia_i_issue_ref_idx (issue_id) REFERENCES `issue`(`id`) ON DELETE CASCADE
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Comments an issue
-- issue_id: ID of the commented issue
-- text:     The text contents of this comment
-- created:  Time when this comment was recorded
CREATE TABLE `issue_comment`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
	`issue_id` INT NOT NULL,
	`text` VARCHAR(255) NOT NULL,
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	INDEX ic_created_idx (`created`),
	CONSTRAINT ic_i_issue_ref_fk FOREIGN KEY ic_i_issue_ref_idx (issue_id) REFERENCES `issue`(`id`) ON DELETE CASCADE
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Marks an issue as resolved (or to be ignored) in some way
-- resolved_issue_id: ID of the resolved issue
-- comment_id:        ID of the comment added to this resolution, if applicable
-- version_threshold: The last version number (inclusive), to which silencing may apply, and for which notifications are NOT generated.
-- 		None if not restricted by version.
-- created:           Time when this resolution was registered
-- deprecates:        Time when this resolution expires, was removed or was broken. May be in the future.
-- silences:          Whether the issue should not be reported while this resolution is active.
-- notifies:          Whether a notification should be generated if this resolution is broken.
-- 		Note: Only one notification may be generated in total.
CREATE TABLE `issue_resolution`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
	`resolved_issue_id` INT NOT NULL,
	`comment_id` INT,
	`version_threshold` VARCHAR(255),
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	`deprecates` DATETIME,
	`silences` BOOLEAN NOT NULL DEFAULT FALSE,
	`notifies` BOOLEAN NOT NULL DEFAULT FALSE,
	INDEX ir_created_idx (`created`),
	INDEX ir_deprecates_idx (`deprecates`),
	INDEX ir_notifies_idx (`notifies`),
	CONSTRAINT ir_i_resolved_issue_ref_fk FOREIGN KEY ir_i_resolved_issue_ref_idx (resolved_issue_id) REFERENCES `issue`(`id`) ON DELETE CASCADE,
	CONSTRAINT ir_ic_comment_ref_fk FOREIGN KEY ir_ic_comment_ref_idx (comment_id) REFERENCES `issue_comment`(`id`) ON DELETE SET NULL
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Represents a notification generated based on a reappeared issue
-- resolution_id: ID of the resolution on which this notification is based
-- created:       Time when this issue notification was added to the database
-- closed:        Time when this notification was closed / marked as read
CREATE TABLE `issue_notification`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
	`resolution_id` INT NOT NULL,
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	`closed` DATETIME,
	INDEX in_created_idx (`created`),
	INDEX in_closed_idx (`closed`),
	CONSTRAINT in_ir_resolution_ref_fk FOREIGN KEY in_ir_resolution_ref_idx (resolution_id) REFERENCES `issue_resolution`(`id`) ON DELETE CASCADE
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;