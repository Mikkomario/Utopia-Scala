-- 
-- Initial database inserts for Exodus
-- Version: v4.0
-- Last generated: 2022-02-19
--

--	Auth	----------

-- Inserts 9 scopes
INSERT INTO `scope` (`id`, `name`) VALUES
	(1, 'read-general-data'),
	(2, 'create-user'),
	(3, 'read-personal-data'),
	(4, 'personal-actions'),
	(5, 'read-organization-data'),
	(6, 'organization-actions'),
	(7, 'reset-password'),
	(8, 'change-email'),
	(9, 'delete-account');

-- Inserts 5 token types
INSERT INTO `token_type` (`id`, `name`) VALUES
	(1, 'api-key');
INSERT INTO `token_type` (`id`, `duration`, `name`) VALUES
	(2, 1320, 'session-token'),
	(4, 15, 'email-validated-session');
INSERT INTO `token_type` (`id`, `name`, `refreshed_type_id`) VALUES
	(3, 'refresh-token', 2);
INSERT INTO `token_type` (`id`, `duration`, `is_single_use_only`, `name`, `refreshed_type_id`) VALUES
	(5, 4320, TRUE, 'email-validation-token', 4);