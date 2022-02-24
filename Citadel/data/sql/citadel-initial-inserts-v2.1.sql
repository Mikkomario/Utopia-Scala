-- 
-- Initial database inserts for Citadel
-- Version: v2.1
-- Last generated: 2022-02-24
--

--	Description	----------

-- Inserts 1 description role
INSERT INTO `description_role` (`id`, `json_key_plural`, `json_key_singular`) VALUES 
	(1, 'names', 'name');


--	Language	----------

-- Inserts 1 language
INSERT INTO `language` (`id`, `iso_code`) VALUES 
	(1, 'en');

-- Inserts 4 language familiarities
INSERT INTO `language_familiarity` (`id`, `order_index`) VALUES 
	(1, 1), 
	(2, 5), 
	(3, 10), 
	(4, 15);


--	Organization	----------

-- Inserts 6 tasks
INSERT INTO `task` (`id`) VALUES 
	(1), 
	(2), 
	(3), 
	(4), 
	(5), 
	(6);

-- Inserts 2 user roles
INSERT INTO `user_role` (`id`) VALUES 
	(1), 
	(2);

-- Inserts 11 user role rights
INSERT INTO `user_role_right` (`role_id`, `task_id`) VALUES 
	(1, 1), 
	(1, 2), 
	(1, 3), 
	(1, 4), 
	(1, 5), 
	(1, 6), 
	(2, 2), 
	(2, 3), 
	(2, 4), 
	(2, 5), 
	(2, 6);

