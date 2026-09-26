-- marr 婚姻登记与婚姻家庭辅导服务管理 -- schema (chen-013)
-- 列名与基线实体契约（@TableName/@TableField）逐列对齐，改列必须同步实体。
-- 库：chen_013

CREATE TABLE IF NOT EXISTS t_marr_cert_bill (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '婚姻登记记录证明核准单号',
  node_no int DEFAULT NULL COMMENT '当前所在层 0..3',
  sign_mode int DEFAULT NULL COMMENT '同层核准方式 0任一人 1两名点齐',
  need_count int DEFAULT NULL COMMENT '本层应签人数',
  sign_count int DEFAULT NULL COMMENT '本层已签人数',
  status int DEFAULT NULL COMMENT '报批情形 0在核 1已核讫 2已打回',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='婚姻登记记录证明逐级核准单';

-- 冷静期与领证期到期催办条目：一条就一件事（届满提醒是一项、领证催办是一项、期满未办按撤回落定是一项）。
-- 倒计时一律从 due_at（届满那天的最后一刻）往前数 amount 个自然日，压到当天就算该催；
-- 挑的时候这么数，回写的时候也这么数，全处只许这一种说法。口径要改只能改 amount，不许人把 due_at 往后拖。
-- 一宗牵着两位当事人，phone_a/phone_b 各存各的，催办各送一条；sent_a/sent_b 分人记送达，
-- 一位当日已收到、另一位没收到，下轮只补没收到的那一位。两位都送到才算已发。
-- 送不出去的挂起（status=2）并当场写明 hang_reason：电话为空，或本宗已按撤回落定（settled_flag=1）不再催。
-- 本轮催过的下轮不再捞；同一事项对同一收信人重复提交的，账上只认头一遍，后到的不补第二句、也不改写先落的那条。
CREATE TABLE IF NOT EXISTS t_marr_due_task (
  id bigint NOT NULL COMMENT '主键',
  item_no varchar(64) DEFAULT NULL COMMENT '到期催办事项单号',
  due_at datetime DEFAULT NULL COMMENT '本项届满之日那天的最后一刻',
  amount decimal(12,2) DEFAULT NULL COMMENT '单条提前催办的自然日数',
  content varchar(255) DEFAULT NULL COMMENT '催办事由与收信人记要',
  phone_a varchar(32) DEFAULT NULL COMMENT '甲方联系电话(两位各存各的)',
  phone_b varchar(32) DEFAULT NULL COMMENT '乙方联系电话(两位各存各的)',
  sent_a int DEFAULT '0' COMMENT '甲方催办送达 0未送 1已送',
  sent_b int DEFAULT '0' COMMENT '乙方催办送达 0未送 1已送',
  settled_flag int DEFAULT '0' COMMENT '本宗已按撤回落定 0否 1是(落定后不再催)',
  hang_reason varchar(255) DEFAULT NULL COMMENT '挂起缘由(当场写明为什么挂)',
  status int DEFAULT NULL COMMENT '条目情形 0待发 1已发 2挂起(发不出去)',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='冷静期与领证期到期催办条目';

CREATE TABLE IF NOT EXISTS t_marr_fix_card (
  id bigint NOT NULL COMMENT '主键',
  biz_no varchar(64) DEFAULT NULL COMMENT '婚姻登记差错更正事务卡号',
  stage int DEFAULT NULL COMMENT '当前关口 0..5',
  status int DEFAULT NULL COMMENT '卡的落定 0在办 1已转出 2已结案',
  content varchar(255) DEFAULT NULL COMMENT '纠错记事',
  last_action varchar(64) DEFAULT NULL COMMENT '最近一次过口动作',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='婚姻登记差错更正事务卡';

-- 卷宗影像件建档卡：现行账。同一份卷宗同一段页次在本表只许有一行（认 site_no+段页次，不认到达先后）。
-- 旧版不删，升版时整行退入 t_marr_img_card_his，另存一层供事后翻检；现行位置只露最新版。
CREATE TABLE IF NOT EXISTS t_marr_img_card (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '影像件建档卡号(系统发号,不许自编)',
  site_no varchar(64) DEFAULT NULL COMMENT '婚姻卷宗档案编号(凭编号挂档案,不凭姓名)',
  seg_start int DEFAULT NULL COMMENT '本段管起页次(页)',
  seg_end int DEFAULT NULL COMMENT '本段管止页次(页)',
  node_no int DEFAULT NULL COMMENT '本件影像版本号(重扫一版进一版)',
  qty decimal(12,2) DEFAULT NULL COMMENT '本卡合并后在册影像页数(页)',
  fine_amt decimal(12,2) DEFAULT NULL COMMENT '本年度新上件页数(页)',
  grade_level int DEFAULT NULL COMMENT '影像完整度档(系统折算写回,手工不得改)',
  handler_name varchar(64) DEFAULT NULL COMMENT '本趟承办人(办一次续一笔,与创建人分列)',
  content varchar(255) DEFAULT NULL COMMENT '出证水印字样(压死那一刻定格,此后不可改)',
  wm_date date DEFAULT NULL COMMENT '水印当日日期(随影像版本走,新旧版各用各的)',
  status int DEFAULT NULL COMMENT '进展 0待核对 1已核对 2已压死',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  cur_key varchar(160) GENERATED ALWAYS AS
    (CASE WHEN del_flag = 0 THEN CONCAT(site_no,'#',seg_start,'#',seg_end) ELSE NULL END) VIRTUAL
    COMMENT '现行账唯一键:同卷宗同段页次只得一行',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者(立卡人,任人重录也不挪)',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  UNIQUE KEY uk_cur_key (cur_key),
  KEY idx_site_no (site_no),
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='卷宗影像件建档卡(现行账)';

-- 影像件历史版本层：只进不改不删，每升一版追加一行；水印字样随版封存，两版不许共用同一行字。
CREATE TABLE IF NOT EXISTS t_marr_img_card_his (
  id bigint NOT NULL COMMENT '主键',
  card_id bigint DEFAULT NULL COMMENT '来源现行卡id',
  bill_no varchar(64) DEFAULT NULL COMMENT '影像件建档卡号',
  site_no varchar(64) DEFAULT NULL COMMENT '婚姻卷宗档案编号',
  seg_start int DEFAULT NULL COMMENT '本段管起页次(页)',
  seg_end int DEFAULT NULL COMMENT '本段管止页次(页)',
  node_no int DEFAULT NULL COMMENT '封存的影像版本号',
  qty decimal(12,2) DEFAULT NULL COMMENT '该版在册封影像页数(页)',
  fine_amt decimal(12,2) DEFAULT NULL COMMENT '该版本年度新上件页数(页)',
  grade_level int DEFAULT NULL COMMENT '该版影像完整度档',
  handler_name varchar(64) DEFAULT NULL COMMENT '该版承办人',
  content varchar(255) DEFAULT NULL COMMENT '该版压死定格的水印字样(原样留存)',
  wm_date date DEFAULT NULL COMMENT '该版水印当日日期',
  status int DEFAULT NULL COMMENT '退现行时卡进展 0待核对 1已核对 2已压死',
  create_by varchar(64) DEFAULT NULL COMMENT '版本封存人',
  create_time datetime DEFAULT NULL COMMENT '版本封存时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  UNIQUE KEY uk_card_node (card_id,node_no),
  KEY idx_site_no (site_no),
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='卷宗影像件历史版本层';

-- 承办续笔：一张卡每办一趟续一笔，创建人(create_by)不挪，跑趟人记这里。
CREATE TABLE IF NOT EXISTS t_marr_img_handler_log (
  id bigint NOT NULL COMMENT '主键',
  card_id bigint DEFAULT NULL COMMENT '影像件建档卡id',
  bill_no varchar(64) DEFAULT NULL COMMENT '影像件建档卡号',
  site_no varchar(64) DEFAULT NULL COMMENT '婚姻卷宗档案编号',
  node_no int DEFAULT NULL COMMENT '承办时所在影像版本号',
  handler_name varchar(64) DEFAULT NULL COMMENT '本趟承办人',
  action varchar(32) DEFAULT NULL COMMENT '过口动作 register建档 reupload重扫 rehang改挂 segment页次增减 verify核对 press压死',
  memo varchar(255) DEFAULT NULL COMMENT '承办记要',
  create_by varchar(64) DEFAULT NULL COMMENT '记录人',
  create_time datetime DEFAULT NULL COMMENT '记录时间',
  PRIMARY KEY (id),
  KEY idx_card_id (card_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='影像件承办续笔';

CREATE TABLE IF NOT EXISTS t_marr_report_row (
  id bigint NOT NULL COMMENT '主键',
  batch_no varchar(64) DEFAULT NULL COMMENT '登记统计报送册号',
  row_no int DEFAULT NULL COMMENT '原册内行次',
  item_code varchar(64) DEFAULT NULL COMMENT '报表项代号',
  qty decimal(12,2) DEFAULT NULL COMMENT '本行填报登记对数(对)',
  status int DEFAULT NULL COMMENT '行进展 0待核 1已汇纳 2挂驳回',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='登记统计报送册核收转存行';

CREATE TABLE IF NOT EXISTS t_marr_risk_line (
  id bigint NOT NULL COMMENT '主键',
  rule_code varchar(64) DEFAULT NULL COMMENT '辅导风险判据代号',
  rule_name varchar(128) DEFAULT NULL COMMENT '判据名称',
  th1_max decimal(12,2) DEFAULT NULL COMMENT '一档风险分值封顶值',
  th2_max decimal(12,2) DEFAULT NULL COMMENT '二档风险分值封顶值',
  th3_max decimal(12,2) DEFAULT NULL COMMENT '三档风险分值封顶值',
  eff_start datetime DEFAULT NULL COMMENT '启用之日',
  eff_end datetime DEFAULT NULL COMMENT '交棒之日(不含)',
  priority int DEFAULT NULL COMMENT '抢档顺位(数值越大越优先)',
  status int DEFAULT NULL COMMENT '判据的情形 0现行 1已停用',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='婚姻家庭辅导风险评估判据线';

CREATE TABLE IF NOT EXISTS t_marr_vol_doc (
  id bigint NOT NULL COMMENT '主键',
  site_no varchar(64) DEFAULT NULL COMMENT '婚姻卷宗档案编号(系统自动发号,四类各归各排)',
  site_name varchar(128) DEFAULT NULL COMMENT '卷宗全称',
  site_type varchar(32) DEFAULT NULL COMMENT '卷宗类别 结婚/离婚/补领/撤销',
  road_name varchar(128) DEFAULT NULL COMMENT '承办处(省—市—区县)',
  img_max decimal(12,2) DEFAULT NULL COMMENT '本档案最多承接影像页数(页)',
  th1_max decimal(12,2) DEFAULT NULL COMMENT '在册影像页数一档上限(页)',
  th2_max decimal(12,2) DEFAULT NULL COMMENT '二档上限(页)',
  th3_max decimal(12,2) DEFAULT NULL COMMENT '三档上限(页)',
  status int DEFAULT NULL COMMENT '档案情形 0在库 1已封卷',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  UNIQUE KEY uk_site_no (site_no),
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='婚姻卷宗基础档案';

-- 查档授权：本人凭证件只看自己名下那一卷；机关凭公函看本辖内全部；律师持委托函，受托哪一宗才看得见哪一宗。
-- viewer_type=1本人：id_no 证件号 + site_no 名下卷
-- viewer_type=2机关：scope_road 本辖内承办处名(按省—市—区县前缀辖)，site_no 留空
-- viewer_type=3律师：id_no 执业证号 + site_no 受托卷宗(一函一宗一行)
CREATE TABLE IF NOT EXISTS t_marr_archive_grant (
  id bigint NOT NULL COMMENT '主键',
  grant_no varchar(64) DEFAULT NULL COMMENT '查档授权登记号',
  viewer_type int DEFAULT NULL COMMENT '查档人情形 1本人 2机关 3律师',
  viewer_name varchar(64) DEFAULT NULL COMMENT '查档人/机关名',
  id_no varchar(32) DEFAULT NULL COMMENT '本人证件号或律师执业证号',
  scope_road varchar(128) DEFAULT NULL COMMENT '机关本辖内承办处名(省—市—区县)',
  site_no varchar(64) DEFAULT NULL COMMENT '本人名下卷或律师受托卷编号(机关留空)',
  matter varchar(255) DEFAULT NULL COMMENT '查档事由(进水印那一行)',
  letter_no varchar(64) DEFAULT NULL COMMENT '公函/法院函/委托函号',
  status int DEFAULT NULL COMMENT '授权情形 0有效 1已失效',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  KEY idx_viewer (viewer_type,id_no),
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='查档授权登记';

-- 初始档案数据：预置两条卷宗基础档案，一条在册、一条已封卷。
-- id=0 为在册卷宗，可继续挂用；id=1 为已封卷卷宗，仅供翻查，新上件一律不收。
-- 在册影像页数上限按一/二/三档取 5/12/30 页；本档案最多承接 50 页，越过三档封顶仍按三档论但不得越承接上限。
INSERT IGNORE INTO t_marr_vol_doc (id, site_no, site_name, site_type, road_name, img_max, th1_max, th2_max, th3_max, status, del_flag, create_by, create_time)
VALUES (0, 'JH0001', '江原县民政局婚姻管理处结婚卷宗（临川案宗）', '结婚', '江原省—江原市—临川县', 50.00, 5.00, 12.00, 30.00, 0, 0, 'seed', NOW()),
       (1, 'LH0001', '江原县民政局婚姻管理处离婚卷宗（已封卷）', '离婚', '江原省—江原市—临川县', 50.00, 5.00, 12.00, 30.00, 1, 0, 'seed', NOW());

-- 初始查档授权：本人一份(名下 JH0001)、律师一份(受托 LH0001，别卷不许列出来)、机关一份(临川县辖内全看)。
INSERT IGNORE INTO t_marr_archive_grant (id, grant_no, viewer_type, viewer_name, id_no, scope_road, site_no, matter, letter_no, status, del_flag, create_by, create_time)
VALUES (0, 'GR0001', 1, '周兰', '360101199001010000', NULL, 'JH0001', '本人调取婚姻登记档案', NULL, 0, 0, 'seed', NOW()),
       (1, 'GR0002', 3, '沈慎之律师', '136012016001', NULL, 'LH0001', '代理离婚后财产纠纷案调档', '(2016)法民委字第07号', 0, 0, 'seed', NOW()),
       (2, 'GR0003', 2, '江原市民政局', NULL, '江原省—江原市—临川县', NULL, '市局抽查婚姻登记卷宗', '江民政函〔2026〕11号', 0, 0, 'seed', NOW());

-- 到期催办扫描岗：这一轮催办只由定时扫描叫醒，页面不留补一条、改一条的入口。
-- cron 只负责叫醒；该不该出手由服务层一处口径把守（只认每天 09:00-11:30、15:00-17:30 两格），此处不重复编码时段。
-- concurrent=1 禁并发，免得两轮并排把同一条催两遍。
INSERT IGNORE INTO t_sys_quartz_job (id, job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
VALUES (332182389491109889, '冷静期与领证期到期催办扫描', 'MARR', 'marrDueTask.scan()', '0 0,30 9,10,11,15,16,17 * * ?', '2', '1', 0, 'seed', NOW(), '到期催办：格内每半小时一轮，只由定时扫描叫醒');
