-- 资料页星座字段（与生日、地区设置一起使用）
ALTER TABLE public.user_profiles
  ADD COLUMN IF NOT EXISTS constellation VARCHAR(20);
