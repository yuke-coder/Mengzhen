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
  sex?: number;
  country?: string;
  province?: string;
  city?: string;
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
    const gender = wechatProfile.sex === 1
      ? "male"
      : wechatProfile.sex === 2
        ? "female"
        : null;
    const location = [
      wechatProfile.country,
      wechatProfile.province,
      wechatProfile.city,
    ]
      .map((part) => part?.trim())
      .filter((part): part is string => Boolean(part))
      .join(" ")
      .trim() || null;

    // 微信允许用户关闭头像/昵称等资料返回；缺失字段不是错误，调用方应
    // 保留本地已有值。只有微信明确返回错误码时才提示授权失败。
    if (wechatProfile.errcode) {
      return NextResponse.json(
        { success: false, error: "微信授权已失效，请重试" },
        { status: 502 },
      );
    }

    const supabase = getSupabaseClient();
    if (!supabase) {
      return NextResponse.json(
        { success: false, error: "存储服务未配置" },
        { status: 503 },
      );
    }

    // 头像是可选字段。微信头像地址偶尔过期或存储服务暂时不可用时，
    // 仍然要把昵称/性别/地区写入，不能让一个缺失头像阻断整次同步。
    let syncedAvatarUrl: string | null = null;
    if (avatarUrl) {
      try {
        const avatarResponse = await fetchWithTimeout(avatarUrl.toString(), {
          redirect: "error",
          headers: { Accept: "image/jpeg,image/png,image/webp" },
        });
        if (!avatarResponse.ok) throw new Error("WECHAT_AVATAR_HTTP_ERROR");

        const declaredLength = Number(avatarResponse.headers.get("content-length") || "0");
        if (declaredLength > MAX_AVATAR_BYTES) throw new Error("WECHAT_AVATAR_TOO_LARGE");

        const contentType = avatarResponse.headers
          .get("content-type")
          ?.split(";", 1)[0]
          .trim()
          .toLowerCase() ?? "";
        const extension = IMAGE_TYPES.get(contentType);
        if (!extension) throw new Error("WECHAT_AVATAR_TYPE_UNSUPPORTED");

        const avatarBytes = new Uint8Array(await avatarResponse.arrayBuffer());
        if (avatarBytes.byteLength === 0 || avatarBytes.byteLength > MAX_AVATAR_BYTES) {
          throw new Error("WECHAT_AVATAR_INVALID");
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
        syncedAvatarUrl = publicUrl.publicUrl;
      } catch (error) {
        console.warn(
          "微信头像同步跳过",
          error instanceof Error ? error.message : "UNKNOWN",
        );
      }
    }

    // 只读取地区隐私状态，不把微信缺失字段写成 null。这样用户原来填写的
    // 内容会保留；性别只要由微信返回非空值，就按微信内容覆盖本地值。
    type ExistingWechatProfile = {
      gender?: string | null;
      hide_region?: boolean | null;
    };
    let existingProfile: ExistingWechatProfile | null = null;
    const existingResult = await supabase
      .from("user_profiles")
      .select("gender, hide_region")
      .eq("user_id", user.id)
      .maybeSingle();
    if (existingResult.error) {
      // 兼容 hide_region 迁移尚未上线的旧数据库；昵称/头像同步仍可继续。
      const legacyResult = await supabase
        .from("user_profiles")
        .select("gender")
        .eq("user_id", user.id)
        .maybeSingle();
      if (legacyResult.error) throw legacyResult.error;
      existingProfile = legacyResult.data as ExistingWechatProfile | null;
    } else {
      existingProfile = existingResult.data as ExistingWechatProfile | null;
    }

    const profileData: Record<string, unknown> = { user_id: user.id };
    if (nickname) profileData.nickname = nickname;
    if (syncedAvatarUrl) profileData.avatar_url = syncedAvatarUrl;
    // A non-empty value returned by WeChat is authoritative, including when
    // the local profile previously used the explicit "secret" value.
    if (gender) {
      profileData.gender = gender;
    }
    if (location && existingProfile?.hide_region !== true) profileData.location = location;
    if (Object.keys(profileData).length > 1) {
      const { error: profileError } = await supabase
        .from("user_profiles")
        .upsert(profileData, { onConflict: "user_id" });
      if (profileError) {
        console.error("保存微信资料失败", profileError);
        return NextResponse.json(
          { success: false, error: "微信资料保存失败，请重试" },
          { status: 500 },
        );
      }
    }

    return NextResponse.json({
      success: true,
      message: "微信资料同步成功",
      nickname: nickname || null,
      avatar_url: syncedAvatarUrl,
      gender,
      location,
    });
  } catch (error) {
    console.error("微信资料同步异常", error instanceof Error ? error.message : "UNKNOWN");
    return NextResponse.json(
      { success: false, error: "微信资料同步失败，请重试" },
      { status: 500 },
    );
  }
}
