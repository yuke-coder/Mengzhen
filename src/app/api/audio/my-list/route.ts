import { NextResponse } from "next/server";
import { getSupabaseClient } from "@/lib/supabase-client";
import { getAuthUser } from "@/lib/auth";

export const dynamic = "force-dynamic";

export async function GET() {
  const user = await getAuthUser();
  if (!user) return NextResponse.json({ error: "请先登录" }, { status: 401 });

  const supabase = getSupabaseClient();
  if (!supabase) return NextResponse.json({ error: "服务器配置错误" }, { status: 500 });

  const { data, error } = await supabase
    .from("audios")
    .select("id, title, file_url, file_key, file_name, file_size, duration, mime_type, library_saved_at")
    .eq("user_id", user.id)
    .eq("is_active", true)
    .not("library_saved_at", "is", null)
    .order("library_saved_at", { ascending: false });

  if (error) {
    console.error("[Audio MyList] 查询失败:", error);
    return NextResponse.json({ error: "获取我的音频失败" }, { status: 500 });
  }

  const audios = (data || []).map(audio => ({
    id: audio.id,
    title: audio.title,
    file_url: audio.file_url,
    file_key: audio.file_key,
    file_name: audio.file_name,
    file_size: audio.file_size || 0,
    duration: audio.duration || 0,
    mime_type: audio.mime_type,
    created_at: audio.library_saved_at,
  }));

  return NextResponse.json({ success: true, audios });
}
