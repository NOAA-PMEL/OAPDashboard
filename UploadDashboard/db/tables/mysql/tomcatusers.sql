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

 Date: 03/19/2021 12:06:48 PM
*/

SET NAMES utf8;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `tomcatusers`
-- ----------------------------
DROP TABLE IF EXISTS `tomcatusers`;
CREATE TABLE `tomcatusers` (
  `db_id` int(20) unsigned NOT NULL AUTO_INCREMENT,
  `username` varchar(64) NOT NULL,
  `password` varchar(1024) NOT NULL,
  PRIMARY KEY (`db_id`),
  CONSTRAINT `fk_tomcat_user_user` FOREIGN KEY (`db_id`) REFERENCES `users` (`db_id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=50 DEFAULT CHARSET=utf8;

-- ----------------------------
--  Records of `tomcatusers`
-- ----------------------------
BEGIN;
INSERT INTO `tomcatusers` VALUES ('24', 'linus', 'drowssap'), ('25', 'devmode', ''), ('26', 'johnny', 'drowssap'), ('43', 'kamb', 'drowssap'), ('49', 'pmcelhany', '429e0f4da67b9b53dc846eb9c10d447f$100000$df016b511daf4a39a47c5408212f8112ed15778871264b4e3f6697c45ca7a23e');
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
