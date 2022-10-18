--
-- Test table for testing length rule application
-- Since 18.10.222, v1.14.1
--

USE vault_test;

CREATE TABLE length_test(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    str VARCHAR(3),
    num INT(1),
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
)Engine=InnoDB;