/*
 Navicat Premium Data Transfer

 Source Server         : 47.97.156.135
 Source Server Type    : MySQL
 Source Server Version : 50706
 Source Host           : 47.97.156.135:3306
 Source Schema         : management

 Target Server Type    : MySQL
 Target Server Version : 50706
 File Encoding         : 65001

 Date: 14/10/2019 11:21:33
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for tbl_acl
-- ----------------------------
DROP TABLE IF EXISTS `tbl_acl`;
CREATE TABLE `tbl_acl`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增Id',
  `acl_name` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '权限名称',
  `menu_url` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '菜单链接地址',
  `acl_type` int(11) NOT NULL COMMENT '1为顶层菜单 无父级菜单。2为子菜单',
  `acl_status` int(11) NULL DEFAULT 0 COMMENT '账户状态，0：启用 1：禁用',
  `acl_seq` int(11) NULL DEFAULT 0 COMMENT '菜单顺序',
  `parent_id` bigint(20) NULL DEFAULT NULL COMMENT '父级Id',
  `created_date` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_date` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Table structure for tbl_app
-- ----------------------------
DROP TABLE IF EXISTS `tbl_app`;
CREATE TABLE `tbl_app`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增Id',
  `app_id` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '接入号',
  `app_name` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '产品名称',
  `user_id` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '用户Id',
  `field` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '所属领域',
  `protocol_type` int(11) NULL DEFAULT NULL COMMENT '协议类型',
  `callback_url` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '回调地址',
  `app_status` int(11) NULL DEFAULT 0 COMMENT '状态，0：启用 1：禁用',
  `extend_code` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '扩展码',
  `speed_limit` int(11) NULL DEFAULT 0 COMMENT '流速',
  `max_connection` int(11) NULL DEFAULT 0 COMMENT '最大连接',
  `send_begin_time` datetime(0) NULL DEFAULT NULL COMMENT '开始发送时间',
  `send_end_time` datetime(0) NULL DEFAULT NULL COMMENT '结束发送时间',
  `price` decimal(6, 2) NULL DEFAULT NULL COMMENT '单条短信价格',
  `pay_type` int(11) NULL DEFAULT 0 COMMENT '支付类型 0 为预付款 1为后付款',
  `channel` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '通道',
  `created_date` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_date` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `user_app_idx`(`user_id`, `app_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Table structure for tbl_black_list
-- ----------------------------
DROP TABLE IF EXISTS `tbl_black_list`;
CREATE TABLE `tbl_black_list`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增Id',
  `app_id` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'app_id',
  `mobile` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '电话号码',
  `type` int(11) NULL DEFAULT NULL COMMENT '类别',
  `created_date` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_date` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `app_id`(`app_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Table structure for tbl_channel
-- ----------------------------
DROP TABLE IF EXISTS `tbl_channel`;
CREATE TABLE `tbl_channel`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增Id',
  `sp_id` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '接入号',
  `sp_name` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '通道名称',
  `sp_type` int(11) NULL DEFAULT 0 COMMENT '通道类型',
  `sp_status` int(11) NULL DEFAULT 0 COMMENT '状态，0:可用 1:不可用',
  `sp_connect_status` int(11) NULL DEFAULT 0 COMMENT '状态，0:可用 1:不可用',
  `sp_ip` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '通道ip',
  `sp_port` int(11) NULL DEFAULT 0 COMMENT '通道端口',
  `speed_limit` int(11) NULL DEFAULT 0 COMMENT '流速',
  `sp_login_name` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '登录名',
  `sp_login_pwd` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '登陆密码',
  `created_date` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_date` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `sp_id`(`sp_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Table structure for tbl_consumption
-- ----------------------------
DROP TABLE IF EXISTS `tbl_consumption`;
CREATE TABLE `tbl_consumption`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增Id',
  `app_id` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'app_id',
  `total_money` decimal(65, 4) NULL DEFAULT NULL COMMENT '充值总金额',
  `used_money` decimal(65, 4) NULL DEFAULT NULL COMMENT '已用总金额',
  `total_num` bigint(20) NULL DEFAULT NULL,
  `used_num` bigint(20) NULL DEFAULT NULL,
  `created_date` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_date` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `app_id`(`app_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Table structure for tbl_deliv_order
-- ----------------------------
DROP TABLE IF EXISTS `tbl_deliv_order`;
CREATE TABLE `tbl_deliv_order`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增Id',
  `own_msg_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '服务自身msgid',
  `sp_msg_id` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '运营商msgid',
  `app_id` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'app_id',
  `msg_state` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '运营商返回状态 0 代表成功',
  `share_date` timestamp(0) NULL DEFAULT NULL COMMENT '分片时间',
  `created_date` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_date` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1753 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Table structure for tbl_login
-- ----------------------------
DROP TABLE IF EXISTS `tbl_login`;
CREATE TABLE `tbl_login`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增Id',
  `user_id` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '用户Id',
  `user_name` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '用户名称',
  `login_name` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '登录名',
  `user_pwd` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '登录密码（加密后存储）',
  `role_id` int(11) NULL DEFAULT NULL COMMENT '角色id',
  `user_status` int(11) NULL DEFAULT 0 COMMENT '账户状态，0：启用 1：禁用',
  `mobile` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '用户手机号',
  `email` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '用户邮箱',
  `created_date` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_date` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `user_id`(`user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Table structure for tbl_module
-- ----------------------------
DROP TABLE IF EXISTS `tbl_module`;
CREATE TABLE `tbl_module`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增Id',
  `app_id` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'app_id',
  `content` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '内容',
  `mod_status` int(11) NULL DEFAULT 0 COMMENT '0 启用 1 禁用',
  `created_date` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_date` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `app_id`(`app_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Table structure for tbl_pro_order
-- ----------------------------
DROP TABLE IF EXISTS `tbl_pro_order`;
CREATE TABLE `tbl_pro_order`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增Id',
  `own_msg_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '服务自身msgid',
  `client_seq_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '客户端seqId',
  `own_seq_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '服务本身seqId',
  `channel_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'channel_id',
  `server_id` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'server_id',
  `protocol` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '协议种类 cmpp  http',
  `des_id` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '收信人手机号',
  `app_id` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'app_id',
  `share_date` timestamp(0) NULL DEFAULT NULL COMMENT '分片时间',
  `created_date` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_date` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1761 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Table structure for tbl_pro_secret
-- ----------------------------
DROP TABLE IF EXISTS `tbl_pro_secret`;
CREATE TABLE `tbl_pro_secret`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增Id',
  `app_id` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'app_id',
  `app_secret` bigint(100) NULL DEFAULT NULL COMMENT 'app_secret',
  `created_date` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_date` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `app_id`(`app_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Table structure for tbl_recharge_record
-- ----------------------------
DROP TABLE IF EXISTS `tbl_recharge_record`;
CREATE TABLE `tbl_recharge_record`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增Id',
  `app_id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `recharge_amount` decimal(65, 0) NULL DEFAULT NULL COMMENT '充值金额',
  `before_recharge` decimal(65, 0) NULL DEFAULT NULL COMMENT '充值前金额',
  `after_recharge` decimal(65, 0) NULL DEFAULT NULL COMMENT '充值后金额',
  `updated_by` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '操作者',
  `created_date` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_date` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Table structure for tbl_res_order
-- ----------------------------
DROP TABLE IF EXISTS `tbl_res_order`;
CREATE TABLE `tbl_res_order`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增Id',
  `own_seq_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '服务本身seqId',
  `sp_msg_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '运营商msgid',
  `share_date` timestamp(0) NULL DEFAULT NULL COMMENT '分片时间',
  `created_date` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_date` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Table structure for tbl_role
-- ----------------------------
DROP TABLE IF EXISTS `tbl_role`;
CREATE TABLE `tbl_role`  (
  `role_id` bigint(20) NOT NULL COMMENT 'roleId',
  `role_name` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '角色名称',
  `role_desc` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '角色描述',
  `type` int(11) NULL DEFAULT 2 COMMENT '角色类型 1 为管理员 2 为普通用户',
  `created_date` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_date` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`role_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Table structure for tbl_role_acl
-- ----------------------------
DROP TABLE IF EXISTS `tbl_role_acl`;
CREATE TABLE `tbl_role_acl`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增Id',
  `role_id` bigint(20) NOT NULL COMMENT 'roleId',
  `acl_id` bigint(20) NOT NULL COMMENT 'aclId',
  `created_date` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_date` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `role_acl_idx`(`role_id`, `acl_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Table structure for tbl_taboo_order
-- ----------------------------
DROP TABLE IF EXISTS `tbl_taboo_order`;
CREATE TABLE `tbl_taboo_order`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增Id',
  `own_msg_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '服务自身msgid',
  `client_seq_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '客户端seqId',
  `own_seq_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '服务本身seqId',
  `channel_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'channel_id',
  `server_id` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'server_id',
  `protocol` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '协议种类 cmpp  http',
  `des_id` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '收信人手机号',
  `link_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'link_id',
  `app_id` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'app_id',
  `share_date` timestamp(0) NULL DEFAULT NULL COMMENT '分片时间',
  `created_date` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_date` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1761 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Table structure for tbl_taboo_word
-- ----------------------------
DROP TABLE IF EXISTS `tbl_taboo_word`;
CREATE TABLE `tbl_taboo_word`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `taboo_word` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `created_date` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_date` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `taboo_word`(`taboo_word`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '禁忌词' ROW_FORMAT = Compact;

-- ----------------------------
-- Table structure for tbl_usage_statistics
-- ----------------------------
DROP TABLE IF EXISTS `tbl_usage_statistics`;
CREATE TABLE `tbl_usage_statistics`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `app_id` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `app_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `user_id` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `channel` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `submit_order_count` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `success_order_count` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `fail_order_count` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `unknown_order_count` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `created_date` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_date` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Table structure for tbl_user_info
-- ----------------------------
DROP TABLE IF EXISTS `tbl_user_info`;
CREATE TABLE `tbl_user_info`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增Id',
  `user_id` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '用户Id',
  `user_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `address` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '地址',
  `contacter` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '对接人',
  `pay_type` int(11) NULL DEFAULT 0 COMMENT '客户类型',
  `user_status` int(11) NULL DEFAULT 0 COMMENT '账户状态，0：启用 1：禁用',
  `mobile` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '用户手机号',
  `email` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '用户邮箱',
  `created_date` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_date` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `user_id`(`user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

SET FOREIGN_KEY_CHECKS = 1;
