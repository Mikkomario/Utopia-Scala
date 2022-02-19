-- 
-- Initial database inserts for Exodus
-- Version: v4.0
-- Last generated: 2022-02-18
--


--	Auth	----------

-- Inserts 9 scopes
INSERT INTO `scope` (id, name) VALUES 
	(1, 'General Data Read'), 
	(2, 'User Creation'), 
	(3, 'Personal Data Read'), 
	(4, 'Personal Actions'), 
	(5, 'Organization Data Read'), 
	(6, 'Organization Actions'), 
	(7, 'Password Reset'), 
	(8, 'Email Change'), 
	(9, 'Account Deletion');

-- Inserts 4 token types
INSERT INTO `token_type` (id, name) VALUES 
	(1, 'Api Key'), 
	(2, 'Refresh Token'), 
	(3, 'Session Token'), 
	(4, 'Email Validation Token'),
	(5, 'Email-Validated Session');

