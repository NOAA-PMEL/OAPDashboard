
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


