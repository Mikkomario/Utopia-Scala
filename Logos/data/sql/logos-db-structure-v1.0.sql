-- 
-- Database structure for logos models
-- Version: v1.0
-- Last generated: 2024-03-20
--

--	Word	----------

-- Represents a character sequence used to separate two statements or parts of a statement
-- text:    The characters that form this delimiter
-- created: Time when this delimiter was added to the database
CREATE TABLE `delimiter`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`text` VARCHAR(2) NOT NULL, 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	INDEX de_text_idx (`text`)
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Represents an individual word used in a text document. Case-sensitive.
-- text:    Text representation of this word
-- created: Time when this word was added to the database
CREATE TABLE `word`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`text` VARCHAR(16) NOT NULL, 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	INDEX w_text_idx (`text`)
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Represents an individual statement made within some text. Consecutive statements form whole texts.
-- delimiter_id: Id of the delimiter that terminates this sentence. None if this sentence is not terminated with any character.
-- created:      Time when this statement was first made
CREATE TABLE `statement`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`delimiter_id` INT, 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	INDEX s_created_idx (`created`), 
	CONSTRAINT s_de_delimiter_ref_fk FOREIGN KEY s_de_delimiter_ref_idx (delimiter_id) REFERENCES `delimiter`(`id`) ON DELETE SET NULL
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Records when a word is used in a statement
-- statement_id: Id of the statement where the referenced word appears
-- word_id:      Id of the word that appears in the described statement
-- order_index:  Index at which the specified word appears within the referenced statement (0-based)
-- style_id:     Style in which this word is used in this context
-- 		References enumeration DisplayStyle
-- 		Possible values are: 1 = default
CREATE TABLE `word_placement`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`statement_id` INT NOT NULL, 
	`word_id` INT NOT NULL, 
	`order_index` TINYINT NOT NULL, 
	`style_id` TINYINT NOT NULL, 
	INDEX wp_order_index_idx (`order_index`), 
	CONSTRAINT wp_s_statement_ref_fk FOREIGN KEY wp_s_statement_ref_idx (statement_id) REFERENCES `statement`(`id`) ON DELETE CASCADE, 
	CONSTRAINT wp_w_word_ref_fk FOREIGN KEY wp_w_word_ref_idx (word_id) REFERENCES `word`(`id`) ON DELETE CASCADE
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;


--	Url	----------

-- Represents the address of an internet service
-- url:     Full http(s) address of this domain in string format. Includes protocol, domain name and possible port number.
-- created: Time when this domain was added to the database
CREATE TABLE `domain`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`url` VARCHAR(12) NOT NULL, 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Represents a specific http(s) request url, not including any query parameters
-- domain_id: Id of the domain part of this url
-- path:      Part of this url that comes after the domain part. Doesn't include any query parameters, nor the initial forward slash.
-- created:   Time when this request path was added to the database
CREATE TABLE `request_path`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`domain_id` INT NOT NULL, 
	`path` VARCHAR(12), 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	CONSTRAINT rp_do_domain_ref_fk FOREIGN KEY rp_do_domain_ref_idx (domain_id) REFERENCES `domain`(`id`) ON DELETE CASCADE
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Represents a link for a specific http(s) request
-- request_path_id:  Id of the targeted internet address, including the specific sub-path
-- query_parameters: Specified request parameters in model format
-- created:          Time when this link was added to the database
CREATE TABLE `link`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`request_path_id` INT NOT NULL, 
	`query_parameters` VARCHAR(255), 
	`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
	CONSTRAINT l_rp_request_path_ref_fk FOREIGN KEY l_rp_request_path_ref_idx (request_path_id) REFERENCES `request_path`(`id`) ON DELETE CASCADE
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

-- Places a link within a statement
-- statement_id: Id of the statement where the specified link is referenced
-- link_id:      Referenced link
-- order_index:  Index where the link appears in the statement (0-based)
CREATE TABLE `link_placement`(
	`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT, 
	`statement_id` INT NOT NULL, 
	`link_id` INT NOT NULL, 
	`order_index` TINYINT NOT NULL, 
	CONSTRAINT lp_s_statement_ref_fk FOREIGN KEY lp_s_statement_ref_idx (statement_id) REFERENCES `statement`(`id`) ON DELETE CASCADE, 
	CONSTRAINT lp_l_link_ref_fk FOREIGN KEY lp_l_link_ref_idx (link_id) REFERENCES `link`(`id`) ON DELETE CASCADE
)Engine=InnoDB DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

