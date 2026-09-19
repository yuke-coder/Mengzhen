import { NextRequest, NextResponse } from "next/server";
import { getSupabaseClient } from "@/lib/supabase-client";
import { getAuthUser } from "@/lib/auth";

export const dynamic = 'force-dynamic';

const PROFILE_SELECT_BASE =
  "nickname, avatar_url, background_url, gender, birthday, location, bio, signature, username_change_count, username_change_reset_at, hide_birthday, hide_region, last_gender";
const PROFILE_SELECT = `${PROFILE_SELECT_BASE}, constellation`;
const VALID_CONSTELLATIONS = new Set([
  "Aries",
  "Taurus",
  "Gemini",
  "Cancer",
  "Leo",
  "Virgo",
  "Libra",
  "Scorpio",
  "Sagittarius",
  "Capricornus",
  "Aquarius",
  "Pisces",
]);

type ProfileRecord = Record<string, unknown>;

/**
 * 真实性别取值（不含 'secret'）。
 *
 * 'secret'（保密）会把 gender 覆写成保密标记，真值就丢了；
 * 因此每次写入真实值时顺手在 last_gender 存一份，供取消保密时回退。
 * 见 supabase/profile_last_gender.sql。
 */
const REAL_GENDERS = new Set(["male", "female", "other"]);

function isMissingConstellationColumn(error: { code?: string; message?: string } | null | undefined): boolean {
  return Boolean(error && (error.code === "42703" || error.message?.toLowerCase().includes("constellation")));
}

async function readProfile(supabase: ReturnType<typeof getSupabaseClient>, userId: string): Promise<ProfileRecord | null> {
  if (!supabase) return null;
  const result = await supabase
    .from("user_profiles")
    .select(PROFILE_SELECT)
    .eq("user_id", userId)
    .maybeSingle();
  if (!result.error) return (result.data as unknown as ProfileRecord | null) ?? null;
  if (!isMissingConstellationColumn(result.error)) throw result.error;

  const legacyResult = await supabase
    .from("user_profiles")
    .select(PROFILE_SELECT_BASE)
    .eq("user_id", userId)
    .maybeSingle();
  if (legacyResult.error) throw legacyResult.error;
  return legacyResult.data ? { ...(legacyResult.data as unknown as ProfileRecord), constellation: null } : null;
}

async function writeProfile(
  supabase: ReturnType<typeof getSupabaseClient>,
  userId: string,
  profileData: ProfileRecord,
  existing: boolean,
): Promise<ProfileRecord | null> {
  if (!supabase) return null;
  const execute = (data: ProfileRecord, select: string) => {
    const query = existing
      ? supabase.from("user_profiles").update(data).eq("user_id", userId)
      : supabase.from("user_profiles").insert({ user_id: userId, ...data });
    return query.select(select).single();
  };

  let result = await execute(profileData, PROFILE_SELECT);
  if (result.error && isMissingConstellationColumn(result.error)) {
    const legacyData = { ...profileData };
    delete legacyData.constellation;
    result = await execute(legacyData, PROFILE_SELECT_BASE);
    if (result.error) throw result.error;
    return result.data ? { ...(result.data as unknown as ProfileRecord), constellation: null } : null;
  }
  if (result.error) throw result.error;
  return (result.data as unknown as ProfileRecord | null) ?? null;
}

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

    const profile = await readProfile(supabase, user.id);

    const profileData = profile || {
      nickname: null,
      avatar_url: null,
      background_url: null,
      gender: null,
      birthday: null,
      location: null,
      bio: null,
      signature: null,
      constellation: null,
      hide_birthday: false,
      hide_region: false,
      last_gender: null,
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
      constellation,
      location,
      bio,
      signature,
      avatar_url,
      background_url,
      hide_birthday,
      hide_region,
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

    if (constellation !== undefined && constellation !== null && !VALID_CONSTELLATIONS.has(constellation)) {
      return NextResponse.json(
        { success: false, error: "星座选项无效" },
        { status: 400 }
      );
    }

    // 生日 / 地区的「保密」开关（Android「不展示」）：只接受布尔。
    // 注意不要顺手清空 birthday / location —— 值要留着，隐藏只是对外不展示。
    for (const [name, value] of [
      ["hide_birthday", hide_birthday],
      ["hide_region", hide_region],
    ] as const) {
      if (value !== undefined && typeof value !== "boolean") {
        return NextResponse.json(
          { success: false, error: `${name} 需为布尔值` },
          { status: 400 }
        );
      }
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
    if (constellation !== undefined) profileData.constellation = constellation || null;
    if (location !== undefined) profileData.location = location || null;
    if (bio !== undefined) profileData.bio = bio || null;
    if (signature !== undefined) profileData.signature = signature || null;
    if (avatar_url !== undefined) profileData.avatar_url = avatar_url || null;
    if (background_url !== undefined) profileData.background_url = background_url || null;
    if (hide_birthday !== undefined) profileData.hide_birthday = hide_birthday;
    if (hide_region !== undefined) profileData.hide_region = hide_region;

    // 性别真值留存：只有写成真值时才记，写成 'secret' 或清空时保持不动。
    // 客户端不参与 —— 这样 Web 端选性别也顺带把真值记下了（Android 取消保密时读它回退）。
    if (typeof gender === "string" && REAL_GENDERS.has(gender)) {
      profileData.last_gender = gender;
    }

    const hasProfileData = Object.keys(profileData).length > 0;
    let profileResult: Record<string, unknown> | null = null;

    if (existingProfile && hasProfileData) {
      try {
        profileResult = await writeProfile(supabase, user.id, profileData, true);
      } catch (error) {
        console.error("更新用户资料失败:", error);
        return NextResponse.json(
          { success: false, error: "更新资料失败" },
          { status: 500 }
        );
      }
    } else if (!existingProfile && hasProfileData) {
      try {
        profileResult = await writeProfile(supabase, user.id, profileData, false);
      } catch (error) {
        console.error("创建用户资料失败:", error);
        return NextResponse.json(
          { success: false, error: "更新资料失败" },
          { status: 500 }
        );
      }
    } else if (existingProfile) {
      try {
        profileResult = await readProfile(supabase, user.id);
      } catch (error) {
        console.error("读取用户资料失败:", error);
      }
    }

    return NextResponse.json({
      success: true,
      message: username ? "用户名和资料更新成功" : "资料更新成功",
      profile: {
        id: user.id,
        username: username ?? user.username,
        ...profileResult,
        constellation: profileResult?.constellation ?? constellation ?? null,
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
