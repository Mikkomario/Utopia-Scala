--
-- Updates Logos DB structure from v0.6 to v0.7
-- Remember to run the migration app after performing this update
--

ALTER TABLE `domain` ADD `is_https` BOOLEAN;