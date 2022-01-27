--
-- Database structure for Exodus models
-- Version: v3.1
-- Type: Update
-- Origin: v3.0
-- Last generated: 2021-11-24
--

-- Used for creating a temporary and limited session based on an authenticated email validation attempt
-- validation_id: Reference to the email validation used as the basis for this session
-- token:         Token used to authenticate against this session
-- expires:       Time when this EmailValidatedSession expires / becomes invalid
-- created:       Time when this EmailValidatedSession was first created
-- closed_after:  Time after which this session was manually closed
CREATE TABLE `email_validated_session`(
	id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
	`validation_id` INT NOT NULL,
	`token` VARCHAR(48) NOT NULL,
	`expires` DATETIME NOT NULL,
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	`closed_after` DATETIME,
	INDEX evs_token_idx (`token`),
	INDEX evs_expires_idx (`expires`),
	INDEX evs_created_idx (`created`),
	INDEX evs_closed_after_idx (`closed_after`),
	CONSTRAINT evs_eva_validation_ref_fk FOREIGN KEY evs_eva_validation_ref_idx (validation_id) REFERENCES `email_validation_attempt`(id) ON DELETE CASCADE
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- New email validation purpose added
INSERT INTO email_validation_purpose(id, name_en) VALUES (4, 'Invitation');