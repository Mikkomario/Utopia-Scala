-- 
-- Database structure for scribe models
-- Version: v0.1
-- Last generated: 2023-05-22
--

--	Logging	----------

-- Represents a type of problem or issue that may occur during a program's run
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
-- class_name:  The class where this event was recorded.
-- method_name: The name of the class method where this event was recorded
-- line_number: The code line number where this event was recorded
-- cause_id:    Id of the stack trace element that originated this element. I.e. the element directly before this element. None if this is the root element.
CREATE TABLE `stack_trace_element`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`class_name` VARCHAR(48) NOT NULL, 
	`method_name` VARCHAR(48) NOT NULL, 
	`line_number` INT NOT NULL, 
	`cause_id` INT, 
	INDEX ste_combo_1_idx (class_name, method_name, line_number), 
	CONSTRAINT ste_ste_cause_ref_fk FOREIGN KEY ste_ste_cause_ref_idx (cause_id) REFERENCES `stack_trace_element`(`id`) ON DELETE SET NULL
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
	CONSTRAINT er_ste_stack_trace_ref_fk FOREIGN KEY er_ste_stack_trace_ref_idx (stack_trace_id) REFERENCES `stack_trace_element`(`id`) ON DELETE CASCADE, 
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
	`version` VARCHAR(48) NOT NULL, 
	`error_id` INT, 
	`details` VARCHAR(128), 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	INDEX iv_created_idx (`created`), 
	INDEX iv_combo_1_idx (issue_id, version, error_id), 
	CONSTRAINT iv_i_issue_ref_fk FOREIGN KEY iv_i_issue_ref_idx (issue_id) REFERENCES `issue`(`id`) ON DELETE CASCADE, 
	CONSTRAINT iv_er_error_ref_fk FOREIGN KEY iv_er_error_ref_idx (error_id) REFERENCES `error_record`(`id`) ON DELETE SET NULL
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Represents a specific occurrence of a recorded issue
-- case_id:        Id of the issue variant that occurred
-- error_messages: Error messages listed in the stack trace
-- created:        Time when the issue occurred or was recorded
CREATE TABLE `issue_occurrence`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`case_id` INT NOT NULL, 
	`error_messages` VARCHAR(255), 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	INDEX io_created_idx (`created`), 
	CONSTRAINT io_iv_case_ref_fk FOREIGN KEY io_iv_case_ref_idx (case_id) REFERENCES `issue_variant`(`id`) ON DELETE CASCADE
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

