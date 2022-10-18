--
-- Used for testing TIMESTAMP and DATETIME date types in Mysql context
--

CREATE TABLE datetime_test
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `timestamp` TIMESTAMP NOT NULL,
    `datetime` DATETIME NOT NULL,

    INDEX (`timestamp`),
    INDEX (`datetime`)

)Engine=InnoDB DEFAULT CHARSET=latin1;