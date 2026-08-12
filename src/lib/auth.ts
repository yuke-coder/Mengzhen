import { cookies } from "next/headers";
import { getSupabaseClient } from "@/lib/supabase-client";
import { SESSION_COOKIE_NAME } from "@/lib/session";

export interface AuthUser {
  id: string;
  username: string;
  created_at: string;
}

export type AuthSessionResolution =
  | { status: "anonymous" }
  | { status: "authenticated"; user: AuthUser };

export class AuthServiceUnavailableError extends Error {
  constructor(message: string, cause?: unknown) {
    super(message, { cause });
    this.name = "AuthServiceUnavailableError";
  }
}

async function clearInvalidSession(token: string) {
  const cookieStore = await cookies();
  cookieStore.delete(SESSION_COOKIE_NAME);

  const client = getSupabaseClient();
  if (!client) return;
  const { error } = await client.from("sessions").delete().eq("token", token);
  if (error) console.error("清理无效 session 失败:", error);
}

export async function resolveAuthSession(): Promise<AuthSessionResolution> {
  const cookieStore = await cookies();
  const sessionToken = cookieStore.get(SESSION_COOKIE_NAME)?.value;
  if (!sessionToken) return { status: "anonymous" };

  const client = getSupabaseClient();
  if (!client) {
    throw new AuthServiceUnavailableError("认证服务未配置");
  }

  const { data: session, error: sessionError } = await client
    .from("sessions")
    .select("user_id, expires_at")
    .eq("token", sessionToken)
    .maybeSingle();

  if (sessionError) {
    throw new AuthServiceUnavailableError("认证服务暂时不可用", sessionError);
  }
  if (!session) {
    await clearInvalidSession(sessionToken);
    return { status: "anonymous" };
  }

  const expiresAt = Date.parse(session.expires_at);
  if (!Number.isFinite(expiresAt) || expiresAt <= Date.now()) {
    await clearInvalidSession(sessionToken);
    return { status: "anonymous" };
  }

  const { data: authUser, error: authError } = await client
    .from("users")
    .select("id, username, created_at")
    .eq("id", session.user_id)
    .maybeSingle();

  if (authError) {
    throw new AuthServiceUnavailableError("认证服务暂时不可用", authError);
  }
  if (!authUser) {
    await clearInvalidSession(sessionToken);
    return { status: "anonymous" };
  }

  return {
    status: "authenticated",
    user: {
      id: String(authUser.id),
      username: authUser.username,
      created_at: authUser.created_at,
    },
  };
}

export async function getAuthUser(): Promise<AuthUser | null> {
  const resolution = await resolveAuthSession();
  return resolution.status === "authenticated" ? resolution.user : null;
}
