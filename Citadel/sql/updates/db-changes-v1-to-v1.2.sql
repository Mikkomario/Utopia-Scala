--
-- Database changes for those updating their v1.0 Citadel version to v1.2 version
--

-- User email address is no longer required
-- User name is now indexed
ALTER TABLE user_settings
    CHANGE email email VARCHAR(128),
    ADD INDEX us_user_name_idx (name);