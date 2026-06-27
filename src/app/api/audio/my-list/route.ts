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
    .from("audio_files")
    .select("id, path, name, size, mime_type, metadata, created_at")
    .eq("user_id", user.id)
    .order("created_at", { ascending: false });

  if (error) {
    console.error("[Audio MyList] 查询失败:", error);
    return NextResponse.json({ error: "获取我的音频失败" }, { status: 500 });
  }

  const audios = (data || []).map((audio) => {
    const { data: urlData } = supabase.storage.from("audios").getPublicUrl(audio.path);
    const metadata = audio.metadata as { duration?: number } | null;
    return {
      id: audio.id,
      title: audio.name,
      file_url: urlData?.publicUrl || "",
      file_key: audio.path,
      file_name: audio.name,
      file_size: audio.size || 0,
      duration: metadata?.duration || 0,
      mime_type: audio.mime_type,
      created_at: audio.created_at,
    };
  });

  return NextResponse.json({ success: true, audios });
}
