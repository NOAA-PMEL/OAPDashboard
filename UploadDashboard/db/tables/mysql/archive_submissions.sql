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

 Date: 03/19/2021 15:29:16 PM
*/

SET NAMES utf8;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `archive_submissions`
-- ----------------------------
DROP TABLE IF EXISTS `archive_submissions`;
CREATE TABLE `archive_submissions` (
  `db_id` int(20) unsigned NOT NULL AUTO_INCREMENT,
  `submit_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `submitter_id` int(20) unsigned NOT NULL,
  `dataset_id` varchar(128) NOT NULL,
  `version` int(11) NOT NULL DEFAULT '1',
  `submission_key` varchar(36) NOT NULL,
  `submit_msg` text,
  `archive_bag` text,
  `package_location` text,
  PRIMARY KEY (`db_id`),
  KEY `submitter_id` (`submitter_id`),
  CONSTRAINT `fk_submission_user` FOREIGN KEY (`submitter_id`) REFERENCES `users` (`db_id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;

-- ----------------------------
--  Records of `archive_submissions`
-- ----------------------------
BEGIN;
INSERT INTO `archive_submissions` VALUES ('1', '2020-07-06 10:36:59', '25', 'BEPK0F4V6', '1', 'BEPK0F4V6', '', '/Users/kamb/tomcat/oa/content/OAPUploadDashboard/ArchiveBundles/bags/BEPK0F4V6/BEPK0F4V6_20200706T053659Z/BEPK0F4V6_bagit.zip', 'BEPK0F4V6/1/BEPK0F4V6_bagit.zip'), ('2', '2020-07-06 11:35:47', '25', 'BEPK0F4V6', '2', 'BEPK0F4V6', 'another submit test', '/Users/kamb/tomcat/oa/content/OAPUploadDashboard/ArchiveBundles/bags/BEPK0F4V6/BEPK0F4V6_20200706T063547Z/BEPK0F4V6_bagit.zip', 'BEPK0F4V6/2/BEPK0F4V6_bagit.zip'), ('3', '2020-07-07 13:11:14', '25', 'BEPK0F4V6', '3', 'BEPK0F4V6', 'another submit test', '/Users/kamb/tomcat/oa/content/OAPUploadDashboard/ArchiveBundles/bags/BEPK0F4V6/BEPK0F4V6_20200707T081114Z/BEPK0F4V6_bagit.zip', 'BEPK0F4V6/2/BEPK0F4V6_bagit.zip'), ('4', '2020-07-07 13:47:17', '25', 'BEPNLDB30', '1', 'BEPNLDB30', 'Thies ees a veery long what about comment do orm\nemipsum foo barr areeba whatchangonnadoa boutoit\nmaube later good buddy?\n', '/Users/kamb/tomcat/oa/content/OAPUploadDashboard/ArchiveBundles/bags/BEPNLDB30/BEPNLDB30_20200707T084717Z/BEPNLDB30_bagit.zip', null), ('5', '2020-07-19 15:20:45', '25', 'BEPK93C2C', '1', 'BEPK93C2C', '', '/Users/kamb/tomcat/oa/content/OAPUploadDashboard/ArchiveBundles/bags/BEPK93C2C/BEPK93C2C_20200719T102045Z/BEPK93C2C_bagit.zip', null), ('6', '2020-07-19 15:30:40', '25', 'BEPK93C2C', '2', 'BEPK93C2C', '', '/Users/kamb/tomcat/oa/content/OAPUploadDashboard/ArchiveBundles/bags/BEPK93C2C/BEPK93C2C_20200719T102913Z/BEPK93C2C_bagit.zip', 'Archive Protocol set to NONE.  Package not staged.'), ('7', '2020-07-21 14:40:52', '25', 'BEKV0XSC6', '1', 'BEKV0XSC6', 'Just testing stuff.', '/Users/kamb/tomcat/oa/content/OAPUploadDashboard/ArchiveBundles/bags/BEKV0XSC6/BEKV0XSC6_20200721T094052Z/BEKV0XSC6_bagit.zip', 'Archive mode set to NONE.  Package not staged.'), ('8', '2020-07-21 14:50:22', '25', 'BEKV0XSC6', '2', 'BEKV0XSC6', 'Just testing stuff.', '/Users/kamb/tomcat/oa/content/OAPUploadDashboard/ArchiveBundles/bags/BEKV0XSC6/BEKV0XSC6_20200721T095022Z/BEKV0XSC6_bagit.zip', 'Archive mode set to NONE.  Package not staged.'), ('9', '2020-07-21 14:54:43', '25', 'BEKV0XSC6', '3', 'BEKV0XSC6', 'Just testing stuff.', '/Users/kamb/tomcat/oa/content/OAPUploadDashboard/ArchiveBundles/bags/BEKV0XSC6/BEKV0XSC6_20200721T095443Z/BEKV0XSC6_bagit.zip', 'Archive mode set to NONE.  Package not staged.'), ('10', '2020-07-21 14:58:11', '25', 'BEKV0XSC6', '4', 'BEKV0XSC6', 'Just testing stuff.', '/Users/kamb/tomcat/oa/content/OAPUploadDashboard/ArchiveBundles/bags/BEKV0XSC6/BEKV0XSC6_20200721T095811Z/BEKV0XSC6_bagit.zip', 'BEKV0XSC6/4/BEKV0XSC6_bagit.zip'), ('11', '2020-07-21 15:02:27', '25', 'BEKV1ZP2X', '1', 'BEKV1ZP2X', 'blah blah', '/Users/kamb/tomcat/oa/content/OAPUploadDashboard/ArchiveBundles/bags/BEKV1ZP2X/BEKV1ZP2X_20200721T100227Z/BEKV1ZP2X_bagit.zip', 'Archive mode set to NONE.  Package not staged.'), ('12', '2020-07-21 15:12:54', '25', 'BEPNLDB30', '2', 'BEPNLDB30', 'Thies ees a veery long what about comment do orm\nemipsum foo barr areeba whatchangonnadoa boutoit\nmaube later good buddy?', '/Users/kamb/tomcat/oa/content/OAPUploadDashboard/ArchiveBundles/bags/BEPNLDB30/BEPNLDB30_20200721T101254Z/BEPNLDB30_bagit.zip', 'Archive mode set to NONE.  Package not staged.'), ('13', '2020-07-21 15:42:00', '25', 'BEPNLDB30', '3', 'BEPNLDB30', 'Thies ees a veery long what about comment do orm\nemipsum foo barr areeba whatchangonnadoa boutoit\nmaube later good buddy?', '/Users/kamb/tomcat/oa/content/OAPUploadDashboard/ArchiveBundles/bags/BEPNLDB30/BEPNLDB30_20200721T104200Z/BEPNLDB30_bagit.zip', 'Archive mode set to NONE.  Package not staged.'), ('14', '2020-07-22 07:11:52', '25', 'BERM5GE4N', '1', 'BERM5GE4N', '', '/Users/kamb/tomcat/oa/content/OAPUploadDashboard/ArchiveBundles/bags/BERM5GE4N/BERM5GE4N_20200722T021152Z/BERM5GE4N_bagit.zip', 'Archive mode set to NONE.  Package not staged.'), ('15', '2020-08-29 12:01:28', '25', 'BET44EDXS', '1', 'BET44EDXS', 'Test submission of WCOA11-01-06-2015 data and metadata from Liqing.', '/Users/kamb/tomcat/oa/content/OAPUploadDashboard/ArchiveBundles/bags/BET44EDXS/BET44EDXS_20200829T070128Z/BET44EDXS_bagit.zip', 'Archive mode set to NONE.  Package not staged.'), ('16', '2020-11-06 09:28:10', '25', 'BEYUHBDDY', '1', 'BEYUHBDDY', '', '/Users/kamb/tomcat/oa/content/OAPUploadDashboard/ArchiveBundles/bags/BEYUHBDDY/BEYUHBDDY_20201106T052810Z/BEYUHBDDY_bagit.zip', 'BEYUHBDDY/1/BEYUHBDDY_bagit.zip'), ('17', '2020-12-16 11:04:00', '25', 'BEWBT6T1F', '1', 'BEWBT6T1F', 'Testing submission', '/Users/kamb/tomcat/oa/content/OAPUploadDashboard/ArchiveBundles/bags/BEWBT6T1F/BEWBT6T1F_20201216T065906Z/BEWBT6T1F_bagit.zip', 'Archive mode set to NONE.  Package not staged.'), ('18', '2021-01-05 09:41:22', '25', 'BET44EDXS', '2', 'BET44EDXS', 'Test resubmission of WCOA11-01-06-2015 data and metadata from Liqing.', '/Users/kamb/tomcat/oa/content/OAPUploadDashboard/ArchiveBundles/bags/BET44EDXS/BET44EDXS_20210105T054122Z/BET44EDXS_bagit.zip', 'Archive mode set to NONE.  Package not staged.'), ('19', '2021-01-15 11:07:54', '25', 'BE3G7H849', '1', 'BE3G7H849', 'Example Profile Submission Package.', '/Users/kamb/tomcat/oa/content/OAPUploadDashboard/ArchiveBundles/bags/BE3G7H849/BE3G7H849_20210115T070754Z/BE3G7H849_bagit.zip', 'Archive mode set to NONE.  Package not staged.'), ('20', '2021-01-15 16:59:49', '25', 'BE3HV4MG9', '1', 'BE3HV4MG9', 'Sample Submission profiles.', '/Users/kamb/tomcat/oa/content/OAPUploadDashboard/ArchiveBundles/bags/BE3HV4MG9/BE3HV4MG9_20210116T125949Z/BE3HV4MG9_bagit.zip', 'Archive mode set to NONE.  Package not staged.'), ('21', '2021-01-15 17:11:24', '25', 'BE3HX01PP', '1', 'BE3HX01PP', 'SAMPLE DATA.   NOT FOR ARCHIVAL.', '/Users/kamb/tomcat/oa/content/OAPUploadDashboard/ArchiveBundles/bags/BE3HX01PP/BE3HX01PP_20210116T011124Z/BE3HX01PP_bagit.zip', 'Archive mode set to NONE.  Package not staged.'), ('22', '2021-01-19 17:54:42', '25', 'BE3TUETXG', '1', 'BE3TUETXG', 'The metadata (from NCEI) in this package is wrong.  It claims north bound as -25.151, but there are no data values at latitudes > ~-46.', '/Users/kamb/tomcat/oa/content/OAPUploadDashboard/ArchiveBundles/bags/BE3TUETXG/BE3TUETXG_20210120T015442Z/BE3TUETXG_bagit.zip', 'Archive mode set to NONE.  Package not staged.'), ('23', '2021-02-23 07:33:22', '25', 'BE4RHVHR6', '1', 'BE4RHVHR6', '', '/Users/kamb/tomcat/oa/content/OAPUploadDashboard/ArchiveBundles/bags/BE4RHVHR6/BE4RHVHR6_20210223T033322Z/BE4RHVHR6_bagit.zip', 'Archive mode set to NONE.  Package not staged.'), ('24', '2021-02-23 08:05:57', '25', 'BE4RHVHR6', '2', 'BE4RHVHR6', '', '/Users/kamb/tomcat/oa/content/OAPUploadDashboard/ArchiveBundles/bags/BE4RHVHR6/BE4RHVHR6_20210223T040557Z/BE4RHVHR6_bagit.zip', 'Archive mode set to NONE.  Package not staged.');
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
