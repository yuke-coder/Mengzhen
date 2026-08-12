-- 个人主页背景图：与 Android / Web 资料页共用同一个 profile 字段。
ALTER TABLE public.user_profiles
  ADD COLUMN IF NOT EXISTS background_url TEXT;

COMMENT ON COLUMN public.user_profiles.background_url IS
  'User profile background image public URL';

-- 头像接口已经使用 avatars 桶，背景图沿用同一个公开桶，避免前端维护两套存储配置。
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
  'avatars',
  'avatars',
  true,
  10485760,
  ARRAY['image/jpeg', 'image/png', 'image/gif', 'image/webp']::text[]
)
ON CONFLICT (id) DO UPDATE SET
  public = EXCLUDED.public,
  file_size_limit = EXCLUDED.file_size_limit,
  allowed_mime_types = EXCLUDED.allowed_mime_types;
