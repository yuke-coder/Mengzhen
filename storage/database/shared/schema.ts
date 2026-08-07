/**
 * 「梦枕」数据库 Schema 定义
 *
 * 表结构：
 * - users: 用户账号表
 * - sessions: 登录会话表
 * - user_profiles: 用户资料扩展表
 * - audios: 音频资源表
 */

// ============================================================
// 1. users — 用户账号表
// ============================================================
export interface User {
  id: string; // UUID 主键
  username: string; // 用户名（唯一，3-20字符，字母数字下划线）
  password_hash: string; // bcrypt 哈希密码
  nickname: string; // 显示昵称
  avatar_url: string | null; // 头像 URL（Supabase Storage 路径）
  created_at: string; // 注册时间
  updated_at: string; // 更新时间
}

// ============================================================
// 2. sessions — 登录会话表
// ============================================================
export interface Session {
  id: string; // UUID 主键
  user_id: string; // 关联 users.id
  token: string; // 随机 session token（写入 httpOnly cookie）
  created_at: string; // 创建时间
  expires_at: string; // 过期时间（默认 1 年）
}

// ============================================================
// 3. user_profiles — 用户资料扩展表
// ============================================================
export interface UserProfile {
  id: string; // UUID 主键
  user_id: string; // 关联 users.id（一对一）
  nickname: string; // 显示昵称
  avatar_url: string | null; // 头像 URL（Supabase Storage 路径）
  gender: "male" | "female" | "other" | null; // 性别
  birthday: string | null; // 生日 (YYYY-MM-DD)
  location: string | null; // 所在地（JSON 字符串：{planet,country,province,city,district}）
  bio: string | null; // 个人简介
  signature: string | null; // 个性签名
  username_change_count: number; // 自然月内用户名修改次数（重置逻辑在应用层）
  username_change_reset_at: string | null; // 上次计数重置时间
  created_at: string;
  updated_at: string;
}

// ============================================================
// 4. audios — 音频资源表
// ============================================================
export interface Audio {
  id: string; // UUID 主键
  user_id: string; // 关联 users.id（谁上传的）
  title: string; // 音频标题
  file_url: string; // 文件存储路径（Supabase Storage 公开 URL）
  file_key: string; // 文件存储键（Supabase Storage 路径，如 audios/userId/xxx.mp3）
  file_name: string; // 原始文件名
  file_size: number; // 文件大小（字节）
  duration: number; // 时长（秒，0 表示未知）
  mime_type: string; // MIME 类型（audio/mp3, audio/wav 等）
  sort_order: number; // 播放排序（越小越靠前）
  is_active: boolean; // 是否启用（软删除用）
  library_saved_at: string | null; // 用户手动存入音频库的时间，null 表示仅供任务使用
  created_at: string;
  updated_at: string;
}

// ============================================================
// SQL 建表语句（用于 Supabase SQL Editor 或 Migration）
// ============================================================

// ============================================================
// 5. feedbacks — 用户反馈表
// ============================================================
export interface Feedback {
  id: string; // UUID 主键
  user_id: string; // 关联 users.id
  type: "bug" | "suggestion"; // 反馈类型
  category: string | null; // 具体反馈场景
  content: string; // 反馈内容
  contact: string | null; // 联系方式
  images: string[] | null; // 图片 URL 列表
  status: 1 | 2 | 3; // 尚未受理 / 受理中 / 受理完毕
  op_group: string | null; // 受理客服组
  op_name: string | null; // 受理客服
  processed_at: string | null;
  created_at: string;
  updated_at: string;
}

export interface FeedbackReply {
  id: string;
  feedback_id: string;
  user_id: string | null;
  sender_role: "user" | "support";
  sender: string;
  content: string;
  images: string[] | null;
  created_at: string;
}

export const CREATE_USERS_TABLE_SQL = `
CREATE TABLE IF NOT EXISTS public.users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  username VARCHAR(20) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  nickname VARCHAR(50) NOT NULL DEFAULT '',
  avatar_url TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 索引：按用户名查询
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username ON public.users(username);
`;

export const CREATE_SESSIONS_TABLE_SQL = `
CREATE TABLE IF NOT EXISTS public.sessions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
  token TEXT UNIQUE NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  expires_at TIMESTAMPTZ NOT NULL
);

-- 索引：按 token 快速查找
CREATE UNIQUE INDEX IF NOT EXISTS idx_sessions_token ON public.sessions(token);
-- 索引：清理过期会话
CREATE INDEX IF NOT EXISTS idx_sessions_expires_at ON public.sessions(expires_at);
`;

export const CREATE_USER_PROFILES_TABLE_SQL = `
CREATE TABLE IF NOT EXISTS public.user_profiles (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID UNIQUE NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
  nickname VARCHAR(50) NOT NULL DEFAULT '',
  avatar_url TEXT,
  gender VARCHAR(10) CHECK (gender IN ('male', 'female', 'other')),
  birthday DATE,
  location JSONB,
  bio TEXT,
  signature TEXT,
  username_change_count INTEGER NOT NULL DEFAULT 0,
  username_change_reset_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 索引
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_profiles_user_id ON public.user_profiles(user_id);
`;

