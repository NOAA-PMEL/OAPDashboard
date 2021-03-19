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

 Date: 03/19/2021 12:07:07 PM
*/

SET NAMES utf8;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `users`
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `db_id` int(20) unsigned NOT NULL AUTO_INCREMENT,
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `modified_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `username` varchar(128) CHARACTER SET latin1 NOT NULL,
  `last_login` timestamp NULL DEFAULT NULL,
  `last_pw_change` timestamp NULL DEFAULT NULL,
  `requires_pw_change` varchar(128) DEFAULT NULL,
  `first_name` varchar(128) CHARACTER SET latin1 NOT NULL,
  `last_name` varchar(128) CHARACTER SET latin1 NOT NULL,
  `middle_name` varchar(128) DEFAULT NULL,
  `email` varchar(128) NOT NULL,
  `telephone` varchar(20) DEFAULT NULL,
  `extension` varchar(8) DEFAULT NULL,
  `organization` varchar(512) DEFAULT NULL,
  PRIMARY KEY (`db_id`),
  UNIQUE KEY `idx_users_username` (`username`) USING BTREE,
  UNIQUE KEY `idx_users_email` (`email`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=50 DEFAULT CHARSET=utf8;

-- ----------------------------
--  Records of `users`
-- ----------------------------
BEGIN;
INSERT INTO `users` VALUES ('24', '2019-09-26 13:16:57', '1970-01-01 00:00:00', 'linus', '2021-03-02 13:34:38', null, null, 'linus', 'linus', null, 'linus.kamb@noaa.gov', null, null, null), ('25', '2019-09-26 13:16:57', '2021-03-03 08:47:49', 'devmode', '2021-03-17 18:47:18', null, null, 'Devonius', 'Munk', 'P', 'linus.kamb@gmail.com', '123.456.7890', '4240', 'allyChallenged'), ('26', '2020-01-15 15:44:57', '2020-01-15 15:44:57', 'johnny', null, null, null, 'Johnoe', 'Testorius', null, 'joe@joe.com', null, null, null), ('43', '2021-01-25 13:40:41', '2021-01-25 13:40:41', 'kamb', null, null, null, 'Linus', 'Kamb', null, 'blah@gmail.com', null, null, 'NOAA/PMEL'), ('49', '2021-03-02 13:58:08', '2021-03-02 13:58:08', 'pmcelhany', '2021-03-02 13:59:01', null, null, 'Paul', 'McElhany', null, 'linuskamb@gmail.com', '206-817-4145', null, 'NOAA Northwest Fisheries Science Center');
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
