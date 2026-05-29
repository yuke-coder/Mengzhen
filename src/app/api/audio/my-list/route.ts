import { NextResponse } from "next/server";
import { getSupabaseClient } from "@/storage/database/supabase-client";
import { cookies } from "next/headers";

const SESSION_COOKIE_NAME = "mindmap_session";

async function getUserIdFromSession(): Promise<string | null> {
  try {
    const cookieStore = await cookies();
    const sessionToken = cookieStore.get(SESSION_COOKIE_NAME)?.value;
    if (!sessionToken) return null;
    const client = getSupabaseClient();
    if (!client) return null;
    const { data: session } = await client
      .from("sessions")
      .select("user_id, expires_at")
      .eq("token", sessionToken)
      .maybeSingle();
    if (!session) return null;
    if (new Date(session.expires_at) < new Date()) return null;
    return session.user_id;
  } catch {
    return null;
  }
}

export async function GET() {
  const userId = await getUserIdFromSession();
  if (!userId) return NextResponse.json({ error: "请先登录" }, { status: 401 });

  const supabase = getSupabaseClient();
  if (!supabase) return NextResponse.json({ error: "服务器配置错误" }, { status: 500 });

  // 从 audios 表读取播放记录
  const { data, error } = await supabase
    .from("audios")
    .select("id, title, file_url, file_key, file_name, file_size, duration, mime_type, created_at")
    .eq("user_id", userId)
    .eq("is_active", true)
    .order("created_at", { ascending: false });

  if (error) {
    console.error("[Audio MyList] 查询失败:", error);
    return NextResponse.json({ error: "获取历史记录失败" }, { status: 500 });
  }

  return NextResponse.json({ success: true, audios: data || [] });
}
