-- 
-- Database structure for scribe models
-- Version: v1.2
-- Last generated: 2025-08-27
--

--	Logging	----------

-- Represents a type of problem or an issue that may occur during a program's run
-- context:        Program context where this issue occurred or was logged. Should be unique.
-- severity_level: The estimated severity of this issue
-- 		References enumeration Severity
-- 		Possible values are: 1 = debug, 2 = info, 3 = warning, 4 = recoverable, 5 = unrecoverable, 6 = critical
-- created:        Time when this issue first occurred or was first recorded
CREATE TABLE `issue`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`context` VARCHAR(96) NOT NULL, 
	`severity_level` TINYINT NOT NULL, 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	INDEX i_created_idx (`created`), 
	INDEX i_combo_1_idx (severity_level, context)
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Represents a single error stack trace line.
-- 		A stack trace indicates how an error propagated through the program flow before it was recorded.
-- file_name:   Name of the file in which this event was recorded
-- class_name:  Name of the class in which this event was recorded. 
-- 		Empty if the class name is identical with the file name.
-- method_name: Name of the method where this event was recorded. Empty if unknown.
-- line_number: The code line number where this event was recorded. None if not available.
-- cause_id:    Id of the stack trace element that originated this element. I.e. the element directly before this element. 
-- 		None if this is the root element.
CREATE TABLE `stack_trace_element_record`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`file_name` VARCHAR(48) NOT NULL, 
	`class_name` VARCHAR(48), 
	`method_name` VARCHAR(48), 
	`line_number` INT, 
	`cause_id` INT, 
	INDEX ster_combo_1_idx (file_name, class_name, method_name, line_number), 
	CONSTRAINT ster_ster_cause_ref_fk FOREIGN KEY ster_ster_cause_ref_idx (cause_id) REFERENCES `stack_trace_element_record`(`id`) ON DELETE SET NULL
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Represents a single error or exception thrown during program runtime
-- exception_type: The name of this exception type. Typically the exception class name.
-- stack_trace_id: Id of the topmost stack trace element that corresponds to this error record
-- cause_id:       Id of the underlying error that caused this error/failure. None if this error represents the root problem.
CREATE TABLE `error_record`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`exception_type` VARCHAR(32) NOT NULL, 
	`stack_trace_id` INT NOT NULL, 
	`cause_id` INT, 
	INDEX er_combo_1_idx (exception_type, stack_trace_id), 
	CONSTRAINT er_ster_stack_trace_ref_fk FOREIGN KEY er_ster_stack_trace_ref_idx (stack_trace_id) REFERENCES `stack_trace_element_record`(`id`) ON DELETE CASCADE, 
	CONSTRAINT er_er_cause_ref_fk FOREIGN KEY er_er_cause_ref_idx (cause_id) REFERENCES `error_record`(`id`) ON DELETE SET NULL
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Represents a specific setting where a problem or an issue occurred
-- issue_id: Id of the issue that occurred
-- version:  The program version in which this issue (variant) occurred
-- error_id: Id of the error / exception that is associated with this issue (variant). None if not applicable.
-- details:  Details about this case and/or setting.
-- created:  Time when this case or variant was first encountered
CREATE TABLE `issue_variant`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`issue_id` INT NOT NULL, 
	`version` VARCHAR(255), 
	`error_id` INT, 
	`details` VARCHAR(128), 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	INDEX iv_created_idx (`created`), 
	INDEX iv_combo_1_idx (issue_id, version, error_id), 
	CONSTRAINT iv_i_issue_ref_fk FOREIGN KEY iv_i_issue_ref_idx (issue_id) REFERENCES `issue`(`id`) ON DELETE CASCADE, 
	CONSTRAINT iv_er_error_ref_fk FOREIGN KEY iv_er_error_ref_idx (error_id) REFERENCES `error_record`(`id`) ON DELETE SET NULL
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Represents one or more specific occurrences of a recorded issue
-- case_id:          Id of the issue variant that occurred
-- error_messages:   Error messages listed in the stack trace. 
-- 		If multiple occurrences are represented, contains data from the latest occurrence.
-- details:          Additional details concerning these issue occurrences.
-- 		In case of multiple occurrences, contains only the latest entry for each detail.
-- count:            Number of issue occurrences represented by this entry
-- occurrence period (first_occurrence, last_occurrence): The first and last time this set of issues occurred
CREATE TABLE `issue_occurrence`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`case_id` INT NOT NULL, 
	`error_messages` VARCHAR(255), 
	`details` VARCHAR(128), 
	`count` INT NOT NULL DEFAULT 1, 
	`first_occurrence` DATETIME NOT NULL, 
	`last_occurrence` DATETIME NOT NULL, 
	INDEX io_combo_1_idx (last_occurrence, first_occurrence), 
	CONSTRAINT io_iv_case_ref_fk FOREIGN KEY io_iv_case_ref_idx (case_id) REFERENCES `issue_variant`(`id`) ON DELETE CASCADE
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;


--	Management	----------

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