export const CREATE_AUDIOS_TABLE_SQL = `
CREATE TABLE IF NOT EXISTS public.audios (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
  title VARCHAR(200) NOT NULL,
  file_url TEXT NOT NULL,
  file_key TEXT,
  file_name VARCHAR(255) NOT NULL,
  file_size INTEGER NOT NULL DEFAULT 0,
  duration REAL NOT NULL DEFAULT 0,
  mime_type VARCHAR(50) NOT NULL DEFAULT 'audio/mpeg',
  sort_order INTEGER NOT NULL DEFAULT 0,
  is_active BOOLEAN NOT NULL DEFAULT true,
  library_saved_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.audios
  ADD COLUMN IF NOT EXISTS library_saved_at TIMESTAMPTZ;

-- 索引：按用户查询自己的音频
CREATE INDEX IF NOT EXISTS idx_audios_user_id ON public.audios(user_id);
-- 索引：按排序查询
CREATE INDEX IF NOT EXISTS idx_audios_sort_order ON public.audios(user_id, sort_order);
-- 唯一约束：同一用户的任务资源路径不重复
DROP INDEX IF EXISTS public.idx_audios_file_key;
CREATE UNIQUE INDEX IF NOT EXISTS idx_audios_user_file_key
  ON public.audios(user_id, file_key)
  WHERE file_key IS NOT NULL;
-- 索引：只扫描已存入音频库的资源
CREATE INDEX IF NOT EXISTS idx_audios_library
  ON public.audios(user_id, library_saved_at DESC)
  WHERE library_saved_at IS NOT NULL;
`;

export const CREATE_FEEDBACKS_TABLE_SQL = `
CREATE TABLE IF NOT EXISTS public.feedbacks (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
  type VARCHAR(20) NOT NULL CHECK (type IN ('bug', 'suggestion')),
  category VARCHAR(80),
  content TEXT NOT NULL,
  contact TEXT,
  images TEXT[],
  status SMALLINT NOT NULL DEFAULT 1 CHECK (status BETWEEN 1 AND 3),
  op_group TEXT,
  op_name TEXT,
  processed_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.feedbacks
  ADD COLUMN IF NOT EXISTS category VARCHAR(80),
  ADD COLUMN IF NOT EXISTS status SMALLINT NOT NULL DEFAULT 1,
  ADD COLUMN IF NOT EXISTS op_group TEXT,
  ADD COLUMN IF NOT EXISTS op_name TEXT,
  ADD COLUMN IF NOT EXISTS processed_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

CREATE TABLE IF NOT EXISTS public.feedback_replies (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  feedback_id UUID NOT NULL REFERENCES public.feedbacks(id) ON DELETE CASCADE,
  user_id UUID REFERENCES public.users(id) ON DELETE SET NULL,
  sender_role VARCHAR(20) NOT NULL DEFAULT 'user'
    CHECK (sender_role IN ('user', 'support')),
  sender VARCHAR(80) NOT NULL,
  content TEXT NOT NULL,
  images TEXT[],
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_feedbacks_user_created
  ON public.feedbacks(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_feedback_replies_feedback_created
  ON public.feedback_replies(feedback_id, created_at ASC);
`;

/**
 * 启用 RLS（行级安全策略）
 * 所有表仅允许用户操作自己的数据
 */
export const ENABLE_RLS_SQL = `
-- 启用 RLS
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.audios ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.feedbacks ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.feedback_replies ENABLE ROW LEVEL SECURITY;

-- users 表：所有人可注册（INSERT），仅自己可读写
CREATE POLICY "Users can view own data" ON public.users FOR SELECT USING (true);
CREATE POLICY "Users can insert new accounts" ON public.users FOR INSERT WITH CHECK (true);
CREATE POLICY "Users can update own data" ON public.users FOR UPDATE USING (auth.uid() = id);

-- sessions 表：通过 API key 操作，不限制（service role 绕过 RLS）
-- 但启用 RLS 防止未认证访问

-- user_profiles 表：登录后可读写自己的资料
CREATE POLICY "Profiles can view own data" ON public.user_profiles FOR SELECT USING (true);
CREATE POLICY "Profiles can insert own profile" ON public.user_profiles FOR INSERT WITH CHECK (true);
CREATE POLICY "Profiles can update own data" ON public.user_profiles FOR UPDATE USING (true);

-- audios 表：用户只能操作自己的音频
CREATE POLICY "Audios can view own" ON public.audios FOR SELECT USING (true);
CREATE POLICY "Audios can insert own" ON public.audios FOR INSERT WITH CHECK (true);
CREATE POLICY "Audios can update own" ON public.audios FOR UPDATE USING (true);
CREATE POLICY "Audios can delete own" ON public.audios FOR DELETE USING (true);

-- feedbacks / feedback_replies 仅由服务端 service role 访问；
-- 用户归属校验在 API 会话层完成，不开放匿名直连策略。
`;
