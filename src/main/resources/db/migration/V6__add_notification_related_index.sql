-- 删除公告/任务/资料时按 related_type + related_id 清理关联通知，巡检任务也会按该组合扫描；
-- 缺少索引会导致全表扫描并放大 InnoDB 锁范围，因此补充联合索引。
CREATE INDEX `idx_notification_related` ON `notification` (`related_type`, `related_id`);
