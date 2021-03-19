-- ----------------------------
--  Table structure for `archive_submissions`
-- ----------------------------
DROP TABLE IF EXISTS `archive_submissions`;
CREATE TABLE `oapdashboard`.`archive_submissions` (
	`db_id` bigint(20) NOT NULL AUTO_INCREMENT,
	`submit_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
	`submitter_id` bigint(20) DEFAULT NULL,
	`dataset_id` varchar(128) NOT NULL,
	`version` int(11) NOT NULL DEFAULT '1',
	`submission_key` varchar(36) NOT NULL,
	`submit_msg` text DEFAULT NULL,
	`archive_bag` text DEFAULT NULL,
	`package_location` text DEFAULT NULL,
	PRIMARY KEY (`db_id`),
	CONSTRAINT `fk_submission_submitter` FOREIGN KEY (`submitter_id`) REFERENCES `oapdashboard`.`users` (`db_id`)   ON UPDATE NO ACTION ON DELETE NO ACTION,
	INDEX `submitter_id` USING BTREE (submitter_id)
) ENGINE=`InnoDB` AUTO_INCREMENT=37 DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ROW_FORMAT=DYNAMIC CHECKSUM=0 DELAY_KEY_WRITE=0;
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
