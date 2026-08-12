import { NextResponse } from "next/server";
import {
  AuthServiceUnavailableError,
  resolveAuthSession,
} from "@/lib/auth";
import { getSupabaseClient } from "@/lib/supabase-client";

export const dynamic = "force-dynamic";

function json(body: object, status = 200) {
  return NextResponse.json(body, {
    status,
    headers: { "Cache-Control": "private, no-store, max-age=0" },
  });
}

export async function GET() {
  try {
    const session = await resolveAuthSession();
    if (session.status === "anonymous") {
      return json({ success: true, authenticated: false, user: null });
    }

    const client = getSupabaseClient();
    if (!client) throw new AuthServiceUnavailableError("认证服务未配置");

    const { data: profile, error: profileError } = await client
      .from("user_profiles")
      .select("nickname, avatar_url, background_url, gender, birthday, location, bio, signature")
      .eq("user_id", session.user.id)
      .maybeSingle();

    if (profileError) {
      throw new AuthServiceUnavailableError("用户资料服务暂时不可用", profileError);
    }

    return json({
      success: true,
      authenticated: true,
      user: {
        id: session.user.id,
        username: session.user.username,
        avatar_url: profile?.avatar_url || null,
        background_url: profile?.background_url || null,
        nickname: profile?.nickname || null,
        gender: profile?.gender || null,
        birthday: profile?.birthday || null,
        location: profile?.location || null,
        bio: profile?.bio || null,
        signature: profile?.signature || null,
        createdAt: session.user.created_at,
      },
    });
  } catch (error) {
    console.error("[Me Error]", error);
    return json(
      { success: false, error: "认证服务暂时不可用，请稍后重试" },
      error instanceof AuthServiceUnavailableError ? 503 : 500,
    );
  }
}
