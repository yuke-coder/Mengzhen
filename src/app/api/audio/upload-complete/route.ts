import { NextRequest, NextResponse } from "next/server";
import { getAuthUser } from "@/lib/auth";
import { getSupabaseClient } from "@/lib/supabase-client";
import {
  AUDIO_BUCKET,
  isSupportedAudio,
  isUserAudioObjectKey,
  normalizeAudioFileName,
  toAudioTitle,
} from "@/lib/audio-upload";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

interface UploadCompleteRequest {
  fileKey?: unknown;
  fileName?: unknown;
  fileSize?: unknown;
  mimeType?: unknown;
}

function objectSize(metadata: Record<string, unknown> | undefined, size: number | undefined): number {
  if (typeof size === "number" && Number.isFinite(size) && size > 0) return size;
  const rawSize = metadata?.size;
  const parsed = typeof rawSize === "number" ? rawSize : Number(rawSize);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : 0;
}

export async function POST(request: NextRequest) {
  try {
    const user = await getAuthUser();
    if (!user) return NextResponse.json({ success: false, error: "请先登录" }, { status: 401 });

    const body = await request.json() as UploadCompleteRequest;
    const fileName = normalizeAudioFileName(body.fileName);
    const expectedFileSize = typeof body.fileSize === "number" && Number.isSafeInteger(body.fileSize)
      ? body.fileSize
      : 0;
    const requestedMimeType = typeof body.mimeType === "string" ? body.mimeType.slice(0, 100) : "";
    if (!fileName || !isUserAudioObjectKey(body.fileKey, user.id)) {
      return NextResponse.json({ success: false, error: "音频上传信息无效" }, { status: 400 });
    }
    if (expectedFileSize <= 0) {
      return NextResponse.json({ success: false, error: "音频文件大小无效" }, { status: 400 });
    }
    if (!isSupportedAudio(fileName, requestedMimeType)) {
      return NextResponse.json({ success: false, error: "不支持的音频格式" }, { status: 400 });
    }

    const supabase = getSupabaseClient();
    if (!supabase) return NextResponse.json({ success: false, error: "存储服务未配置" }, { status: 503 });

    const { data: objectInfo, error: infoError } = await supabase.storage
      .from(AUDIO_BUCKET)
      .info(body.fileKey);
    if (infoError || !objectInfo) {
      console.error("[audio-upload-complete] 未找到直传音频:", infoError);
      return NextResponse.json({ success: false, error: "未找到已上传的音频，请重新上传" }, { status: 404 });
    }

    const fileSize = objectSize(objectInfo.metadata, objectInfo.size);
    if (!fileSize) {
      return NextResponse.json({ success: false, error: "无法读取已上传音频的大小，请重新上传" }, { status: 422 });
    }
    if (fileSize !== expectedFileSize) {
      return NextResponse.json({ success: false, error: "已上传音频不完整，请重新上传" }, { status: 422 });
    }
    const mimeType = objectInfo.contentType || requestedMimeType || "audio/mpeg";
    if (!isSupportedAudio(fileName, mimeType)) {
      return NextResponse.json({ success: false, error: "已上传文件不是受支持的音频格式" }, { status: 422 });
    }

    const { data: existing, error: existingError } = await supabase
      .from("audios")
      .select("id, file_url, file_key, file_name, file_size")
      .eq("user_id", user.id)
      .eq("file_key", body.fileKey)
      .maybeSingle();
    if (existingError) {
      console.error("[audio-upload-complete] 查询已有音频失败:", existingError);
      return NextResponse.json({ success: false, error: "音频登记失败，请重试" }, { status: 500 });
    }

    const { data: publicUrl } = supabase.storage.from(AUDIO_BUCKET).getPublicUrl(body.fileKey);
    if (existing) {
      return NextResponse.json({
        success: true,
        audio_url: existing.file_url || publicUrl.publicUrl,
        file_key: existing.file_key,
        file_name: existing.file_name,
        file_size: existing.file_size,
      });
    }

    const { data: created, error: createError } = await supabase
      .from("audios")
      .insert({
        user_id: user.id,
        title: toAudioTitle(fileName),
        file_url: publicUrl.publicUrl,
        file_key: body.fileKey,
        file_name: fileName,
        file_size: fileSize,
        duration: 0,
        mime_type: mimeType,
        sort_order: 0,
        is_active: true,
      })
      .select("file_url, file_key, file_name, file_size")
      .maybeSingle();

    if (createError || !created) {
      // Another completion request may have won the race. Return that row instead of creating a duplicate.
      const { data: raced } = await supabase
        .from("audios")
        .select("file_url, file_key, file_name, file_size")
        .eq("user_id", user.id)
        .eq("file_key", body.fileKey)
        .maybeSingle();
      if (raced) {
        return NextResponse.json({ success: true, audio_url: raced.file_url, file_key: raced.file_key, file_name: raced.file_name, file_size: raced.file_size });
      }
      console.error("[audio-upload-complete] 写入音频记录失败:", createError);
      return NextResponse.json({ success: false, error: "音频登记失败，请重试" }, { status: 500 });
    }

    return NextResponse.json({
      success: true,
      audio_url: created.file_url,
      file_key: created.file_key,
      file_name: created.file_name,
      file_size: created.file_size,
    });
  } catch (error) {
    console.error("[audio-upload-complete] 异常:", error);
    return NextResponse.json({ success: false, error: "音频登记失败，请重试" }, { status: 500 });
  }
}
