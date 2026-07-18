import { NextRequest, NextResponse } from "next/server";
import { getSupabaseClient } from "@/lib/supabase-client";
import { getAuthUser } from "@/lib/auth";

export async function POST(request: NextRequest) {
  try {
    const user = await getAuthUser();
    if (!user) {
      return NextResponse.json({ success: false, error: "请先登录" }, { status: 401 });
    }

    const body = await request.json();
    const { fileKey } = body;

    if (typeof fileKey !== "string" || !fileKey) {
      return NextResponse.json({ success: false, error: "缺少音频资源标识" }, { status: 400 });
    }

    const supabase = getSupabaseClient();

    if (!supabase) {
      return NextResponse.json({ success: false, error: "服务器配置错误" }, { status: 500 });
    }

    const { data: audio, error: findError } = await supabase
      .from("audios")
      .select("id, library_saved_at")
      .eq("user_id", user.id)
      .eq("file_key", fileKey)
      .maybeSingle();

    if (findError) {
      console.error("[save-to-library] 查询失败:", findError);
      return NextResponse.json({ success: false, error: "查询音频资源失败，请重试" }, { status: 500 });
    }
    if (!audio) {
      return NextResponse.json({ success: false, error: "音频资源不存在或不属于当前用户" }, { status: 404 });
    }
    if (audio.library_saved_at) {
      return NextResponse.json({
        success: true,
        message: "已在音频库中",
        library_saved_at: audio.library_saved_at,
      });
    }

    const savedAt = new Date().toISOString();
    const { data: savedAudio, error: updateError } = await supabase
      .from("audios")
      .update({ library_saved_at: savedAt, updated_at: savedAt })
      .eq("id", audio.id)
      .eq("user_id", user.id)
      .select("library_saved_at")
      .maybeSingle();

    if (updateError || !savedAudio) {
      console.error("[save-to-library] 保存失败:", updateError);
      return NextResponse.json({ success: false, error: "存入音频库失败，请重试" }, { status: 500 });
    }

    return NextResponse.json({ success: true, library_saved_at: savedAudio.library_saved_at });
  } catch (error) {
    console.error("[save-to-library] 异常:", error);
    return NextResponse.json(
      { success: false, error: "存入音频库失败，请重试" },
      { status: 500 }
    );
  }
}
