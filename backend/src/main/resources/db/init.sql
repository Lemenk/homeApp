-- MySQL 8 生产建表脚本（手动执行：mysql -u root -p < init.sql）
CREATE DATABASE IF NOT EXISTS family_home DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE family_home;

CREATE TABLE IF NOT EXISTS t_user (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  openid VARCHAR(64) COMMENT '微信小程序 openid（登录凭证）',
  phone VARCHAR(20) COMMENT '手机号（短信验证码登录）',
  nickname VARCHAR(64) COMMENT '昵称',
  avatar VARCHAR(255) COMMENT '头像 URL',
  status TINYINT DEFAULT 1 COMMENT '状态：1正常 0删除',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_phone (phone),
  UNIQUE KEY uk_openid (openid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

CREATE TABLE IF NOT EXISTS t_family (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  name VARCHAR(64) NOT NULL COMMENT '家庭名称',
  creator_id BIGINT NOT NULL COMMENT '创建人（户主）用户 ID',
  invite_code VARCHAR(16) NOT NULL UNIQUE COMMENT '邀请码（成员凭此加入家庭）',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家庭';

CREATE TABLE IF NOT EXISTS t_family_member (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  family_id BIGINT NOT NULL COMMENT '家庭 ID',
  user_id BIGINT NOT NULL COMMENT '用户 ID',
  role VARCHAR(16) NOT NULL COMMENT '角色：creator(户主)/member(成员)',
  joined_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  UNIQUE KEY uk_family_user (family_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家庭成员关系';

CREATE TABLE IF NOT EXISTS t_ledger (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  name VARCHAR(64) NOT NULL COMMENT '账本名称',
  type VARCHAR(16) NOT NULL COMMENT '账本类型：public(公共)/personal(个人)',
  icon VARCHAR(32) COMMENT '图标 key',
  theme VARCHAR(32) COMMENT '主题',
  owner_id BIGINT NOT NULL COMMENT '所有者用户 ID',
  family_id BIGINT COMMENT '公共账本关联的家庭 ID',
  status TINYINT DEFAULT 1 COMMENT '状态：1正常 0删除',
  is_default TINYINT DEFAULT 0 COMMENT '是否为默认账本：1默认 0非默认',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账本';

CREATE TABLE IF NOT EXISTS t_ledger_member (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  ledger_id BIGINT NOT NULL COMMENT '账本 ID',
  user_id BIGINT NOT NULL COMMENT '用户 ID',
  role VARCHAR(16) NOT NULL COMMENT '角色：owner(所有者)/member(成员)',
  joined_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  UNIQUE KEY uk_ledger_user (ledger_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账本成员关系';

CREATE TABLE IF NOT EXISTS t_category (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  ledger_id BIGINT NOT NULL COMMENT '所属账本 ID',
  type VARCHAR(16) NOT NULL COMMENT '分类类型：expense(支出)/income(收入)',
  name VARCHAR(32) NOT NULL COMMENT '分类名称',
  icon VARCHAR(32) COMMENT '图标 key（如 food/traffic/salary）',
  sort INT DEFAULT 0 COMMENT '排序值（越小越靠前）',
  enabled TINYINT DEFAULT 1 COMMENT '是否启用：1启用 0停用',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分类';

CREATE TABLE IF NOT EXISTS t_tag (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  ledger_id BIGINT NOT NULL COMMENT '所属账本 ID',
  name VARCHAR(32) NOT NULL COMMENT '标签名称',
  color VARCHAR(16) COMMENT '标签颜色（十六进制色值）',
  created_by BIGINT COMMENT '创建人用户 ID',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签';

CREATE TABLE IF NOT EXISTS t_account (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  ledger_id BIGINT NOT NULL COMMENT '所属账本 ID',
  type VARCHAR(16) NOT NULL COMMENT '账户类型：asset(资金)/credit(信贷)/stored_value(储值)',
  name VARCHAR(64) NOT NULL COMMENT '账户名称',
  icon VARCHAR(32) COMMENT '图标 key（对应前端 AppIcon 映射表）',
  balance DECIMAL(15,2) DEFAULT 0 COMMENT '当前余额',
  group_name VARCHAR(32) COMMENT '账户分组（自由文本，如：日常/备用）',
  remark VARCHAR(255) COMMENT '备注',
  include_in_total TINYINT DEFAULT 1 COMMENT '是否计入总资产：1计入 0不计入',
  status TINYINT DEFAULT 1 COMMENT '状态：1正常 0删除',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账户';

CREATE TABLE IF NOT EXISTS t_bill (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  ledger_id BIGINT NOT NULL COMMENT '所属账本 ID',
  type VARCHAR(16) NOT NULL COMMENT '账单类型：expense(支出)/income(收入)/transfer(转账)',
  category_id BIGINT COMMENT '分类 ID',
  member_id BIGINT COMMENT '记账人（家庭成员 ID）',
  amount DECIMAL(15,2) NOT NULL COMMENT '账单金额',
  bill_date DATETIME NOT NULL COMMENT '记账时间（秒级精度）',
  remark VARCHAR(255) COMMENT '备注',
  created_by BIGINT COMMENT '创建人用户 ID',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY idx_ledger_date (ledger_id, bill_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账单';

CREATE TABLE IF NOT EXISTS t_bill_account (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  bill_id BIGINT NOT NULL COMMENT '账单 ID',
  account_id BIGINT NOT NULL COMMENT '账户 ID',
  direction VARCHAR(8) NOT NULL COMMENT '资金方向：out(支出/转出)/in(收入/转入)',
  amount DECIMAL(15,2) NOT NULL COMMENT '该账户明细金额',
  pair_id BIGINT COMMENT '转账配对 ID（同笔转账转入/转出两条明细关联）',
  KEY idx_bill (bill_id),
  KEY idx_account (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账单-账户明细';

CREATE TABLE IF NOT EXISTS t_bill_tag (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  bill_id BIGINT NOT NULL COMMENT '账单 ID',
  tag_id BIGINT NOT NULL COMMENT '标签 ID',
  UNIQUE KEY uk_bill_tag (bill_id, tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账单-标签';

CREATE TABLE IF NOT EXISTS t_budget (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  ledger_id BIGINT NOT NULL COMMENT '所属账本 ID',
  category_id BIGINT NOT NULL COMMENT '分类 ID',
  amount DECIMAL(15,2) NOT NULL COMMENT '预算金额',
  period_type VARCHAR(16) NOT NULL COMMENT '周期类型：monthly(按月)/custom(自定义区间)',
  start_date DATE COMMENT '自定义周期开始日期',
  end_date DATE COMMENT '自定义周期结束日期',
  remark VARCHAR(64) COMMENT '备注',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预算';

CREATE TABLE IF NOT EXISTS t_audit_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  ledger_id BIGINT NOT NULL COMMENT '所属账本 ID',
  bill_id BIGINT COMMENT '关联账单 ID',
  operator_id BIGINT NOT NULL COMMENT '操作人用户 ID',
  action VARCHAR(16) NOT NULL COMMENT '操作类型：create(新增)/update(更新)/delete(删除)',
  change_detail VARCHAR(2000) COMMENT '变更详情（JSON，含账单快照/前后对比）',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作留痕';

CREATE TABLE IF NOT EXISTS t_account_balance_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  account_id BIGINT NOT NULL COMMENT '账户 ID',
  old_balance DECIMAL(15,2) COMMENT '调整前余额',
  new_balance DECIMAL(15,2) COMMENT '调整后余额',
  reason VARCHAR(255) COMMENT '调整原因',
  operator_id BIGINT COMMENT '操作人用户 ID',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='余额调整留痕';
