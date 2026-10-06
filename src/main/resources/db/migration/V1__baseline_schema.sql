
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
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `booking_seats` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `price` decimal(10,2) NOT NULL,
  `booking_id` bigint NOT NULL,
  `event_seat_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKlglnmuv4xfx9h7b6rfijhip3j` (`booking_id`,`event_seat_id`),
  KEY `FKbnfsmd5ou9e0d3goma42xg8bp` (`event_seat_id`),
  CONSTRAINT `FKbnfsmd5ou9e0d3goma42xg8bp` FOREIGN KEY (`event_seat_id`) REFERENCES `event_seats` (`id`),
  CONSTRAINT `FKmbi9ciapn0nvat63t0a8tv478` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bookings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `booking_reference` varchar(255) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `status` enum('PENDING','CONFIRMED','CANCELLED','FAILED','EXPIRED') NOT NULL,
  `total_amount` decimal(10,2) NOT NULL,
  `event_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `idempotency_key` varchar(100) DEFAULT NULL,
  `request_fingerprint` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKe92mgyq35mdeo8gc1un2o6uk0` (`booking_reference`),
  UNIQUE KEY `uk_booking_user_idempotency` (`user_id`,`idempotency_key`),
  KEY `idx_bookings_user_created_at` (`user_id`,`created_at` DESC),
  KEY `idx_bookings_event_status` (`event_id`,`status`),
  CONSTRAINT `FK2ww82bk3npaiyu9oeehwtt2q3` FOREIGN KEY (`event_id`) REFERENCES `events` (`id`),
  CONSTRAINT `FKeyog2oic85xg7hsu2je2lx3s6` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `event_seats` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `price` decimal(10,2) NOT NULL,
  `status` enum('AVAILABLE','BOOKED','LOCKED') NOT NULL,
  `event_id` bigint NOT NULL,
  `seat_id` bigint NOT NULL,
  `locked_until` datetime(6) DEFAULT NULL,
  `locked_by_booking_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK5sq09gsxtbcwpk0frq6fl2t4s` (`event_id`,`seat_id`),
  KEY `FKl7eack333vbgtvtcspjw2thne` (`seat_id`),
  KEY `FKtiukn2fbmmc7lxywx1rdn1i4n` (`locked_by_booking_id`),
  KEY `idx_event_seats_event_status` (`event_id`,`status`),
  KEY `idx_event_seats_status_locked_until` (`status`,`locked_until`),
  CONSTRAINT `FK4msxxyowfen62g7dkctsf7c7y` FOREIGN KEY (`event_id`) REFERENCES `events` (`id`),
  CONSTRAINT `FKl7eack333vbgtvtcspjw2thne` FOREIGN KEY (`seat_id`) REFERENCES `seats` (`id`),
  CONSTRAINT `FKtiukn2fbmmc7lxywx1rdn1i4n` FOREIGN KEY (`locked_by_booking_id`) REFERENCES `bookings` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `events` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category` enum('COMEDY','CONCERT','MOVIE','OTHER','SPORTS','THEATRE') NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(1000) NOT NULL,
  `end_time` datetime(6) NOT NULL,
  `name` varchar(255) NOT NULL,
  `start_time` datetime(6) NOT NULL,
  `status` enum('CANCELLED','COMPLETED','ONGOING','UPCOMING') NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `hall_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_events_hall_start_end` (`hall_id`,`start_time`,`end_time`),
  CONSTRAINT `FKq4ae93bqrbhtadn64yumsgk6n` FOREIGN KEY (`hall_id`) REFERENCES `halls` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `halls` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `venue_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKmv53p1s2aabic94w841iu6quh` (`venue_id`,`name`),
  CONSTRAINT `FKikgdvpf0hxbmg4fri5lq8o0t9` FOREIGN KEY (`venue_id`) REFERENCES `venues` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount` decimal(10,2) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `payment_reference` varchar(255) NOT NULL,
  `status` enum('FAILED','PENDING','SUCCESS') NOT NULL,
  `booking_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK4jacl30fsqtdp5mhmg5wnvn7q` (`payment_reference`),
  KEY `idx_payments_booking_status` (`booking_id`,`status`),
  KEY `idx_payments_booking_created_at` (`booking_id`,`created_at` DESC),
  CONSTRAINT `FKc52o2b1jkxttngufqp3t7jr3h` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `refunds` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount` decimal(10,2) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `refund_reference` varchar(255) NOT NULL,
  `status` enum('FAILED','SUCCESS') NOT NULL,
  `booking_id` bigint NOT NULL,
  `payment_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK2irx02s3tuilqcgvcvemmd04l` (`refund_reference`),
  KEY `FKpt9ic0j1y6xwlej99wnynvnpy` (`payment_id`),
  KEY `idx_refunds_booking_status` (`booking_id`,`status`),
  CONSTRAINT `FK3nlfu8o4nsl1kcfp2sg8q4gnd` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`),
  CONSTRAINT `FKpt9ic0j1y6xwlej99wnynvnpy` FOREIGN KEY (`payment_id`) REFERENCES `payments` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `seats` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `row_name` varchar(255) NOT NULL,
  `seat_number` int NOT NULL,
  `seat_type` enum('PREMIUM','REGULAR','VIP') NOT NULL,
  `hall_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK5rwk1upcw5aji278kpfehnoig` (`hall_id`,`row_name`,`seat_number`),
  CONSTRAINT `FK3jtfe0f60bcpbavj4mctjeasw` FOREIGN KEY (`hall_id`) REFERENCES `halls` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `email` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `role` enum('ADMIN','USER') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `venues` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `address` varchar(255) NOT NULL,
  `city` varchar(255) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `name` varchar(255) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_venues_city` (`city`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

