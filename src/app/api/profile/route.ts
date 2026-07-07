import { NextRequest, NextResponse } from "next/server";
import { getSupabaseClient } from "@/lib/supabase-client";
import { getAuthUser } from "@/lib/auth";

export const dynamic = 'force-dynamic';

export async function GET() {
  try {
    const user = await getAuthUser();
    if (!user) {
      return NextResponse.json(
        { success: false, error: "请先登录" },
        { status: 401 }
      );
    }

    const supabase = getSupabaseClient();
    if (!supabase) {
      return NextResponse.json(
        { success: false, error: "数据库未配置" },
        { status: 503 }
      );
    }

    const { data: profile } = await supabase
      .from("user_profiles")
      .select(
        "nickname, avatar_url, gender, birthday, location, bio, signature, username_change_count, username_change_reset_at"
      )
      .eq("user_id", user.id)
      .maybeSingle();

    const profileData = profile || {
      nickname: null,
      avatar_url: null,
      gender: null,
      birthday: null,
      location: null,
      bio: null,
      signature: null,
    };

    return NextResponse.json({
      success: true,
      profile: {
        id: user.id,
        username: user.username,
        ...profileData,
        nickname: profileData.nickname as string | null || user.username,
        createdAt: user.created_at,
      },
    });
  } catch (error) {
    console.error("获取用户资料异常:", error);
    return NextResponse.json(
      { success: false, error: "服务器错误" },
      { status: 500 }
    );
  }
}

export async function PUT(request: NextRequest) {
  try {
    const user = await getAuthUser();
    if (!user) {
      return NextResponse.json(
        { success: false, error: "请先登录" },
        { status: 401 }
      );
    }

    const body = await request.json();
    const {
      username,
      nickname,
      gender,
      birthday,
      location,
      bio,
      signature,
      avatar_url,
    } = body;

    const supabase = getSupabaseClient();
    if (!supabase) {
      return NextResponse.json(
        { success: false, error: "数据库未配置" },
        { status: 503 }
      );
    }

    if (username !== undefined) {
      const { data: existingUser } = await supabase
        .from("users")
        .select("id")
        .eq("username", username)
        .neq("id", user.id)
        .maybeSingle();

      if (existingUser) {
        return NextResponse.json(
          { success: false, error: "用户名已被占用" },
          { status: 400 }
        );
      }

      const { error: updateUserError } = await supabase
        .from("users")
        .update({ username })
        .eq("id", user.id);

      if (updateUserError) {
        console.error("更新用户名失败:", updateUserError);
        return NextResponse.json(
          { success: false, error: "更新用户名失败" },
          { status: 500 }
        );
      }
    }

    if (nickname !== undefined && nickname && (nickname.length < 1 || nickname.length > 50)) {
      return NextResponse.json(
        { success: false, error: "昵称长度需在 1-50 个字符之间" },
        { status: 400 }
      );
    }

    if (signature && signature.length > 200) {
      return NextResponse.json(
        { success: false, error: "个性签名不能超过 200 个字符" },
        { status: 400 }
      );
    }

    if (bio && bio.length > 500) {
      return NextResponse.json(
        { success: false, error: "个人简介不能超过 500 个字符" },
        { status: 500 }
      );
    }

    const { data: existingProfile } = await supabase
      .from("user_profiles")
      .select("id")
      .eq("user_id", user.id)
      .maybeSingle();

    const profileData: Record<string, unknown> = {};
    if (nickname !== undefined) profileData.nickname = nickname || null;
    if (gender !== undefined) profileData.gender = gender || null;
    if (birthday !== undefined) profileData.birthday = birthday || null;
    if (location !== undefined) profileData.location = location || null;
    if (bio !== undefined) profileData.bio = bio || null;
    if (signature !== undefined) profileData.signature = signature || null;
    if (avatar_url !== undefined) profileData.avatar_url = avatar_url || null;

    const hasProfileData = Object.keys(profileData).length > 0;
    let profileResult: Record<string, unknown> | null = null;

    if (existingProfile && hasProfileData) {
      const { data, error } = await supabase
        .from("user_profiles")
        .update(profileData)
        .eq("user_id", user.id)
        .select(
          "nickname, avatar_url, gender, birthday, location, bio, signature, username_change_count"
        )
        .single();

      if (error) {
        console.error("更新用户资料失败:", error);
        return NextResponse.json(
          { success: false, error: "更新资料失败" },
          { status: 500 }
        );
      }
      profileResult = data;
    } else if (!existingProfile && hasProfileData) {
      const { data, error } = await supabase
        .from("user_profiles")
        .insert({
          user_id: user.id,
          ...profileData,
        })
        .select(
          "nickname, avatar_url, gender, birthday, location, bio, signature, username_change_count"
        )
        .single();

      if (error) {
        console.error("创建用户资料失败:", error);
        return NextResponse.json(
          { success: false, error: "更新资料失败" },
          { status: 500 }
        );
      }
      profileResult = data;
    } else if (existingProfile) {
      const { data } = await supabase
        .from("user_profiles")
        .select(
          "nickname, avatar_url, gender, birthday, location, bio, signature, username_change_count"
        )
        .eq("user_id", user.id)
        .maybeSingle();
      profileResult = data;
    }

    return NextResponse.json({
      success: true,
      message: username ? "用户名和资料更新成功" : "资料更新成功",
      profile: {
        id: user.id,
        username: username ?? user.username,
        ...profileResult,
        nickname: (profileResult?.nickname || username || user.username),
        createdAt: user.created_at,
      },
    });
  } catch (error) {
    console.error("更新用户资料异常:", error);
    return NextResponse.json(
      { success: false, error: "服务器错误" },
      { status: 500 }
    );
  }
}
