/*
 Navicat Premium Data Transfer

 Source Server         : new-bock
 Source Server Type    : MySQL
 Source Server Version : 50173
 Source Host           : new-bock
 Source Database       : ioap_me

 Target Server Type    : MySQL
 Target Server Version : 50173
 File Encoding         : utf-8

 Date: 12/13/2019 09:42:43 AM
*/

SET NAMES utf8;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `citation`
-- ----------------------------
DROP TABLE IF EXISTS `citation`;
CREATE TABLE `citation` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `version` bigint(20) NOT NULL,
  `citation_author_list` longtext COLLATE utf8_bin,
  `cruise_id` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `data_use` longtext COLLATE utf8_bin,
  `dataset_abstract` longtext COLLATE utf8_bin,
  `document_id` bigint(20) NOT NULL,
  `expocode` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `purpose` longtext COLLATE utf8_bin,
  `research_projects` longtext COLLATE utf8_bin,
  `scientific_references` longtext COLLATE utf8_bin,
  `section` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `supplemental_information` longtext COLLATE utf8_bin,
  `title` longtext COLLATE utf8_bin,
  `use_limitation` longtext COLLATE utf8_bin,
  PRIMARY KEY (`id`),
  KEY `FKioom1ffp0yx3uv5qa7ary352r` (`document_id`),
  CONSTRAINT `FKioom1ffp0yx3uv5qa7ary352r` FOREIGN KEY (`document_id`) REFERENCES `document` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=125 DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- ----------------------------
--  Table structure for `country_suggestion`
-- ----------------------------
DROP TABLE IF EXISTS `country_suggestion`;
CREATE TABLE `country_suggestion` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `version` bigint(20) NOT NULL,
  `suggestion` varchar(255) COLLATE utf8_bin NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- ----------------------------
--  Table structure for `document`
-- ----------------------------
DROP TABLE IF EXISTS `document`;
CREATE TABLE `document` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `version` bigint(20) NOT NULL,
  `last_modified` varchar(255) COLLATE utf8_bin NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=125 DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- ----------------------------
--  Table structure for `document_update_listener`
-- ----------------------------
DROP TABLE IF EXISTS `document_update_listener`;
CREATE TABLE `document_update_listener` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `version` bigint(20) NOT NULL,
  `document_id` varchar(255) COLLATE utf8_bin NOT NULL,
  `document_location` varchar(255) COLLATE utf8_bin NOT NULL,
  `expo_code` varchar(255) COLLATE utf8_bin NOT NULL,
  `notification_url` varchar(255) COLLATE utf8_bin NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- ----------------------------
--  Table structure for `funding`
-- ----------------------------
DROP TABLE IF EXISTS `funding`;
CREATE TABLE `funding` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `version` bigint(20) NOT NULL,
  `agency_name` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `document_id` bigint(20) DEFAULT NULL,
  `grant_number` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `grant_title` longtext COLLATE utf8_bin,
  `funding_idx` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKkwcc9fmu807ouw7t7s9koqs6k` (`document_id`),
  CONSTRAINT `FKkwcc9fmu807ouw7t7s9koqs6k` FOREIGN KEY (`document_id`) REFERENCES `document` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=63 DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- ----------------------------
--  Table structure for `generic_variable`
-- ----------------------------
DROP TABLE IF EXISTS `generic_variable`;
CREATE TABLE `generic_variable` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `version` bigint(20) NOT NULL,
  `abbreviation` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `analyzing_instrument` longtext COLLATE utf8_bin,
  `batch_number` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `biological_subject` longtext COLLATE utf8_bin,
  `calculation_method` longtext COLLATE utf8_bin,
  `cell_type` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `crm_manufacture` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `curve_fitting_method` longtext COLLATE utf8_bin,
  `detailed_information` longtext COLLATE utf8_bin,
  `drying_method` longtext COLLATE utf8_bin,
  `duration` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `equilibrator_pressure_measure_method` longtext COLLATE utf8_bin,
  `equilibrator_temperature_measure_method` longtext COLLATE utf8_bin,
  `equilibrator_type` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `equilibrator_volume` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `field_replicate` longtext COLLATE utf8_bin,
  `flow_rate` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `freqency_of_standardization` longtext COLLATE utf8_bin,
  `full_variable_name` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `gas_concentration` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `gas_dectector_resolution` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `gas_dectector_uncertainty` longtext COLLATE utf8_bin,
  `gas_detector_manufacture` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `gas_detector_model` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `gas_flow_rate` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `headspace_volume` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `intake_depth` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `intake_location` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `life_stage` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `magnitude_of_blank_correction` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `manipulation_method` longtext COLLATE utf8_bin,
  `measured` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `observation_detail` longtext COLLATE utf8_bin,
  `observation_type` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `pco2temperature` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `ph_scale` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `ph_standards` longtext COLLATE utf8_bin,
  `ph_temperature` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `poison` longtext COLLATE utf8_bin,
  `poison_description` longtext COLLATE utf8_bin,
  `poison_volume` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `quality_flag` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `reference_method` longtext COLLATE utf8_bin,
  `researcher_institution` longtext COLLATE utf8_bin,
  `researcher_name` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `sampling_instrument` longtext COLLATE utf8_bin,
  `seawater_volume` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `species_id_code` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `standard_gas_manufacture` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `standard_gas_uncertainties` longtext COLLATE utf8_bin,
  `standardization_technique` longtext COLLATE utf8_bin,
  `storage_method` longtext COLLATE utf8_bin,
  `temperature_correction_method` longtext COLLATE utf8_bin,
  `temperature_measurement` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `temperature_standarization` longtext COLLATE utf8_bin,
  `titration_type` longtext COLLATE utf8_bin,
  `uncertainty` longtext COLLATE utf8_bin,
  `units` longtext COLLATE utf8_bin,
  `vapor_correction` longtext COLLATE utf8_bin,
  `vented` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `class` varchar(255) COLLATE utf8_bin NOT NULL,
  `document_id` bigint(20) DEFAULT NULL,
  `variables_idx` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK664yjl2d7damuendddx0vpw2l` (`document_id`),
  CONSTRAINT `FK664yjl2d7damuendddx0vpw2l` FOREIGN KEY (`document_id`) REFERENCES `document` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3117 DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- ----------------------------
