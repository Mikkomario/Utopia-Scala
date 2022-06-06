--
-- Additional Test data
--

USE vault_test;

-- A table with non-auto-increment primary key
CREATE TABLE index_test
(
    id INT NOT NULL PRIMARY KEY,
    text VARCHAR(128)

)Engine=InnoDB;