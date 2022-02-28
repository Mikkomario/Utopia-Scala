--
-- A few test tables for VAULT 0.1
--

CREATE DATABASE vault_test;
USE vault_test;

-- A table for testing different data type uses
CREATE TABLE person
(
	row_id int not null primary key AUTO_INCREMENT, 
	name varchar(32) not null, 
	age int, 
	is_admin boolean default false, 
	created timestamp not null default CURRENT_TIMESTAMP

)Engine=InnoDB;