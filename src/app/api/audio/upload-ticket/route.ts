import { NextRequest, NextResponse } from "next/server";
import { getAuthUser } from "@/lib/auth";
import { getSupabaseClient } from "@/lib/supabase-client";
import {
  AUDIO_BUCKET,
  createAudioObjectKey,
  formatByteSize,
  getAudioExtension,
  isSupportedAudio,
  isTusUploadEnabled,
  isUserAudioObjectKey,
  normalizeAudioFileName,
  toDirectTusEndpoint,
} from "@/lib/audio-upload";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

interface UploadTicketRequest {
  fileName?: unknown;
  fileSize?: unknown;
  mimeType?: unknown;
  resumeFileKey?: unknown;
}

const BUCKET_LIMIT_CACHE_TTL = 5 * 60 * 1000;
let cachedBucketLimit: { value: number | null; expiresAt: number } | null = null;
let bucketLimitRequest: Promise<number | null> | null = null;

async function getAudioBucketLimit(): Promise<number | null> {
  if (cachedBucketLimit && cachedBucketLimit.expiresAt > Date.now()) return cachedBucketLimit.value;
  if (bucketLimitRequest) return bucketLimitRequest;

  bucketLimitRequest = (async () => {
    const supabase = getSupabaseClient();
    if (!supabase) throw new Error("存储服务未配置");
    const { data: bucket, error } = await supabase.storage.getBucket(AUDIO_BUCKET);
    if (error || !bucket) {
      console.error("[audio-upload-ticket] 读取音频存储配置失败:", error);
      throw new Error("音频存储空间暂不可用，请稍后重试");
    }
    const value = bucket.file_size_limit ?? null;
    cachedBucketLimit = { value, expiresAt: Date.now() + BUCKET_LIMIT_CACHE_TTL };
    return value;
  })();

  try {
    return await bucketLimitRequest;
  } finally {
    bucketLimitRequest = null;
  }
}

export async function POST(request: NextRequest) {
  try {
    const user = await getAuthUser();
    if (!user) return NextResponse.json({ success: false, error: "请先登录" }, { status: 401 });

    const body = await request.json() as UploadTicketRequest;
    const fileName = normalizeAudioFileName(body.fileName);
    const fileSize = typeof body.fileSize === "number" && Number.isSafeInteger(body.fileSize) ? body.fileSize : 0;
    const mimeType = typeof body.mimeType === "string" ? body.mimeType.slice(0, 100) : "";

    if (!fileName) return NextResponse.json({ success: false, error: "音频文件名无效" }, { status: 400 });
    if (fileSize <= 0) return NextResponse.json({ success: false, error: "音频文件为空或大小无效" }, { status: 400 });
    if (!isSupportedAudio(fileName, mimeType)) {
      return NextResponse.json({ success: false, error: "不支持的音频格式，请上传 MP3 / WAV / OGG / M4A / FLAC 等格式" }, { status: 400 });
    }

    const supabase = getSupabaseClient();
    if (!supabase) return NextResponse.json({ success: false, error: "存储服务未配置" }, { status: 503 });
    const bucketLimit = await getAudioBucketLimit();
    if (bucketLimit && fileSize > bucketLimit) {
      return NextResponse.json({
        success: false,
        error: `音频文件过大，当前最多支持 ${formatByteSize(bucketLimit)}`,
      }, { status: 413 });
    }

    let fileKey: string;
    if (body.resumeFileKey === undefined) {
      fileKey = createAudioObjectKey(user.id, fileName);
    } else {
      if (!isUserAudioObjectKey(body.resumeFileKey, user.id)) {
        return NextResponse.json({ success: false, error: "无法继续这份音频，请重新选择文件" }, { status: 400 });
      }
      const selectedExtension = getAudioExtension(fileName);
      if (selectedExtension && getAudioExtension(body.resumeFileKey) !== selectedExtension) {
        return NextResponse.json({ success: false, error: "继续上传的音频格式不匹配，请重新选择文件" }, { status: 400 });
      }
      fileKey = body.resumeFileKey;
    }
    const { data: signedUpload, error: signError } = await supabase.storage
      .from(AUDIO_BUCKET)
      .createSignedUploadUrl(fileKey);

    if (signError || !signedUpload) {
      console.error("[audio-upload-ticket] 创建临时上传凭证失败:", signError);
      return NextResponse.json({ success: false, error: "无法准备音频上传，请稍后重试" }, { status: 503 });
    }

    const supabaseUrl = process.env.SUPABASE_URL;
    if (!supabaseUrl) return NextResponse.json({ success: false, error: "存储服务未配置" }, { status: 503 });

    return NextResponse.json({
      success: true,
      fileKey,
      uploadToken: signedUpload.token,
      signedUploadUrl: signedUpload.signedUrl,
      tusEndpoint: toDirectTusEndpoint(supabaseUrl),
      tusEnabled: isTusUploadEnabled(supabaseUrl),
      bucket: AUDIO_BUCKET,
    });
  } catch (error) {
    console.error("[audio-upload-ticket] 异常:", error);
    return NextResponse.json({ success: false, error: "无法准备音频上传，请稍后重试" }, { status: 500 });
  }
}
