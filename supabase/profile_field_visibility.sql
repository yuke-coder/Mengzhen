-- 生日 / 地区的「保密」可见性字段
--
-- 背景：
-- Android 端资料编辑页有「不展示生日 / 不展示地区」开关（喜马拉雅 9.5.4.8 真本
-- MyDetailFragment 的 publicGender / 生日 hide 开关），但梦枕 `user_profiles` 里没有任何可见性字段，
-- `/api/profile` 的 PUT 也不接受这类参数，导致该开关只能存在手机本地 —— 换设备/重装就丢，
-- Web 端也完全不认，用户以为「已保密」其实只在那一台手机上生效。
--
-- 性别不需要这两个列：它复用 Web 端既有的 `gender = 'secret'` 编码
-- （见 supabase/profile_gender_secret.sql）。
--
-- 语义（与 Web 端「保密」一致）：
--   真实值照常存在 birthday / location 里，本列只表示「对外不展示」。
--   不采用「清空值」来模拟隐藏 —— 那会真的丢数据且不可还原。
--
-- 默认 FALSE = 照常展示，因此对既有数据是纯增量、无需回填。
ALTER TABLE public.user_profiles
  ADD COLUMN IF NOT EXISTS hide_birthday BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS hide_region BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN public.user_profiles.hide_birthday IS
  '生日对外是否隐藏（Android「不展示生日」/ Web「保密」）；true 时对外不展示，值本身保留';
COMMENT ON COLUMN public.user_profiles.hide_region IS
  '地区对外是否隐藏（Android「不展示地区」/ Web「保密」）；true 时对外不展示，值本身保留';
