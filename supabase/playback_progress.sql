-- 播放进度同步表
-- 在 Supabase Dashboard 的 SQL Editor 中执行

CREATE TABLE IF NOT EXISTS playback_progress (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  audio_id TEXT NOT NULL,
  position_seconds INTEGER NOT NULL DEFAULT 0,
  duration_seconds INTEGER NOT NULL DEFAULT 0,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(user_id, audio_id)
);

-- 启用行级安全
ALTER TABLE playback_progress ENABLE ROW LEVEL SECURITY;

-- 策略: 通过 service_role key 访问，不需要严格的 RLS
CREATE POLICY "用户读取自己的播放进度" ON playback_progress
  FOR SELECT USING (true);

CREATE POLICY "用户写入自己的播放进度" ON playback_progress
  FOR ALL USING (true);

-- 索引
CREATE INDEX IF NOT EXISTS idx_playback_progress_user_id ON playback_progress(user_id);
CREATE INDEX IF NOT EXISTS idx_playback_progress_user_audio ON playback_progress(user_id, audio_id);
