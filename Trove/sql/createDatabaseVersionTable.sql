-- Creates a table that records database version updates
CREATE TABLE database_version
(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    version VARCHAR(16) NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX dv_version_idx (version),
    INDEX dv_creation_idx (created)

)Engine=InnoDB DEFAULT CHARSET=latin1;