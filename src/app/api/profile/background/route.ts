import { NextRequest, NextResponse } from "next/server";
import { getSupabaseClient } from "@/lib/supabase-client";
import { getAuthUser } from "@/lib/auth";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

const BUCKET = "avatars";
const MAX_FILE_SIZE = 10 * 1024 * 1024;
const EXTENSIONS: Record<string, string> = {
  "image/jpeg": "jpg",
  "image/png": "png",
  "image/gif": "gif",
  "image/webp": "webp",
};

interface UploadTicketRequest {
  fileSize?: unknown;
  mimeType?: unknown;
}

interface UploadCompleteRequest extends UploadTicketRequest {
  fileKey?: unknown;
}

function uploadMetadata(body: UploadTicketRequest) {
  const fileSize = typeof body.fileSize === "number" && Number.isSafeInteger(body.fileSize)
    ? body.fileSize
    : 0;
  const mimeType = typeof body.mimeType === "string" ? body.mimeType.slice(0, 100) : "";
  return { fileSize, mimeType, extension: EXTENSIONS[mimeType] };
}

function isUserBackgroundKey(value: unknown, userId: string): value is string {
  if (typeof value !== "string") return false;
  const escapedUserId = userId.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  return new RegExp(`^backgrounds/${escapedUserId}/[0-9]+_[0-9a-f]{8}\\.(jpg|png|gif|webp)$`).test(value);
}

function storedObjectSize(object: { size?: number; metadata?: Record<string, unknown> }): number {
  if (typeof object.size === "number" && Number.isFinite(object.size) && object.size > 0) {
    return object.size;
  }
  const size = Number(object.metadata?.size);
  return Number.isFinite(size) && size > 0 ? size : 0;
}

export async function POST(request: NextRequest) {
  try {
    const user = await getAuthUser();
    if (!user) {
      return NextResponse.json({ success: false, error: "请先登录" }, { status: 401 });
    }

    const body = await request.json().catch(() => null) as UploadTicketRequest | null;
    if (!body) {
      return NextResponse.json({ success: false, error: "背景图上传信息无效" }, { status: 400 });
    }
    const { fileSize, mimeType, extension } = uploadMetadata(body);
    if (!extension) {
      return NextResponse.json(
        { success: false, error: "仅支持 JPG、PNG、GIF、WebP 格式" },
        { status: 400 },
      );
    }
    if (fileSize <= 0 || fileSize > MAX_FILE_SIZE) {
      return NextResponse.json(
        { success: false, error: "背景图片不能超过 10 MB" },
        { status: 400 },
      );
    }

    const supabase = getSupabaseClient();
    if (!supabase) {
      return NextResponse.json({ success: false, error: "存储服务未配置" }, { status: 503 });
    }

    const fileKey = `backgrounds/${user.id}/${Date.now()}_${crypto.randomUUID().slice(0, 8)}.${extension}`;
    const { data, error } = await supabase.storage
      .from(BUCKET)
      .createSignedUploadUrl(fileKey);

    if (error || !data) {
      console.error("创建个人背景上传凭证失败:", error);
      return NextResponse.json(
        { success: false, error: "无法准备背景图上传，请稍后重试" },
        { status: 503 },
      );
    }

    return NextResponse.json({
      success: true,
      file_key: fileKey,
      signed_upload_url: data.signedUrl,
    });
  } catch (error) {
    console.error("创建个人背景上传凭证异常:", error);
    return NextResponse.json(
      { success: false, error: "无法准备背景图上传，请稍后重试" },
      { status: 500 },
    );
  }
}

export async function PUT(request: NextRequest) {
  try {
    const user = await getAuthUser();
    if (!user) {
      return NextResponse.json({ success: false, error: "请先登录" }, { status: 401 });
    }

    const body = await request.json().catch(() => null) as UploadCompleteRequest | null;
    if (!body) {
      return NextResponse.json({ success: false, error: "背景图上传信息无效" }, { status: 400 });
    }
    const { fileSize, mimeType, extension } = uploadMetadata(body);
    if (
      !extension ||
      fileSize <= 0 ||
      fileSize > MAX_FILE_SIZE ||
      !isUserBackgroundKey(body.fileKey, user.id) ||
      !body.fileKey.endsWith(`.${extension}`)
    ) {
      return NextResponse.json({ success: false, error: "背景图上传信息无效" }, { status: 400 });
    }

    const supabase = getSupabaseClient();
    if (!supabase) {
      return NextResponse.json({ success: false, error: "存储服务未配置" }, { status: 503 });
    }

    const { data: object, error: infoError } = await supabase.storage
      .from(BUCKET)
      .info(body.fileKey);
    const actualSize = object ? storedObjectSize(object) : 0;
    if (infoError || !object || actualSize !== fileSize || object.contentType !== mimeType) {
      console.error("校验个人背景直传文件失败:", infoError, {
        hasObject: Boolean(object),
        expectedSize: fileSize,
        actualSize,
        expectedType: mimeType,
        actualType: object?.contentType,
      });
      if (object) await supabase.storage.from(BUCKET).remove([body.fileKey]);
      return NextResponse.json(
        { success: false, error: "背景图上传不完整，请重新选择" },
        { status: 422 },
      );
    }

    const { data } = supabase.storage.from(BUCKET).getPublicUrl(body.fileKey);
    const backgroundUrl = data.publicUrl;
    const { error: profileError } = await supabase
      .from("user_profiles")
      .upsert({ user_id: user.id, background_url: backgroundUrl }, { onConflict: "user_id" });

    if (profileError) {
      console.error("保存个人背景地址失败:", profileError);
      return NextResponse.json(
        { success: false, error: "背景图保存失败，请重试" },
        { status: 500 },
      );
    }

    return NextResponse.json({ success: true, message: "背景图已更新", background_url: backgroundUrl });
  } catch (error) {
    console.error("登记个人背景异常:", error);
    return NextResponse.json({ success: false, error: "服务器错误" }, { status: 500 });
  }
}
