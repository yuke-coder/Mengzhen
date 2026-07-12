import { createClient, SupabaseClient } from "@supabase/supabase-js";

let supabaseInstance: SupabaseClient | null = null;

/**
 * 获取 Supabase 客户端实例（单例模式）
 * 必须设置环境变量 SUPABASE_URL 和 SUPABASE_SERVICE_ROLE_KEY
 */
export function getSupabaseClient(): SupabaseClient | null {
  const url = process.env.SUPABASE_URL;
  const key = process.env.SUPABASE_SERVICE_ROLE_KEY;

  if (!url || !key) {
    console.error("========================================");
    console.error("[Supabase] 缺少必要的环境变量！");
    console.error("[Supabase] SUPABASE_URL:", url ? "✅ 已设置" : "❌ 未设置");
    console.error("[Supabase] SUPABASE_SERVICE_ROLE_KEY:", key ? "✅ 已设置" : "❌ 未设置");
    console.error("[Supabase] 当前 NODE_ENV:", process.env.NODE_ENV);
    console.error("[Supabase] 所有环境变量键:", Object.keys(process.env).sort().join(', '));
    console.error("========================================");
    return null;
  }

  if (!supabaseInstance) {
    try {
      console.log("[Supabase] 正在初始化客户端...");
      supabaseInstance = createClient(url, key, {
        auth: { autoRefreshToken: false, persistSession: false },
      });
      console.log("[Supabase] ✅ 客户端初始化成功");
    } catch (error) {
      console.error("[Supabase] ❌ 初始化失败:", error);
      return null;
    }
  }

  return supabaseInstance;
}

/**
 * 检查 Supabase 是否已正确配置
 */
export function isSupabaseConfigured(): boolean {
  const configured = !!(process.env.SUPABASE_URL && process.env.SUPABASE_SERVICE_ROLE_KEY);
  if (!configured) {
    console.error("[Supabase] isSupabaseConfigured: ❌ 未配置");
  }
  return configured;
}
