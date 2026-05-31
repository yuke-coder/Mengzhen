import { NextRequest, NextResponse } from "next/server";
import { getSupabaseClient } from "@/storage/database/supabase-client";
import { getAuthUser } from "@/lib/auth";

export async function GET(request: NextRequest) {
  const user = await getAuthUser();
  if (!user) return NextResponse.json({ error: "请先登录" }, { status: 401 });

  const supabase = getSupabaseClient();
  if (!supabase) return NextResponse.json({ error: "服务器配置错误" }, { status: 500 });

  const { searchParams } = new URL(request.url);
  const fileKey = searchParams.get("fileKey");
  if (!fileKey) return NextResponse.json({ error: "缺少 fileKey" }, { status: 400 });

  const { data, error } = await supabase
    .from("audio_files")
    .select("id, user_id, path, name, size, mime_type, metadata, created_at")
    .eq("user_id", user.id)
    .eq("path", fileKey)
    .maybeSingle();

  if (error || !data) {
    return NextResponse.json({ error: "音频不存在" }, { status: 404 });
  }

  const { data: urlData } = supabase.storage.from("audios").getPublicUrl(fileKey);

  return NextResponse.json({
    success: true,
    audio: {
      id: data.id,
      name: data.name,
      path: data.path,
      size: data.size,
      mime_type: data.mime_type,
      metadata: data.metadata,
      created_at: data.created_at,
      serverUrl: urlData?.publicUrl || `/api/audio/proxy?key=${encodeURIComponent(fileKey)}`,
    }
  });
}
