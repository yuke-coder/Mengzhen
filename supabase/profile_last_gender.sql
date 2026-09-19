-- 「取消保密后回退到开启保密前的选项」所需的性别真值留存。
--
-- ## 为什么需要单独一列
--
-- 梦枕用 gender = 'secret' 表达「保密」（Web 端一开始就这么存，见
-- supabase/profile_gender_secret.sql，Android 端已统一到同一编码）。
-- 这个编码的副作用是：**保密会把真值覆写掉**。
-- 用户先选「男」，再勾「保密」，gender 就变成 'secret'，「男」这个信息在后端消失了；
-- 等他取消保密时，后端已经不知道该回退到男还是女。
--
-- 真本 Android 的做法与此相反：MyDetailInfo 上保留真实的 gender，
-- 「不展示」是另一个独立布尔 publicGender（MyDetailFragment 的 CheckBox 回调
-- `setPublicGender(bl2 ^ true)`）。即「保留真值 + 单独开关」。
-- 这里补的 last_gender 就是那一份「真值」。
--
-- ## 维护方式
--
-- 由 PUT /api/profile 自动维护：只有当本次请求把 gender 写成真实值
-- （male / female / other）时才同步 last_gender；写成 'secret' 或清空时保持不动。
-- 客户端**不需要**也不应该传这个字段 —— 这样 Web 端选性别时也顺便把真值记下了。
--
-- 约束与 gender 一致，但**不含 'secret'**：这一列只存真值。

ALTER TABLE public.user_profiles
  ADD COLUMN IF NOT EXISTS last_gender TEXT;

COMMENT ON COLUMN public.user_profiles.last_gender IS
  '「保密」前的真实性别（male/female/other）。由 PUT /api/profile 在 gender 被写成真值时自动维护，用于取消保密时回退。';

ALTER TABLE public.user_profiles DROP CONSTRAINT IF EXISTS user_profiles_last_gender_check;
ALTER TABLE public.user_profiles
  ADD CONSTRAINT user_profiles_last_gender_check
  CHECK (last_gender IS NULL OR last_gender IN ('male', 'female', 'other'));

-- 回填：gender 还是真值的用户说明从没开过保密，当前值即真值。
-- （gender = 'secret' 的用户无法反推，只能等他们下次选性别时自然回填。）
UPDATE public.user_profiles
   SET last_gender = gender
 WHERE last_gender IS NULL
   AND gender IN ('male', 'female', 'other');
