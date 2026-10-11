-- MySQL dump 10.13  Distrib 8.0.42, for macos15.2 (arm64)
--
-- Host: 127.0.0.1    Database: runninghandai_DB
-- ------------------------------------------------------
-- Server version	8.0.41

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `bookmark`
--

DROP TABLE IF EXISTS `bookmark`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bookmark` (
  `course_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `member_id` bigint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `bookmark_uk` (`member_id`,`course_id`),
  KEY `FKt84bki61yn6g2g7k38hjclkxk` (`course_id`),
  CONSTRAINT `FK5bm7rup91j277mc7gg63akie2` FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`),
  CONSTRAINT `FKt84bki61yn6g2g7k38hjclkxk` FOREIGN KEY (`course_id`) REFERENCES `course` (`course_id`)
) ENGINE=InnoDB AUTO_INCREMENT=209 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `course`
--

DROP TABLE IF EXISTS `course`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course` (
  `distance` double NOT NULL,
  `duration` int NOT NULL,
  `max_ele` double NOT NULL,
  `min_ele` double NOT NULL,
  `course_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `external_id` varchar(255) DEFAULT NULL,
  `gpx_path` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `area` enum('HAEUN_GWANGAN','NORTHERN_BUSAN','SEOMYEON_DONGNAE','SONGJEONG_GIJANG','SOUTHERN_COAST','ETC','WESTERN_NAKDONGRIVER','WONDOSIM') DEFAULT NULL,
  `level` enum('EASY','HARD','MEDIUM') NOT NULL,
  `start_point` point NOT NULL,
  `member_id` bigint DEFAULT NULL,
  `spot_status` enum('COMPLETED','FAILED','IN_PROGRESS','NOT_APPLICABLE','NOT_STARTED') NOT NULL DEFAULT 'COMPLETED',
  PRIMARY KEY (`course_id`),
  UNIQUE KEY `UK4xqvdpkafb91tt3hsb67ga3fj` (`name`),
  UNIQUE KEY `UKftj9sywcqetdlrcts15h17nx3` (`external_id`),
  SPATIAL KEY `idx_course_location` (`start_point`),
  KEY `fk_course_creator` (`member_id`),
  CONSTRAINT `fk_course_creator` FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=196 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `course_image`
--

DROP TABLE IF EXISTS `course_image`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_image` (
  `course_id` bigint NOT NULL,
  `course_img_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `img_url` varchar(255) NOT NULL,
  PRIMARY KEY (`course_img_id`),
  UNIQUE KEY `UKti4s11n8wee7ym2yndccdbms2` (`course_id`),
  CONSTRAINT `FK5uweik1v6wv796ggohfekp7wb` FOREIGN KEY (`course_id`) REFERENCES `course` (`course_id`)
) ENGINE=InnoDB AUTO_INCREMENT=193 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `course_spot`
--

DROP TABLE IF EXISTS `course_spot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_spot` (
  `course_spot_id` bigint NOT NULL AUTO_INCREMENT,
  `course_id` bigint NOT NULL,
  `spot_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`course_spot_id`),
  UNIQUE KEY `course_spot_uk` (`course_id`,`spot_id`),
  KEY `fk_spot` (`spot_id`),
  CONSTRAINT `fk_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`course_id`),
  CONSTRAINT `fk_spot` FOREIGN KEY (`spot_id`) REFERENCES `spot` (`spot_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1384 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `course_themes`
--

DROP TABLE IF EXISTS `course_themes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_themes` (
  `course_course_id` bigint NOT NULL,
  `theme` enum('DOWNTOWN','MOUNTAIN','RIVERSIDE','SEA','ETC') DEFAULT NULL,
  KEY `FKlmlrl4xgc258abdvsrfh9pvft` (`course_course_id`),
  CONSTRAINT `FKlmlrl4xgc258abdvsrfh9pvft` FOREIGN KEY (`course_course_id`) REFERENCES `course` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `member`
--

DROP TABLE IF EXISTS `member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `member` (
  `created_at` datetime(6) NOT NULL,
  `member_id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) NOT NULL,
  `nickname` varchar(20) NOT NULL,
  `email` varchar(100) NOT NULL,
  `provider_id` varchar(255) NOT NULL,
  `refresh_token` varchar(255) DEFAULT NULL,
  `provider` enum('GOOGLE','KAKAO','NAVER') NOT NULL,
  `role` enum('ADMIN','USER') NOT NULL,
  PRIMARY KEY (`member_id`),
  UNIQUE KEY `UKhh9kg6jti4n1eoiertn2k6qsc` (`nickname`),
  UNIQUE KEY `UKlxi0241cqql7g0knl5ssy59ij` (`provider`,`provider_id`)
) ENGINE=InnoDB AUTO_INCREMENT=51 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `review`
--

