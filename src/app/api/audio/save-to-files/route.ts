import { NextRequest, NextResponse } from "next/server";
import { getSupabaseClient } from "@/storage/database/supabase-client";

export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    const { fileKey, name, size, mime_type } = body;

    if (!fileKey || !name) {
      return NextResponse.json({ success: false, error: "缺少必要参数" }, { status: 400 });
    }

    const supabase = getSupabaseClient();

    if (!supabase) {
      return NextResponse.json({ success: false, error: "服务器配置错误" }, { status: 500 });
    }

    // 从路径中提取 user_id: audios/userId/xxx.mp3
    const pathParts = fileKey.split("/");
    const userId = pathParts.length >= 2 ? pathParts[1] : null;

    if (!userId) {
      return NextResponse.json({ success: false, error: "无法识别用户" }, { status: 400 });
    }

    // 写入 audio_files 表
    const { error } = await supabase.from("audio_files").insert({
      user_id: userId,
      bucket_id: "audios",
      path: fileKey,
      name,
      size: size || 0,
      mime_type: mime_type || "audio/mpeg",
    });

    if (error) {
      console.error("[save-to-files] 插入失败:", error);
      // 可能已经存在，尝试忽略
      if (error.code === "23505") {
        return NextResponse.json({ success: true, message: "已存在" });
      }
      return NextResponse.json({ success: false, error: error.message }, { status: 500 });
    }

    return NextResponse.json({ success: true });
  } catch (err) {
    console.error("[save-to-files] 异常:", err);
    return NextResponse.json(
      { success: false, error: err instanceof Error ? err.message : "服务器错误" },
      { status: 500 }
    );
  }
}
