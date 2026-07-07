import { NextRequest, NextResponse } from "next/server";
import bcrypt from "bcryptjs";
import { getSupabaseClient } from "@/lib/supabase-client";
import { createSession, toAuthUser, type SessionUser } from "@/lib/session";

export const dynamic = "force-dynamic";

export async function POST(request: NextRequest) {
  try {
    const { username, password } = await request.json();
    const normalizedUsername = typeof username === "string" ? username.trim() : "";
    const normalizedPassword = typeof password === "string" ? password : "";

    if (!normalizedUsername || !normalizedPassword) {
      return NextResponse.json(
        { success: false, error: "请填写用户名和密码" },
        { status: 400 }
      );
    }

    const client = getSupabaseClient();
    if (!client) {
      return NextResponse.json(
        { success: false, error: "数据库服务未配置，请联系管理员" },
        { status: 500 }
      );
    }

    const { data: user, error: findError } = await client
      .from("users")
      .select("id, username, password_hash, created_at")
      .eq("username", normalizedUsername)
      .maybeSingle();

    if (findError) {
      console.error("查找用户失败:", findError);
      return NextResponse.json(
        { success: false, error: "服务器错误，请稍后重试" },
        { status: 500 }
      );
    }

    if (user) {
      const matched = await bcrypt.compare(normalizedPassword, user.password_hash);
      if (!matched) {
        return NextResponse.json(
          { success: false, error: "用户名或密码错误" },
          { status: 401 }
        );
      }

      await createSession(client, user.id);
      return NextResponse.json({
        success: true,
        message: "登录成功",
        user: toAuthUser(user),
      });
    }

    if (normalizedPassword.length < 6) {
      return NextResponse.json(
        { success: false, error: "新账号密码不能少于 6 位" },
        { status: 400 }
      );
    }

    const { data: newUser, error: insertError } = await client
      .from("users")
      .insert({
        username: normalizedUsername,
        password_hash: await bcrypt.hash(normalizedPassword, 12),
      })
      .select("id, username, created_at")
      .single<SessionUser>();

    if (insertError || !newUser) {
      if (insertError?.code === "23505") {
        return NextResponse.json(
          { success: false, error: "用户名或密码错误" },
          { status: 401 }
        );
      }
      console.error("创建用户失败:", insertError);
      return NextResponse.json(
        { success: false, error: "注册失败，请稍后重试" },
        { status: 500 }
      );
    }

    await createSession(client, newUser.id);
    return NextResponse.json({
      success: true,
      message: "注册成功",
      user: toAuthUser(newUser),
    });
  } catch (error) {
    console.error("统一认证错误:", error);
    return NextResponse.json(
      { success: false, error: "登录失败，请稍后重试" },
      { status: 500 }
    );
  }
}
