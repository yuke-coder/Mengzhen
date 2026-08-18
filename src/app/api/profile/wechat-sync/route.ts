import { randomUUID } from "node:crypto";
import { NextRequest, NextResponse } from "next/server";
import { getAuthUser } from "@/lib/auth";
import { getSupabaseClient } from "@/lib/supabase-client";

export const dynamic = "force-dynamic";
export const runtime = "nodejs";

const MAX_AVATAR_BYTES = 5 * 1024 * 1024;
const IMAGE_TYPES = new Map([
  ["image/jpeg", "jpg"],
  ["image/png", "png"],
  ["image/webp", "webp"],
]);

type WechatError = {
  errcode?: number;
  errmsg?: string;
};

type WechatAccessToken = WechatError & {
  access_token?: string;
  openid?: string;
};

type WechatUserInfo = WechatError & {
  nickname?: string;
  headimgurl?: string;
};

async function fetchWithTimeout(
  input: string,
  init: RequestInit = {},
  timeoutMs = 10_000,
) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), timeoutMs);
  try {
    return await fetch(input, {
      ...init,
      cache: "no-store",
      signal: controller.signal,
    });
  } finally {
    clearTimeout(timeout);
  }
}

async function fetchWechatJson<T>(url: URL): Promise<T> {
  const response = await fetchWithTimeout(url.toString());
  if (!response.ok) throw new Error("WECHAT_UPSTREAM_HTTP_ERROR");
  return await response.json() as T;
}

function normalizeWechatAvatarUrl(value: string): URL | null {
  try {
    const url = new URL(value);
    const allowedHost = url.hostname === "qlogo.cn" ||
      url.hostname.endsWith(".qlogo.cn");
    if (!allowedHost || (url.protocol !== "http:" && url.protocol !== "https:")) {
      return null;
    }
    url.protocol = "https:";
    return url;
  } catch {
    return null;
  }
}

export async function POST(request: NextRequest) {
  try {
    const user = await getAuthUser();
    if (!user) {
      return NextResponse.json(
        { success: false, error: "请先登录" },
        { status: 401 },
      );
    }

    const appId = process.env.WECHAT_APP_ID?.trim();
    const appSecret = process.env.WECHAT_APP_SECRET?.trim();
    if (!appId || !appSecret) {
      return NextResponse.json(
        { success: false, error: "微信同步尚未配置" },
        { status: 503 },
      );
    }

    const body = await request.json() as { code?: unknown };
    const code = typeof body.code === "string" ? body.code.trim() : "";
    if (!code || code.length > 512) {
      return NextResponse.json(
        { success: false, error: "微信授权凭证无效" },
        { status: 400 },
      );
    }

    const tokenUrl = new URL("https://api.weixin.qq.com/sns/oauth2/access_token");
    tokenUrl.search = new URLSearchParams({
      appid: appId,
      secret: appSecret,
      code,
      grant_type: "authorization_code",
    }).toString();
    const token = await fetchWechatJson<WechatAccessToken>(tokenUrl);
    if (!token.access_token || !token.openid || token.errcode) {
      return NextResponse.json(
        { success: false, error: "微信授权已失效，请重试" },
        { status: 502 },
      );
    }

    const profileUrl = new URL("https://api.weixin.qq.com/sns/userinfo");
    profileUrl.search = new URLSearchParams({
      access_token: token.access_token,
      openid: token.openid,
      lang: "zh_CN",
    }).toString();
    const wechatProfile = await fetchWechatJson<WechatUserInfo>(profileUrl);
    const nickname = wechatProfile.nickname?.trim().slice(0, 50) ?? "";
    const headImageUrl = wechatProfile.headimgurl?.trim() ?? "";
    const avatarUrl = normalizeWechatAvatarUrl(headImageUrl);
    if (wechatProfile.errcode || !nickname || !avatarUrl) {
      return NextResponse.json(
        { success: false, error: "未能读取微信头像和昵称" },
        { status: 502 },
      );
    }

    const avatarResponse = await fetchWithTimeout(avatarUrl.toString(), {
      redirect: "error",
      headers: { Accept: "image/jpeg,image/png,image/webp" },
    });
    if (!avatarResponse.ok) throw new Error("WECHAT_AVATAR_HTTP_ERROR");

    const declaredLength = Number(avatarResponse.headers.get("content-length") || "0");
    if (declaredLength > MAX_AVATAR_BYTES) {
      return NextResponse.json(
        { success: false, error: "微信头像文件过大" },
        { status: 413 },
      );
    }
    const contentType = avatarResponse.headers
      .get("content-type")
      ?.split(";", 1)[0]
      .trim()
      .toLowerCase() ?? "";
    const extension = IMAGE_TYPES.get(contentType);
    if (!extension) {
      return NextResponse.json(
        { success: false, error: "微信头像格式不受支持" },
        { status: 415 },
      );
    }

    const avatarBytes = new Uint8Array(await avatarResponse.arrayBuffer());
    if (avatarBytes.byteLength === 0 || avatarBytes.byteLength > MAX_AVATAR_BYTES) {
      return NextResponse.json(
        { success: false, error: "微信头像文件无效" },
        { status: 413 },
      );
    }

    const supabase = getSupabaseClient();
    if (!supabase) {
      return NextResponse.json(
        { success: false, error: "存储服务未配置" },
        { status: 503 },
      );
    }
    const fileKey = `avatars/${user.id}/wechat_${Date.now()}_${randomUUID().slice(0, 8)}.${extension}`;
    const { error: uploadError } = await supabase.storage
      .from("avatars")
      .upload(fileKey, avatarBytes, {
        contentType,
        upsert: false,
      });
    if (uploadError) throw new Error("WECHAT_AVATAR_UPLOAD_ERROR");

    const { data: publicUrl } = supabase.storage
      .from("avatars")
      .getPublicUrl(fileKey);

    return NextResponse.json({
      success: true,
      nickname,
      avatar_url: publicUrl.publicUrl,
    });
  } catch (error) {
    console.error("微信资料同步异常", error instanceof Error ? error.message : "UNKNOWN");
    return NextResponse.json(
      { success: false, error: "微信资料同步失败，请重试" },
      { status: 500 },
    );
  }
}
