--
-- More Test tables for Vault
-- It is expected that the first sql has been run
--

USE vault_test;

CREATE TABLE strength
(
	row_id int not null primary key AUTO_INCREMENT, 
	owner_id int not null, 
	name varchar(255) not null, 
	power_level int, 
	
	FOREIGN KEY (owner_id) REFERENCES person(row_id) ON DELETE CASCADE
	
)Engine=InnoDB;