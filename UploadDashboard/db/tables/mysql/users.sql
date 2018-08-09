CREATE TABLE `users` (
  `db_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `modified_time` timestamp NOT NULL,
  `username` varchar(128) CHARACTER SET latin1 NOT NULL,
  `last_login` timestamp NULL DEFAULT NULL,
  `first_name` varchar(128) CHARACTER SET latin1 NOT NULL,
  `last_name` varchar(128) CHARACTER SET latin1 NOT NULL,
  `email` varchar(128) CHARACTER SET latin1 NOT NULL,
  PRIMARY KEY (`db_id`),
  KEY `idx_users_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8

-- modtime: DEFAULT CURRENT_TIMESTAMP  ON UPDATE CURRENT_TIMESTAMP 
-- dunkel 5.1 can't have more than 1 column as  default current_timestamp
