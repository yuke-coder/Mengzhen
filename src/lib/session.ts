import { randomBytes } from "node:crypto";
import { cookies } from "next/headers";
import type { SupabaseClient } from "@supabase/supabase-js";

export const SESSION_COOKIE_NAME = "mengzhen_session";
export const SESSION_MAX_AGE = 365 * 24 * 60 * 60;

export interface SessionUser {
  id: string | number;
  username: string;
  created_at: string;
}

export function toAuthUser(user: SessionUser) {
  return {
    id: user.id,
    username: user.username,
    createdAt: user.created_at,
  };
}

export async function createSession(client: SupabaseClient, userId: string | number) {
  const token = randomBytes(48).toString("base64url");
  const expiresAt = new Date();
  expiresAt.setFullYear(expiresAt.getFullYear() + 1);

  try {
    await client.from("sessions").insert({
      user_id: userId,
      token,
      expires_at: expiresAt.toISOString(),
    });
  } catch (error) {
    console.error("存储 session 失败:", error);
  }

  const cookieStore = await cookies();
  cookieStore.set(SESSION_COOKIE_NAME, token, {
    httpOnly: true,
    secure: process.env.NODE_ENV !== "development",
    sameSite: "lax",
    maxAge: SESSION_MAX_AGE,
    path: "/",
  });
}
