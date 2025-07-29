-- 用户表
CREATE TABLE `user` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `username` VARCHAR(50),
  `password` VARCHAR(255),
  `type` VARCHAR(20),
  `battery_capacity` DOUBLE,
  `current_power` DOUBLE
);

-- 充电桩表
CREATE TABLE `charging_pile` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `pile_number` VARCHAR(20),
  `pile_type` VARCHAR(20),
  `charging_power` DOUBLE,
  `status` VARCHAR(20)
);

-- 充电请求表
CREATE TABLE `charging_request` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `request_id` VARCHAR(50),
  `user_id` BIGINT,
  `charging_mode` VARCHAR(20),
  `charging_amount` DOUBLE,
  `status` VARCHAR(20),
  `create_time` DATETIME,
  `start_time` DATETIME,
  `end_time` DATETIME,
  `charging_pile_id` INT,
  `queue_position` INT
);

-- 充电详单表
CREATE TABLE `charging_bill` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `bill_number` VARCHAR(50),
  `bill_time` DATETIME,
  `request_id` VARCHAR(50),
  `user_id` BIGINT,
  `pile_number` VARCHAR(20),
  `charged_amount` DOUBLE,
  `charging_duration` INT,
  `start_time` DATETIME,
  `stop_time` DATETIME,
  `charging_fee` DECIMAL(10,2),
  `service_fee` DECIMAL(10,2),
  `total_fee` DECIMAL(10,2),
  `charging_mode` VARCHAR(20),
  `charging_power` DOUBLE
);

-- 系统配置表
CREATE TABLE `system_config` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `config_key` VARCHAR(100),
  `config_value` VARCHAR(500),
  `description` VARCHAR(200)
);

-- 系统常量表
CREATE TABLE `system_constant` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `constant_key` VARCHAR(100),
  `constant_value` VARCHAR(500),
  `constant_type` VARCHAR(20),
  `description` VARCHAR(200),
  `is_active` TINYINT(1)
);

-- =====================================
-- 数据插入语句
-- =====================================

-- 插入充电桩数据
INSERT INTO `charging_pile` (`pile_number`, `pile_type`, `charging_power`, `status`) VALUES
('F001', 'FAST', 30.0, 'AVAILABLE'),
('F002', 'FAST', 30.0, 'AVAILABLE'),
('T001', 'SLOW', 7.0, 'AVAILABLE'),
('T002', 'SLOW', 7.0, 'AVAILABLE'),
('T003', 'SLOW', 7.0, 'AVAILABLE');

-- 插入系统配置数据
INSERT INTO `system_config` (`config_key`, `config_value`, `description`) VALUES
('waiting_area_capacity', '6', '等候区最大车位容量'),
('pile_queue_length', '2', '充电桩队列长度'),
('schedule_strategy', 'ORIGINAL', '调度策略，可选值：ORIGINAL(原始调度)、SINGLE_BATCH_OPTIMAL(单次调度最优)、FULL_BATCH_OPTIMAL(批量调度最优)');

-- 插入系统常量数据
INSERT INTO `system_constant` (`constant_key`, `constant_value`, `constant_type`, `description`, `is_active`) VALUES
('fast_charging_power', '30.0', 'DOUBLE', '快充功率(kW)', 1),
('slow_charging_power', '7.0', 'DOUBLE', '慢充功率(kW)', 1);

