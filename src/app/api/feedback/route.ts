import { NextResponse } from 'next/server';
import { cookies } from 'next/headers';
import { getSupabaseClient } from '@/lib/supabase-client';
import { SESSION_COOKIE_NAME } from '@/lib/session';

export const dynamic = 'force-dynamic';

const FEEDBACK_FIELDS =
  'id, type, category, content, contact, images, status, op_group, op_name, processed_at, created_at, updated_at';
const REPLY_FIELDS =
  'id, feedback_id, sender_role, sender, content, images, created_at';

async function getFeedbackIdentity() {
  const cookieStore = await cookies();
  const sessionToken = cookieStore.get(SESSION_COOKIE_NAME)?.value;

  if (!sessionToken) {
    return {
      error: NextResponse.json(
        { success: false, message: '请先登录' },
        { status: 401 }
      ),
    };
  }

  const client = getSupabaseClient();
  if (!client) {
    return {
      error: NextResponse.json(
        { success: false, message: '数据库未配置' },
        { status: 503 }
      ),
    };
  }

  const { data: session, error: sessionError } = await client
    .from('sessions')
    .select('user_id, expires_at')
    .eq('token', sessionToken)
    .maybeSingle();

  if (sessionError || !session || new Date(session.expires_at) < new Date()) {
    return {
      error: NextResponse.json(
        { success: false, message: '登录状态已失效' },
        { status: 401 }
      ),
    };
  }

  return { client, userId: session.user_id as string };
}

function textValue(value: unknown, maxLength: number): string {
  return typeof value === 'string' ? value.trim().slice(0, maxLength) : '';
}

function imageValues(value: unknown): string[] | null {
  if (!Array.isArray(value)) return null;
  const images = value
    .filter((item): item is string => typeof item === 'string' && item.length > 0)
    .slice(0, 6);
  return images.length > 0 ? images : null;
}

export async function GET(request: Request) {
  try {
    const identity = await getFeedbackIdentity();
    if (identity.error) return identity.error;

    const feedbackId = new URL(request.url).searchParams.get('id');
    if (feedbackId) {
      const { data: feedback, error } = await identity.client
        .from('feedbacks')
        .select(FEEDBACK_FIELDS)
        .eq('user_id', identity.userId)
        .eq('id', feedbackId)
        .maybeSingle();
      if (error) throw error;
      if (!feedback) {
        return NextResponse.json(
          { success: false, message: '反馈记录不存在' },
          { status: 404 }
        );
      }

      const { data: replies, error: replyError } = await identity.client
        .from('feedback_replies')
        .select(REPLY_FIELDS)
        .eq('feedback_id', feedbackId)
        .order('created_at', { ascending: true });
      if (replyError) throw replyError;

      return NextResponse.json({
        success: true,
        feedback,
        replies: replies ?? [],
      });
    }

    const { data, error } = await identity.client
      .from('feedbacks')
      .select(FEEDBACK_FIELDS)
      .eq('user_id', identity.userId)
      .order('created_at', { ascending: false })
      .limit(100);
    if (error) throw error;
    return NextResponse.json({ success: true, feedbacks: data ?? [] });
  } catch (error) {
    console.error('[Feedback Query Error]', error);
    return NextResponse.json(
      { success: false, message: '反馈记录加载失败' },
      { status: 500 }
    );
  }
}

export async function POST(request: Request) {
  try {
    const identity = await getFeedbackIdentity();
    if (identity.error) return identity.error;

    const body = await request.json();
    const feedbackId = textValue(body.feedbackId, 80);
    const content = textValue(body.content, feedbackId ? 200 : 2000);
    const images = imageValues(body.images);

    if (feedbackId) {
      if (!content) {
        return NextResponse.json(
          { success: false, message: '回复内容不得为空' },
          { status: 400 }
        );
      }

      const { data: feedback, error: feedbackError } = await identity.client
        .from('feedbacks')
        .select('id, status')
        .eq('id', feedbackId)
        .eq('user_id', identity.userId)
        .maybeSingle();
      if (feedbackError) throw feedbackError;
      if (!feedback) {
        return NextResponse.json(
          { success: false, message: '反馈记录不存在' },
          { status: 404 }
        );
      }
      if (feedback.status === 3) {
        return NextResponse.json(
          { success: false, status: 999, message: '该反馈已关闭，请重新提交' },
          { status: 409 }
        );
      }

      const { data: reply, error: replyError } = await identity.client
        .from('feedback_replies')
        .insert({
          feedback_id: feedbackId,
          user_id: identity.userId,
          sender_role: 'user',
          sender: '我',
          content,
          images,
        })
        .select(REPLY_FIELDS)
        .single();
      if (replyError) throw replyError;

      await identity.client
        .from('feedbacks')
        .update({ updated_at: new Date().toISOString() })
        .eq('id', feedbackId)
        .eq('user_id', identity.userId);

      return NextResponse.json({ success: true, message: '回复成功', reply });
    }

    const type = textValue(body.type, 20);
    if (!['bug', 'suggestion'].includes(type) || !content) {
      return NextResponse.json(
        { success: false, message: '反馈类型和反馈内容不能为空' },
        { status: 400 }
      );
    }

    const { data: feedback, error } = await identity.client
      .from('feedbacks')
      .insert({
        user_id: identity.userId,
        type,
        category: textValue(body.category, 80) || null,
        content,
        contact: textValue(body.contact, 200) || null,
        images,
      })
      .select(FEEDBACK_FIELDS)
      .single();

    if (error) throw error;
    return NextResponse.json({ success: true, message: '提交成功', feedback });
  } catch (error) {
    console.error('[Feedback Error]', error);
    return NextResponse.json(
      { success: false, message: '服务器内部错误' },
      { status: 500 }
    );
  }
}
