/*
 Navicat Premium Data Transfer

 Source Server         : local Mysql 3306
 Source Server Type    : MySQL
 Source Server Version : 50721
 Source Host           : localhost
 Source Database       : oapdashboard

 Target Server Type    : MySQL
 Target Server Version : 50721
 File Encoding         : utf-8

 Date: 10/28/2019 16:01:30 PM
*/

SET NAMES utf8;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `addresses`
-- ----------------------------
DROP TABLE IF EXISTS `addresses`;
CREATE TABLE `addresses` (
  `db_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `supplemental_to` varchar(256) DEFAULT NULL,
  `address1` varchar(256) DEFAULT NULL,
  `address2` varchar(256) DEFAULT NULL,
  `city` varchar(256) DEFAULT NULL,
  `state_or_area` varchar(128) DEFAULT NULL,
  `postal_code` varchar(16) DEFAULT NULL,
  `country` varchar(128) DEFAULT NULL,
  PRIMARY KEY (`db_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `archive_submissions`
-- ----------------------------
DROP TABLE IF EXISTS `archive_submissions`;
CREATE TABLE `archive_submissions` (
  `db_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `submit_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `submitter_id` bigint(20) DEFAULT NULL,
  `dataset_id` varchar(128) NOT NULL,
  `version` int(11) NOT NULL DEFAULT '1',
  `submission_key` varchar(36) NOT NULL,
  `package_location` varchar(255) DEFAULT NULL,
  `submit_msg` text,
  PRIMARY KEY (`db_id`),
  KEY `submitter_id` (`submitter_id`),
  CONSTRAINT `fk_submission_submitter` FOREIGN KEY (`submitter_id`) REFERENCES `users` (`db_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=34 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;
##
## Originally using mysql builtin uuid() generator as it generated time-based UUIDs.
## No longer using UUIDs. for now.  LK, 20191028
#
# delimiter ;;
# CREATE TRIGGER `before_insert_submissions` BEFORE INSERT ON `archive_submissions` FOR EACH ROW BEGIN
#  IF new.submission_key IS NULL THEN
#    SET new.submission_key = uuid();
#  END IF;
#END;
# ;;
#delimiter ;

-- ----------------------------
--  Table structure for `org_addr`
-- ----------------------------
DROP TABLE IF EXISTS `org_addr`;
CREATE TABLE `org_addr` (
  `org_id` bigint(20) NOT NULL,
  `addr_id` bigint(20) NOT NULL,
  `addr_type` varchar(32) DEFAULT NULL,
  `addr_label` varchar(132) DEFAULT NULL,
  KEY `org_id` (`org_id`),
  KEY `addr_id` (`addr_id`),
  CONSTRAINT `addr_org_fk` FOREIGN KEY (`org_id`) REFERENCES `organizations` (`db_id`) ON DELETE CASCADE ON UPDATE NO ACTION,
  CONSTRAINT `org_addr_fk` FOREIGN KEY (`addr_id`) REFERENCES `addresses` (`db_id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `organizations`
-- ----------------------------
DROP TABLE IF EXISTS `organizations`;
CREATE TABLE `organizations` (
  `db_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `mod_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `abbreviation` varchar(64) DEFAULT NULL,
  `short_name` varchar(128) DEFAULT NULL,
  `full_name` varchar(512) NOT NULL,
  `parent_org` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`db_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `roles`
-- ----------------------------
DROP TABLE IF EXISTS `roles`;
CREATE TABLE `roles` (
  `db_id` int(4) unsigned NOT NULL AUTO_INCREMENT,
  `role` varchar(128) CHARACTER SET latin1 NOT NULL,
  `description` varchar(256) CHARACTER SET latin1 DEFAULT NULL,
  PRIMARY KEY (`db_id`)
) ENGINE=MyISAM AUTO_INCREMENT=10 DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `submission_status`
-- ----------------------------
DROP TABLE IF EXISTS `submission_status`;
CREATE TABLE `submission_status` (
  `db_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `submission_id` bigint(20) NOT NULL,
  `status_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `status` varchar(128) NOT NULL DEFAULT 'INITIAL',
  `message` text,
  PRIMARY KEY (`db_id`),
  KEY `submitter_id` (`submission_id`) USING BTREE,
  CONSTRAINT `fk_status_submission` FOREIGN KEY (`submission_id`) REFERENCES `archive_submissions` (`db_id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;

-- ----------------------------
--  Table structure for `tomcatroles`
-- ----------------------------
DROP TABLE IF EXISTS `tomcatroles`;
CREATE TABLE `tomcatroles` (
  `db_id` int(4) unsigned NOT NULL AUTO_INCREMENT,
  `user_dbid` int(4) NOT NULL,
  `username` varchar(64) CHARACTER SET latin1 NOT NULL,
  `role_dbid` int(4) NOT NULL,
  `userrole` varchar(64) CHARACTER SET latin1 NOT NULL,
  PRIMARY KEY (`db_id`),
  KEY `user_dbid` (`user_dbid`),
  KEY `role_dbid` (`role_dbid`)
) ENGINE=MyISAM AUTO_INCREMENT=24 DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `tomcatusers`
-- ----------------------------
DROP TABLE IF EXISTS `tomcatusers`;
CREATE TABLE `tomcatusers` (
  `db_id` int(4) unsigned NOT NULL AUTO_INCREMENT,
  `username` varchar(64) NOT NULL,
  `password` varchar(1024) NOT NULL,
  PRIMARY KEY (`db_id`)
) ENGINE=MyISAM AUTO_INCREMENT=27 DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `user_addr`
-- ----------------------------
DROP TABLE IF EXISTS `user_addr`;
CREATE TABLE `user_addr` (
  `user_id` bigint(20) NOT NULL,
  `addr_id` bigint(20) NOT NULL,
  `addr_type` varchar(32) DEFAULT 'POST',
  `addr_label` varchar(128) DEFAULT NULL,
  KEY `user_id` (`user_id`),
  KEY `addr_id` (`addr_id`),
  CONSTRAINT `addr_user_fk` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`db_id`) ON DELETE CASCADE ON UPDATE NO ACTION,
  CONSTRAINT `user_addr_fk` FOREIGN KEY (`addr_id`) REFERENCES `addresses` (`db_id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `user_info`
-- ----------------------------
DROP TABLE IF EXISTS `user_info`;
CREATE TABLE `user_info` (
  `db_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `mod_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `username` varchar(128) NOT NULL,
  `email` varchar(256) DEFAULT NULL,
  `first_name` varchar(128) NOT NULL,
  `middle` varchar(128) DEFAULT NULL,
  `last_name` varchar(128) NOT NULL,
  PRIMARY KEY (`db_id`),
  UNIQUE KEY `unq_username` (`username`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `user_org`
-- ----------------------------
DROP TABLE IF EXISTS `user_org`;
CREATE TABLE `user_org` (
  `user_id` bigint(20) NOT NULL,
  `org_id` bigint(20) NOT NULL,
  `relation` varchar(128) DEFAULT NULL,
  `department` varchar(128) DEFAULT NULL,
  KEY `user_id` (`user_id`),
  KEY `org_id` (`org_id`),
  CONSTRAINT `org_user_fk` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`db_id`) ON DELETE CASCADE ON UPDATE NO ACTION,
  CONSTRAINT `user_org_fk` FOREIGN KEY (`org_id`) REFERENCES `organizations` (`db_id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `users`
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `db_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `modified_time` timestamp NOT NULL DEFAULT '1970-01-01 00:00:00',
  `username` varchar(128) CHARACTER SET latin1 NOT NULL,
  `last_login` timestamp NULL DEFAULT NULL,
  `first_name` varchar(128) CHARACTER SET latin1 NOT NULL,
  `last_name` varchar(128) CHARACTER SET latin1 NOT NULL,
  `email` varchar(128) CHARACTER SET latin1 NOT NULL,
  PRIMARY KEY (`db_id`),
  KEY `idx_users_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8;

-- ----------------------------
--  View structure for `email_auth`
-- ----------------------------
DROP VIEW IF EXISTS `email_auth`;
CREATE ALGORITHM=UNDEFINED DEFINER=`oapadmin`@`localhost` SQL SECURITY INVOKER VIEW `email_auth` AS select `u`.`email` AS `username`,`ua`.`password` AS `password` from (`users` `u` join `tomcatusers` `ua`) where (`u`.`db_id` = `ua`.`db_id`);

-- ----------------------------
--  View structure for `uid_auth`
-- ----------------------------
DROP VIEW IF EXISTS `uid_auth`;
CREATE ALGORITHM=UNDEFINED DEFINER=`oapadmin`@`localhost` SQL SECURITY INVOKER VIEW `uid_auth` AS select `u`.`username` AS `username`,`ua`.`password` AS `password` from (`users` `u` join `tomcatusers` `ua`) where (`u`.`db_id` = `ua`.`db_id`);

-- ----------------------------
--  Function structure for `UuidFromBin`
-- ----------------------------
DROP FUNCTION IF EXISTS `UuidFromBin`;
delimiter ;;
CREATE DEFINER=`root`@`localhost` FUNCTION `UuidFromBin`(_bin BINARY(16)) RETURNS binary(36)
    DETERMINISTIC
    SQL SECURITY INVOKER
RETURN
        LCASE(CONCAT_WS('-',
            HEX(SUBSTR(_bin,  5, 4)),
            HEX(SUBSTR(_bin,  3, 2)),
            HEX(SUBSTR(_bin,  1, 2)),
            HEX(SUBSTR(_bin,  9, 2)),
            HEX(SUBSTR(_bin, 11))
                 ))
 ;;
delimiter ;

-- ----------------------------
--  Function structure for `UuidToBin`
-- ----------------------------
DROP FUNCTION IF EXISTS `UuidToBin`;
delimiter ;;
CREATE DEFINER=`root`@`localhost` FUNCTION `UuidToBin`(_uuid BINARY(36)) RETURNS binary(16)
    DETERMINISTIC
    SQL SECURITY INVOKER
RETURN
        UNHEX(CONCAT(
            SUBSTR(_uuid, 15, 4),
            SUBSTR(_uuid, 10, 4),
            SUBSTR(_uuid,  1, 8),
            SUBSTR(_uuid, 20, 4),
            SUBSTR(_uuid, 25) ))
 ;;
delimiter ;

SET FOREIGN_KEY_CHECKS = 1;
