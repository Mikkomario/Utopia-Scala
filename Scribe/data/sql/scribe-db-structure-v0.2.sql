-- 
-- Database structure for Scribe models
-- Version: v0.2
-- Last generated: 2021-12-12
--

-- Represents a type of problem that may occur during a program's run
-- context:   Program context where this problem occurred or was logged. Should be unique.
-- severity:  Severity of this problem
-- created:   Time when this problem first occurred
CREATE TABLE `problem`(
	id INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`context` VARCHAR(96) NOT NULL, 
	`severity` INT NOT NULL, 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	INDEX p_created_idx (`created`), 
	INDEX p_combo_1_idx (severity, context)
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Represents a field that specifies some program functionality
-- category:     Name of the broader category where this field belongs
-- created:      Time when this field was introduced
CREATE TABLE `setting_field`(
	id INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`category` VARCHAR(64) NOT NULL, 
	`name` VARCHAR(64) NOT NULL, 
	`description` VARCHAR(255), 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	INDEX sf_created_idx (`created`), 
	INDEX sf_combo_1_idx (category, name)
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Represents a specific setting where a problem occurred
-- problem_id: Id of the problem that occurred
-- details:    Details about this problem case, like the error message, for example
-- created:    Time when this case first occurred
CREATE TABLE `problem_case`(
	id INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`problem_id` INT NOT NULL, 
	`details` VARCHAR(320), 
	`stack` VARCHAR(10000), 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	INDEX pc_created_idx (`created`), 
	CONSTRAINT pc_p_problem_ref_fk FOREIGN KEY pc_p_problem_ref_idx (problem_id) REFERENCES `problem`(id) ON DELETE CASCADE
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Represents a single setting value assignment
-- field_id:         Id of the field this value is for
-- value:            Value assigned for this field
-- created:          Time when this value was specified
-- deprecated_after: Time when this value was replaced with another
CREATE TABLE `setting_value`(
	id INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`field_id` INT NOT NULL, 
	`value` VARCHAR(255), 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	`deprecated_after` DATETIME, 
	INDEX sv_created_idx (`created`), 
	INDEX sv_deprecated_after_idx (`deprecated_after`), 
	CONSTRAINT sv_sf_field_ref_fk FOREIGN KEY sv_sf_field_ref_idx (field_id) REFERENCES `setting_field`(id) ON DELETE CASCADE
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Represents a case where a previously occurred problem repeats again
-- case_id:  Id of the problem case that repeated
-- created:  Time when that case repeated itself
CREATE TABLE `problem_repeat`(
	id INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`case_id` INT NOT NULL, 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	INDEX pr_created_idx (`created`), 
	CONSTRAINT pr_pc_case_ref_fk FOREIGN KEY pr_pc_case_ref_idx (case_id) REFERENCES `problem_case`(id) ON DELETE CASCADE
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

