-- 
-- Initial database inserts for Exodus
-- Version: v4.0
-- Last generated: 2022-02-19
--

--	Auth	----------

-- Inserts 4 email validation purposes
INSERT INTO `email_validation_purpose` (`id`, `name`) VALUES
	(1, 'user-creation'),
	(2, 'email-change'),
	(3, 'password-reset'),
	(4, 'organization-invitation');

-- Inserts 15 scopes
INSERT INTO `scope` (`id`, `name`) VALUES
	(1, 'read-general-data'),
	(2, 'create-user'),
	(3, 'read-personal-data'),
	(4, 'personal-actions'),
	(5, 'join-organization'),
	(6, 'create-organization'),
	(7, 'read-organization-data'),
	(8, 'organization-actions'),
	(9, 'request-password-reset'),
	(10, 'change-known-password'),
	(11, 'replace-forgotten-password'),
	(12, 'change-email'),
	(13, 'terminate-other-sessions'),
	(14, 'revoke-other-tokens'),
	(15, 'delete-account');

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