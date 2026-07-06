import { NextRequest, NextResponse } from "next/server";
import { getSupabaseClient } from "@/lib/supabase-client";
import { getAuthUser } from "@/lib/auth";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";
export const maxDuration = 60;

const ALLOWED_EXTENSIONS = [".mp3", ".wav", ".ogg", ".m4a", ".flac", ".aac"];

function translateStorageError(rawMessage: string): string {
  const msg = (rawMessage || "").toLowerCase();
  if (msg.includes("invalid content type") || msg.includes("mime") || msg.includes("content-type")) {
    return "不支持的音频格式，请上传 MP3 / WAV / OGG / M4A / FLAC 等格式";
  }
  if (msg.includes("not found") || msg.includes("bucket") && msg.includes("not")) {
    return "存储空间未配置，请联系管理员";
  }
  if (msg.includes("unauthorized") || msg.includes("permission") || msg.includes("forbidden")) {
    return "上传权限不足，请重新登录后再试";
  }
  if (msg.includes("network") || msg.includes("timeout") || msg.includes("econn")) {
    return "网络连接异常，请检查网络后重试";
  }
  return rawMessage || "上传失败，请重试";
}

export async function POST(request: NextRequest) {
  try {
    const authUser = await getAuthUser();
    if (!authUser) {
      return NextResponse.json(
        { success: false, error: "请先登录" },
        { status: 401 }
      );
    }
    const userId = authUser.id;

    const url = new URL(request.url);
    const saveToFiles = url.searchParams.get("save_to_files") === "true";

    const formData = await request.formData();
    const file = formData.get("audio");
    if (!(file instanceof File)) {
      return NextResponse.json(
        { success: false, error: "未找到音频文件字段" },
        { status: 400 }
      );
    }

    const filename = file.name || "audio";
    const fileType = file.type || "";
    const fileSize = file.size;

    const ext = "." + filename.split(".").pop()?.toLowerCase();
    const typeOk = fileType.startsWith("audio/") || fileType === "";
    const extOk = ALLOWED_EXTENSIONS.includes(ext);
    if (!typeOk && !extOk) {
      return NextResponse.json(
        { success: false, error: `不支持的音频格式，请上传 ${ALLOWED_EXTENSIONS.join(", ")} 文件` },
        { status: 400 }
      );
    }

    const supabase = getSupabaseClient();
    if (!supabase) {
      return NextResponse.json(
        { success: false, error: "存储服务未配置" },
        { status: 503 }
      );
    }

    const timestamp = Date.now();
    const randomStr = Math.random().toString(36).substring(2, 10);
    const fileExt = ext || ".mp3";
    const fileName = `audios/${userId}/${timestamp}_${randomStr}${fileExt}`;

    console.log(`[Audio Upload] 上传文件: ${filename}, 大小: ${fileSize} 字节, 目标路径: ${fileName}`);

    const { error: uploadError } = await supabase.storage
      .from("audios")
      .upload(fileName, file, {
        contentType: fileType || "audio/mpeg",
        upsert: true,
      });

    if (uploadError) {
      console.error("[Audio Upload] 存储上传失败:", uploadError);
      const userMsg = translateStorageError(uploadError.message || "");
      return NextResponse.json(
        { success: false, error: userMsg },
        { status: 500 }
      );
    }

    const { data: urlData } = supabase.storage.from("audios").getPublicUrl(fileName);
    const audioUrl = urlData.publicUrl;

    const { error: dbError } = await supabase.from("audios").insert({
      user_id: userId,
      title: filename.replace(/\.[^/.]+$/, ""),
      file_url: audioUrl,
      file_key: fileName,
      file_name: filename,
      file_size: fileSize,
      duration: 0,
      mime_type: fileType || `audio/${fileExt.slice(1)}`,
      sort_order: 0,
      is_active: true,
    });

    if (dbError) {
      console.error("[Audio Upload] audios 表写入失败:", dbError);
      await supabase.storage.from("audios").remove([fileName]);
      return NextResponse.json(
        { success: false, error: "音频记录保存失败，请重试" },
        { status: 500 }
      );
    }

    if (saveToFiles) {
      const { data: bucketData } = await supabase.storage.getBucket("audios");
      const bucketId = bucketData?.id || "00000000-0000-0000-0000-000000000000";

      const { error: fileError } = await supabase.from("audio_files").insert({
        user_id: userId,
        bucket_id: bucketId,
        path: fileName,
        name: filename,
        size: fileSize,
        mime_type: fileType || `audio/${fileExt.slice(1)}`,
      });

      if (fileError) {
        console.error("[Audio Upload] audio_files 表写入失败:", fileError);
      }
    }

    return NextResponse.json({
      success: true,
      message: "音频上传成功",
      audio_url: audioUrl,
      file_key: fileName,
      file_name: filename,
      file_size: fileSize,
    });
  } catch (error) {
    console.error("[Audio Upload] 异常:", error);
    const rawMsg = error instanceof Error ? error.message : "服务器内部错误";
    const userMsg = translateStorageError(rawMsg);
    return NextResponse.json(
      { success: false, error: userMsg },
      { status: 500 }
    );
  }
}
