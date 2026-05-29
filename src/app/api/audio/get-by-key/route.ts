import { NextRequest, NextResponse } from "next/server";
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

export async function GET(request: NextRequest) {
  const userId = await getUserIdFromSession();
  if (!userId) return NextResponse.json({ error: "请先登录" }, { status: 401 });

  const supabase = getSupabaseClient();
  if (!supabase) return NextResponse.json({ error: "服务器配置错误" }, { status: 500 });

  const { searchParams } = new URL(request.url);
  const fileKey = searchParams.get("fileKey");
  if (!fileKey) return NextResponse.json({ error: "缺少 fileKey" }, { status: 400 });

  // 查找 audio_files 中对应文件
  const { data, error } = await supabase
    .from("audio_files")
    .select("id, user_id, path, name, size, mime_type, metadata, created_at")
    .eq("user_id", userId)
    .eq("path", fileKey)
    .maybeSingle();

  if (error || !data) {
    return NextResponse.json({ error: "音频不存在" }, { status: 404 });
  }

  // 构建可访问的 URL
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
