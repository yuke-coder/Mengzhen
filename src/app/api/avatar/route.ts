import { NextRequest, NextResponse } from "next/server";
import { getSupabaseClient } from "@/lib/supabase-client";
import { getAuthUser } from "@/lib/auth";

export const dynamic = 'force-dynamic';

const ALLOWED_TYPES = ["image/jpeg", "image/png", "image/gif", "image/webp"];

export async function POST(request: NextRequest) {
  try {
    const user = await getAuthUser();
    if (!user) {
      return NextResponse.json(
        { success: false, error: "请先登录" },
        { status: 401 }
      );
    }

    const formData = await request.formData();
    const file = formData.get("avatar") as File | null;

    if (!file) {
      return NextResponse.json(
        { success: false, error: "请选择头像图片" },
        { status: 400 }
      );
    }

    if (!ALLOWED_TYPES.includes(file.type)) {
      return NextResponse.json(
        { success: false, error: "仅支持 JPG、PNG、GIF、WebP 格式" },
        { status: 400 }
      );
    }

    const supabase = getSupabaseClient();
    if (!supabase) {
      return NextResponse.json(
        { success: false, error: "存储服务未配置" },
        { status: 503 }
      );
    }

    const timestamp = Date.now();
    const randomStr = Math.random().toString(36).substring(2, 8);
    const ext = file.name.split(".").pop() || "jpg";
    const fileName = `avatars/${user.id}/${timestamp}_${randomStr}.${ext}`;

    const { error: uploadError } = await supabase.storage
      .from("avatars")
      .upload(fileName, file, {
        contentType: file.type,
        upsert: true,
      });

    if (uploadError) {
      console.error("上传头像失败:", uploadError);
      return NextResponse.json(
        { success: false, error: "上传失败，请重试" },
        { status: 500 }
      );
    }

    const { data: urlData } = supabase.storage
      .from("avatars")
      .getPublicUrl(fileName);

    const avatarUrl = urlData.publicUrl;

    const { error: upsertError } = await supabase
      .from("user_profiles")
      .upsert({
        user_id: user.id,
        avatar_url: avatarUrl,
      }, {
        onConflict: 'user_id',
      });

    if (upsertError) {
      console.error("更新头像失败:", upsertError);
      return NextResponse.json(
        { success: false, error: "保存头像失败" },
        { status: 500 }
      );
    }

    return NextResponse.json({
      success: true,
      message: "头像上传成功",
      avatar_url: avatarUrl,
    });
  } catch (error) {
    console.error("上传头像异常:", error);
    return NextResponse.json(
      { success: false, error: "服务器错误" },
      { status: 500 }
    );
  }
}

const DEFAULT_AVATARS = {
  male: "/avatars/default-male.png",
  female: "/avatars/default-female.png",
  secret: "/avatars/default-secret.png",
};

export async function DELETE(request: NextRequest) {
  try {
    const user = await getAuthUser();
    if (!user) {
      return NextResponse.json(
        { success: false, error: "请先登录" },
        { status: 401 }
      );
    }

    const { searchParams } = new URL(request.url);
    const gender = searchParams.get("gender") || "secret";
    const defaultAvatar = DEFAULT_AVATARS[gender as keyof typeof DEFAULT_AVATARS] || DEFAULT_AVATARS.secret;

    const supabase = getSupabaseClient();
    if (!supabase) {
      return NextResponse.json(
        { success: false, error: "存储服务未配置" },
        { status: 503 }
      );
    }

    const { data: profile, error: fetchError } = await supabase
      .from("user_profiles")
      .select("avatar_url")
      .eq("user_id", user.id)
      .maybeSingle();

    if (fetchError) {
      console.error("查询头像失败:", fetchError);
      return NextResponse.json(
        { success: false, error: "查询头像失败" },
        { status: 500 }
      );
    }

    if (profile?.avatar_url) {
      const urlParts = profile.avatar_url.split("/");
      const fileName = urlParts.slice(-3).join("/");
      
      try {
        await supabase.storage
          .from("avatars")
          .remove([fileName]);
      } catch (deleteError) {
        console.error("删除头像文件失败:", deleteError);
      }
    }

    const { error: updateError } = await supabase
      .from("user_profiles")
      .update({ avatar_url: defaultAvatar })
      .eq("user_id", user.id);

    if (updateError) {
      console.error("清除头像失败:", updateError);
      return NextResponse.json(
        { success: false, error: "清除头像失败" },
        { status: 500 }
      );
    }

    return NextResponse.json({
      success: true,
      message: "头像已重置为默认",
    });
  } catch (error) {
    console.error("删除头像异常:", error);
    return NextResponse.json(
      { success: false, error: "服务器错误" },
      { status: 500 }
    );
  }
}
