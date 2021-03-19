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

 Date: 03/19/2021 12:06:33 PM
*/

SET NAMES utf8;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `roles`
-- ----------------------------
DROP TABLE IF EXISTS `roles`;
CREATE TABLE `roles` (
  `db_id` int(20) unsigned NOT NULL AUTO_INCREMENT,
  `role` varchar(128) CHARACTER SET latin1 NOT NULL,
  `description` varchar(256) CHARACTER SET latin1 DEFAULT NULL,
  PRIMARY KEY (`db_id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8;

-- ----------------------------
--  Records of `roles`
-- ----------------------------
BEGIN;
INSERT INTO `roles` VALUES ('7', 'TomcatMgr', 'Tomcat manager webapp user role.'), ('8', 'oapdashboarduser', 'OAP Dashboard user role.'), ('10', 'dashboardadmin', 'OAP Dashboard Administrator role.'), ('11', 'groupadmin', 'Dashboard Group Administrator.');
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
