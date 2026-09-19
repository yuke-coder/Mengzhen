-- 性别「保密」值（Web 端「保密」/ Android 端「不展示」）
--
-- 背景：
-- user_profiles_gender_check 原本是 CHECK (gender IN ('male','female','other'))，
-- 但 Web 端网页设置页的性别三选一用的是 ["male","female","secret"]
-- （见 src/components/ximalaya-web-settings-page.tsx），
-- src/lib/auth-context.tsx 的类型也是 'male' | 'female' | 'secret'，
-- src/components/user-menu.tsx 更是用 `gender !== 'secret'` 来决定是否隐藏性别行，
-- /api/avatar 的 DEFAULT_AVATARS 也带了 secret 键。
--
-- 即：前端一直按 'secret' 表达「保密」，数据库却不接受该值，
-- 导致勾「保密」时写入被拒（Postgres 23514），网页与 Android 两端的保密/不展示双双失效。
--
-- 本迁移把约束放宽为前端实际使用的取值集合。属于纯放宽（原值仍全部合法），
-- 不影响既有数据，可安全重复执行。
ALTER TABLE public.user_profiles
  DROP CONSTRAINT IF EXISTS user_profiles_gender_check;

ALTER TABLE public.user_profiles
  ADD CONSTRAINT user_profiles_gender_check
  CHECK (gender IS NULL OR gender IN ('male', 'female', 'secret', 'other'));