--  Table structure for `ncei_suggestion`
-- ----------------------------
DROP TABLE IF EXISTS `ncei_suggestion`;
CREATE TABLE `ncei_suggestion` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `version` bigint(20) NOT NULL,
  `suggestion` varchar(255) COLLATE utf8_bin NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- ----------------------------
--  Table structure for `person`
-- ----------------------------
DROP TABLE IF EXISTS `person`;
CREATE TABLE `person` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `version` bigint(20) NOT NULL,
  `address1` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `address2` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `city` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `country` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `email` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `extension` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `first_name` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `id_type` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `institution` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `last_name` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `mi` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `rid` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `state` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `telephone` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `zip` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `class` varchar(255) COLLATE utf8_bin NOT NULL,
  `document_id` bigint(20) DEFAULT NULL,
  `investigators_idx` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKahsnx7wkyvehbeknklxn4365c` (`document_id`),
  CONSTRAINT `FKahsnx7wkyvehbeknklxn4365c` FOREIGN KEY (`document_id`) REFERENCES `document` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=431 DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- ----------------------------
--  Table structure for `platform`
-- ----------------------------
DROP TABLE IF EXISTS `platform`;
CREATE TABLE `platform` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `version` bigint(20) NOT NULL,
  `country` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `document_id` bigint(20) DEFAULT NULL,
  `name` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `owner` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `platform_id` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `platform_type` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `platforms_idx` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK82o5gkh3q5c2slwffed5e1ac4` (`document_id`),
  CONSTRAINT `FK82o5gkh3q5c2slwffed5e1ac4` FOREIGN KEY (`document_id`) REFERENCES `document` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=57 DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- ----------------------------
--  Table structure for `suggest_query`
-- ----------------------------
DROP TABLE IF EXISTS `suggest_query`;
CREATE TABLE `suggest_query` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `version` bigint(20) NOT NULL,
  `query` varchar(255) COLLATE utf8_bin NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- ----------------------------
--  Table structure for `time_and_location`
-- ----------------------------
DROP TABLE IF EXISTS `time_and_location`;
CREATE TABLE `time_and_location` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `version` bigint(20) NOT NULL,
  `document_id` bigint(20) NOT NULL,
  `east_lon` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `end_date` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `geo_names` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `north_lat` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `organism_loc` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `south_lat` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `spatial_ref` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `start_date` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `west_lon` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKfi5wht63e70paohywuf0psh4t` (`document_id`),
  CONSTRAINT `FKfi5wht63e70paohywuf0psh4t` FOREIGN KEY (`document_id`) REFERENCES `document` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=125 DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

SET FOREIGN_KEY_CHECKS = 1;
