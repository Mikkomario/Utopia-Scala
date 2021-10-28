--
-- Inserts initial data to the database
-- Intended to follow the main database structure document import
-- Should be followed by description import using the Citadel Description Importer
-- Applicable to versions v2.0 and above
--

-- Inserts available languages
-- 1 = English
INSERT INTO `language` (id, iso_code) VALUES (1, 'en');

-- Inserts language familiarities
-- 1 = Primary language
-- 2 = Fluent and preferred
-- 3 = Fluent
-- 4 = OK
-- 5 = OK, less preferred
-- 6 = Knows a little (better than nothing)
INSERT INTO language_familiarity (id, order_index) VALUES
    (1, 1), (2, 2), (3, 3), (4, 4), (5, 5), (6, 6);

-- Inserts description roles
-- 1 = Name
INSERT INTO description_role (id, json_key_singular, json_key_plural) VALUES (1, 'name', 'names');

-- Inserts tasks
-- 1 = Delete organization
-- 2 = Change user roles (to similar or lower)
-- 3 = Invite new users to organization (with similar or lower role)
-- 4 = Edit organization description (including name)
-- 5 = Remove users (of lower role) from the organization
-- 6 = Cancel organization deletion
INSERT INTO task (id) VALUES (1), (2), (3), (4), (5), (6);

-- Inserts user roles
-- 1 = Owner (all rights)
-- 2 = Admin/Steward (all rights except right to delete the organization)
INSERT INTO user_role (id) VALUES (1), (2);

-- Insert user role rights
INSERT INTO user_role_right (role_id, task_id) VALUES
    (1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6),
    (2, 2), (2, 3), (2, 4), (2, 5), (2, 6);