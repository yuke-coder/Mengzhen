import { NextRequest, NextResponse } from "next/server";
import { getSupabaseClient } from "@/lib/supabase-client";
import { getAuthUser } from "@/lib/auth";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

/**
 * 生成 Supabase Storage 签名 URL，供原生 MediaPlayer 直接访问
 * GET /api/audio/signed-url?key=audios/xxx/yyy.mp3
 */
export async function GET(request: NextRequest) {
  const user = await getAuthUser();
  if (!user) {
    return NextResponse.json({ error: "请先登录" }, { status: 401 });
  }

  const { searchParams } = new URL(request.url);
  const key = searchParams.get("key");

  if (!key || key.includes("..") || !key.startsWith("audios/")) {
    return NextResponse.json({ error: "无效的文件路径" }, { status: 400 });
  }

  const pathParts = key.split("/");
  const pathUserId = pathParts.length >= 2 ? pathParts[1] : null;
  if (!pathUserId || pathUserId !== user.id) {
    return NextResponse.json({ error: "无权访问此文件" }, { status: 403 });
  }

  const supabase = getSupabaseClient();
  if (!supabase) {
    return NextResponse.json({ error: "服务器配置错误" }, { status: 500 });
  }

  try {
    // 生成 2 小时有效期的签名 URL
    const { data, error } = await supabase.storage
      .from("audios")
      .createSignedUrl(key, 7200);

    if (error || !data?.signedUrl) {
      console.error("[Signed URL] 生成失败:", error?.message, "key:", key);
      return NextResponse.json({ error: "生成签名 URL 失败" }, { status: 500 });
    }

    return NextResponse.json({ signedUrl: data.signedUrl });
  } catch (err) {
    console.error("[Signed URL] 异常:", err);
    return NextResponse.json({ error: "服务器内部错误" }, { status: 500 });
  }
}
