import { NextResponse } from 'next/server';
import { cookies } from 'next/headers';
import { getSupabaseClient } from '@/lib/supabase-client';
import { SESSION_COOKIE_NAME } from '@/lib/session';

export const dynamic = 'force-dynamic';

export async function POST(request: Request) {
  try {
    const cookieStore = await cookies();
    const sessionToken = cookieStore.get(SESSION_COOKIE_NAME)?.value;

    if (!sessionToken) {
      return NextResponse.json(
        { success: false, message: '请先登录' },
        { status: 401 }
      );
    }

    const client = getSupabaseClient();
    if (!client) {
      return NextResponse.json(
        { success: false, message: '数据库未配置' },
        { status: 503 }
      );
    }

    const { data: session, error: sessionError } = await client
      .from('sessions')
      .select('user_id, expires_at')
      .eq('token', sessionToken)
      .maybeSingle();

    if (sessionError || !session) {
      return NextResponse.json(
        { success: false, message: '登录状态已失效' },
        { status: 401 }
      );
    }

    const expiresAt = new Date(session.expires_at);
    if (expiresAt < new Date()) {
      return NextResponse.json(
        { success: false, message: '登录状态已过期' },
        { status: 401 }
      );
    }

    const body = await request.json();
    const { type, content, contact, images } = body;

    if (!type || !content?.trim()) {
      return NextResponse.json(
        { success: false, message: '反馈类型和反馈内容不能为空' },
        { status: 400 }
      );
    }

    if (!['bug', 'suggestion'].includes(type)) {
      return NextResponse.json(
        { success: false, message: '反馈类型不正确' },
        { status: 400 }
      );
    }

    const { error } = await client.from('feedbacks').insert({
      user_id: session.user_id,
      type,
      content: content.trim(),
      contact: contact?.trim() || null,
      images: Array.isArray(images) && images.length > 0 ? images : null,
    });

    if (error) {
      console.error('[Feedback Insert Error]', error);
      return NextResponse.json(
        { success: false, message: '提交失败，请稍后重试' },
        { status: 500 }
      );
    }

    return NextResponse.json({ success: true, message: '提交成功' });
  } catch (error) {
    console.error('[Feedback Error]', error);
    return NextResponse.json(
      { success: false, message: '服务器内部错误' },
      { status: 500 }
    );
  }
}
