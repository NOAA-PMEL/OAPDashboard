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

 Date: 03/19/2021 12:06:23 PM
*/

SET NAMES utf8;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `submission_status`
-- ----------------------------
DROP TABLE IF EXISTS `submission_status`;
CREATE TABLE `submission_status` (
  `db_id` int(20) unsigned NOT NULL AUTO_INCREMENT,
  `submission_id` int(20) unsigned NOT NULL,
  `status_time` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `status` varchar(128) NOT NULL DEFAULT 'INITIAL',
  `message` text,
  PRIMARY KEY (`db_id`),
  KEY `submitter_id` (`submission_id`) USING BTREE,
  CONSTRAINT `fk_status_record` FOREIGN KEY (`submission_id`) REFERENCES `archive_submissions` (`db_id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=54 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;

-- ----------------------------
--  Records of `submission_status`
-- ----------------------------
BEGIN;
INSERT INTO `submission_status` VALUES ('1', '1', '2020-07-06 10:36:59', 'INITIAL', 'Archive submission initiated.'), ('2', '1', '2020-07-06 10:37:00', 'STAGED', 'Submission package staged for pickup at:BEPK0F4V6/1/BEPK0F4V6_bagit.zip'), ('3', '2', '2020-07-06 11:35:47', 'INITIAL', 'Archive submission initiated.'), ('4', '2', '2020-07-06 11:35:48', 'STAGED', 'Submission package staged for pickup at:BEPK0F4V6/2/BEPK0F4V6_bagit.zip'), ('5', '3', '2020-07-07 13:11:14', 'INITIAL', 'Archive submission initiated.'), ('6', '3', '2020-07-07 13:11:14', 'STAGED', 'Submission package staged for pickup at:BEPK0F4V6/2/BEPK0F4V6_bagit.zip'), ('7', '4', '2020-07-07 13:47:17', 'INITIAL', 'Archive submission initiated.'), ('8', '4', '2020-07-07 13:47:17', 'STAGED', 'Submission package staged for pickup at:null'), ('9', '5', '2020-07-19 15:20:45', 'INITIAL', 'Archive submission initiated.'), ('10', '5', '2020-07-19 15:21:51', 'STAGED', 'Submission package staged for pickup at:null'), ('11', '6', '2020-07-19 15:30:40', 'INITIAL', 'Archive submission initiated.'), ('12', '6', '2020-07-19 15:31:48', 'STAGED', 'Submission package staged for pickup at:Archive Protocol set to NONE.  Package not staged.'), ('13', '7', '2020-07-21 14:40:52', 'INITIAL', 'Archive submission initiated.'), ('14', '7', '2020-07-21 14:40:52', 'STAGED', 'Submission package staged for pickup at:Archive mode set to NONE.  Package not staged.'), ('15', '8', '2020-07-21 14:50:22', 'INITIAL', 'Archive submission initiated.'), ('16', '8', '2020-07-21 14:53:51', 'STAGED', 'Submission package staged for pickup at:Archive mode set to NONE.  Package not staged.'), ('17', '9', '2020-07-21 14:54:43', 'INITIAL', 'Archive submission initiated.'), ('18', '9', '2020-07-21 14:54:43', 'STAGED', 'Submission package staged for pickup at:Archive mode set to NONE.  Package not staged.'), ('19', '10', '2020-07-21 14:58:11', 'INITIAL', 'Archive submission initiated.'), ('20', '10', '2020-07-21 14:58:11', 'STAGED', 'Submission package staged for pickup at:BEKV0XSC6/4/BEKV0XSC6_bagit.zip'), ('21', '11', '2020-07-21 15:02:27', 'INITIAL', 'Archive submission initiated.'), ('22', '11', '2020-07-21 15:02:27', 'STAGED', 'Submission package staged for pickup at:Archive mode set to NONE.  Package not staged.'), ('23', '12', '2020-07-21 15:12:54', 'INITIAL', 'Archive submission initiated.'), ('24', '12', '2020-07-21 15:12:54', 'STAGED', 'Submission package staged for pickup at:Archive mode set to NONE.  Package not staged.'), ('25', '13', '2020-07-21 15:42:00', 'INITIAL', 'Archive submission initiated.'), ('26', '13', '2020-07-21 15:42:00', 'STAGED', 'Submission package staged for pickup at:Archive mode set to NONE.  Package not staged.'), ('27', '14', '2020-07-22 07:11:52', 'INITIAL', 'Archive submission initiated.'), ('28', '14', '2020-07-22 07:11:52', 'STAGED', 'Submission package staged for pickup at:Archive mode set to NONE.  Package not staged.'), ('29', '15', '2020-08-29 12:01:28', 'INITIAL', 'Archive submission initiated.'), ('30', '15', '2020-08-29 12:01:28', 'STAGED', 'Submission package staged for pickup at:Archive mode set to NONE.  Package not staged.'), ('31', '16', '2020-11-06 09:28:10', 'INITIAL', 'Archive submission initiated.'), ('32', '16', '2020-11-06 09:28:11', 'STAGED', 'Submission package staged for pickup at:BEYUHBDDY/1/BEYUHBDDY_bagit.zip'), ('33', '17', '2020-12-16 11:04:01', 'INITIAL', 'Archive submission initiated.'), ('34', '17', '2020-12-16 11:04:37', 'STAGED', 'Submission package staged for pickup at:Archive mode set to NONE.  Package not staged.'), ('35', '18', '2021-01-05 09:41:22', 'INITIAL', 'Archive submission initiated.'), ('36', '18', '2021-01-05 09:41:22', 'STAGED', 'Submission package staged for pickup at:Archive mode set to NONE.  Package not staged.'), ('37', '19', '2021-01-15 11:07:54', 'INITIAL', 'Archive submission initiated.'), ('38', '19', '2021-01-15 11:07:54', 'STAGED', 'Submission package staged for pickup at:Archive mode set to NONE.  Package not staged.'), ('39', '20', '2021-01-15 16:59:49', 'INITIAL', 'Archive submission initiated.'), ('40', '20', '2021-01-15 16:59:49', 'STAGED', 'Submission package staged for pickup at:Archive mode set to NONE.  Package not staged.'), ('41', '21', '2021-01-15 17:11:24', 'INITIAL', 'Archive submission initiated.'), ('42', '21', '2021-01-15 17:11:24', 'STAGED', 'Submission package staged for pickup at:Archive mode set to NONE.  Package not staged.'), ('43', '22', '2021-01-19 17:54:42', 'INITIAL', 'Archive submission initiated.'), ('44', '22', '2021-01-19 17:54:42', 'STAGED', 'Submission package staged for pickup at:Archive mode set to NONE.  Package not staged.'), ('46', '16', '2021-01-29 12:36:47', 'RECEIVED', 'Package received by NCEI'), ('49', '16', '2021-01-29 14:52:05', 'ACCEPTED', 'Accepted by archive'), ('50', '23', '2021-02-23 07:33:22', 'INITIAL', 'Archive submission initiated.'), ('51', '23', '2021-02-23 07:33:22', 'STAGED', 'Submission package staged for pickup at:Archive mode set to NONE.  Package not staged.'), ('52', '24', '2021-02-23 08:05:57', 'INITIAL', 'Archive submission initiated.'), ('53', '24', '2021-02-23 08:05:57', 'STAGED', 'Submission package staged for pickup at:Archive mode set to NONE.  Package not staged.');
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
