import { NextRequest, NextResponse } from "next/server";
import { getSupabaseClient } from "@/storage/database/supabase-client";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function GET(request: NextRequest) {
  const { searchParams } = new URL(request.url);
  const key = searchParams.get("key");

  // 安全检查：只允许 audios 存储桶的文件（防止目录遍历攻击）
  if (!key || key.includes("..") || !key.startsWith("audios/")) {
    return NextResponse.json({ error: "无效的文件路径" }, { status: 400 });
  }

  // file_key 即存储路径，直接使用（如 "audios/userId/timestamp_filename.mp3"）
  const fileName = key;

  const supabase = getSupabaseClient();
  if (!supabase) {
    console.error("[Audio Proxy] Supabase 客户端未初始化，缺少 SUPABASE_URL 或 SUPABASE_SERVICE_ROLE_KEY");
    return NextResponse.json({ error: "服务器配置错误" }, { status: 500 });
  }

  try {
    console.log("[Audio Proxy] 下载文件:", fileName);
    
    const { data, error } = await supabase.storage
      .from("audios")
      .download(fileName);

    if (error || !data) {
      console.error("[Audio Proxy] 下载失败:", error?.message || "无数据", "fileName:", fileName);
      return NextResponse.json({ error: "文件下载失败" }, { status: 502 });
    }

    // 获取文件 MIME 类型
    const contentType = data.type || "audio/mpeg";
    const buffer = await data.arrayBuffer();

    return new NextResponse(buffer, {
      status: 200,
      headers: {
        "Content-Type": contentType,
        "Content-Length": buffer.byteLength.toString(),
        "Cache-Control": "public, max-age=86400",
      },
    });
  } catch (err) {
    console.error("[Audio Proxy] 异常:", err);
    return NextResponse.json({ error: "服务器内部错误" }, { status: 500 });
  }
}
