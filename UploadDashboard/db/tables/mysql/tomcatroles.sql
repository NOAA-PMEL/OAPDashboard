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

 Date: 03/19/2021 12:06:41 PM
*/

SET NAMES utf8;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `tomcatroles`
-- ----------------------------
DROP TABLE IF EXISTS `tomcatroles`;
CREATE TABLE `tomcatroles` (
  `db_id` int(20) unsigned NOT NULL AUTO_INCREMENT,
  `user_dbid` int(20) unsigned NOT NULL,
  `username` varchar(64) NOT NULL,
  `role_dbid` int(20) unsigned NOT NULL,
  `userrole` varchar(64) NOT NULL,
  PRIMARY KEY (`db_id`),
  KEY `user_dbid` (`user_dbid`),
  KEY `role_dbid` (`role_dbid`),
  CONSTRAINT `fk_tomcat_role_user` FOREIGN KEY (`user_dbid`) REFERENCES `tomcatusers` (`db_id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=54 DEFAULT CHARSET=utf8;

-- ----------------------------
--  Records of `tomcatroles`
-- ----------------------------
BEGIN;
INSERT INTO `tomcatroles` VALUES ('21', '24', 'linus', '8', 'oapdashboarduser'), ('22', '25', 'devmode', '8', 'oapdashboarduser'), ('28', '26', 'johnny', '8', 'oapdashboarduser'), ('32', '24', 'linus', '10', 'dashboardadmin'), ('33', '26', 'johnny', '10', 'dashboardadmin'), ('48', '43', 'kamb', '8', 'oapdashboarduser'), ('53', '49', 'pmcelhany', '8', 'oapdashboarduser');
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