DROP TABLE IF EXISTS `review`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `review` (
  `review_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `contents` varchar(2000) NOT NULL,
  `stars` double NOT NULL,
  `course_id` bigint NOT NULL,
  `member_id` bigint DEFAULT NULL,
  PRIMARY KEY (`review_id`),
  KEY `FKk0ccx5i4ci2wd70vegug074w1` (`member_id`),
  KEY `FKprox8elgnr8u5wrq1983degk` (`course_id`),
  CONSTRAINT `FKk0ccx5i4ci2wd70vegug074w1` FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`) ON DELETE SET NULL,
  CONSTRAINT `FKprox8elgnr8u5wrq1983degk` FOREIGN KEY (`course_id`) REFERENCES `course` (`course_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=79 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `road_condition`
--

DROP TABLE IF EXISTS `road_condition`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `road_condition` (
  `course_id` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `road_condition_id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) NOT NULL,
  `description` text,
  PRIMARY KEY (`road_condition_id`),
  KEY `FKsha81pvm99jy3b1lv7guo6dtx` (`course_id`),
  CONSTRAINT `FKsha81pvm99jy3b1lv7guo6dtx` FOREIGN KEY (`course_id`) REFERENCES `course` (`course_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1046 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `spot`
--

DROP TABLE IF EXISTS `spot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `spot` (
  `spot_id` bigint NOT NULL AUTO_INCREMENT,
  `external_id` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `address` varchar(255) NOT NULL,
  `description` text,
  `category` enum('NATURE','HISTORY','RECREATION','EXPERIENCE','INDUSTRIAL','ARCHITECTURE','KOREAN_FOOD','WESTERN_FOOD','JAPANESE_FOOD','CHINESE_FOOD','GLOBAL_FOOD','CAFE','CLUB','UNKNOWN') NOT NULL,
  `lat` double NOT NULL,
  `lon` double NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`spot_id`),
  UNIQUE KEY `external_id` (`external_id`)
) ENGINE=InnoDB AUTO_INCREMENT=576 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `spot_image`
--

DROP TABLE IF EXISTS `spot_image`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `spot_image` (
  `spot_img_id` bigint NOT NULL AUTO_INCREMENT,
  `img_url` varchar(255) NOT NULL,
  `original_url` varchar(255) NOT NULL,
  `spot_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`spot_img_id`),
  UNIQUE KEY `spot_id` (`spot_id`),
  CONSTRAINT `fk_spot_image_spot` FOREIGN KEY (`spot_id`) REFERENCES `spot` (`spot_id`)
) ENGINE=InnoDB AUTO_INCREMENT=460 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `track_point`
--

DROP TABLE IF EXISTS `track_point`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `track_point` (
  `ele` double NOT NULL,
  `lat` double NOT NULL,
  `lon` double NOT NULL,
  `sequence` int NOT NULL,
  `course_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `track_point_id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`track_point_id`),
  KEY `FKpupqiw5q83q159swqraqw9hpm` (`course_id`),
  CONSTRAINT `FKpupqiw5q83q159swqraqw9hpm` FOREIGN KEY (`course_id`) REFERENCES `course` (`course_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3269355 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-11 15:55:22
