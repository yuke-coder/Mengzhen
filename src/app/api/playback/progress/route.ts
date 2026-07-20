import { NextRequest, NextResponse } from 'next/server';
import { getSupabaseClient } from '@/lib/supabase-client';
import { getAuthUser } from '@/lib/auth';

export const dynamic = 'force-dynamic';

/**
 * 播放进度同步 API
 *
 * GET /api/playback/progress - 获取当前用户所有播放进度
 * PUT /api/playback/progress - 上报/更新某条音频的播放进度
 * DELETE /api/playback/progress?audioId=xxx - 删除某条进度
 */

// 获取所有播放进度
export async function GET() {
  try {
    const user = await getAuthUser();
    if (!user) {
      return NextResponse.json({ success: false, error: '请先登录' }, { status: 401 });
    }

    const supabase = getSupabaseClient();
    if (!supabase) {
      return NextResponse.json({ success: false, error: '服务器未配置' }, { status: 503 });
    }

    const { data, error } = await supabase
      .from('playback_progress')
      .select('audio_id, position_seconds, duration_seconds, updated_at')
      .eq('user_id', user.id)
      .order('updated_at', { ascending: false });

    if (error) {
      console.error('[Playback Progress] 查询失败:', error);
      return NextResponse.json({ success: false, error: '获取播放进度失败' }, { status: 500 });
    }

    return NextResponse.json({ success: true, progress: data || [] });
  } catch (error) {
    console.error('[Playback Progress] GET 异常:', error);
    return NextResponse.json({ success: false, error: '服务器错误' }, { status: 500 });
  }
}

// 上报/更新播放进度
export async function PUT(request: NextRequest) {
  try {
    const user = await getAuthUser();
    if (!user) {
      return NextResponse.json({ success: false, error: '请先登录' }, { status: 401 });
    }

    const body = await request.json();
    const { audioId, positionSeconds, durationSeconds } = body;

    if (!audioId || typeof positionSeconds !== 'number' || positionSeconds < 0) {
      return NextResponse.json({ success: false, error: '参数无效' }, { status: 400 });
    }

    const supabase = getSupabaseClient();
    if (!supabase) {
      return NextResponse.json({ success: false, error: '服务器未配置' }, { status: 503 });
    }

    // upsert: 如果 (user_id, audio_id) 组合已存在则更新，否则插入
    const { error } = await supabase
      .from('playback_progress')
      .upsert({
        user_id: user.id,
        audio_id: audioId,
        position_seconds: Math.floor(positionSeconds),
        duration_seconds: typeof durationSeconds === 'number' ? Math.floor(durationSeconds) : 0,
        updated_at: new Date().toISOString(),
      }, {
        onConflict: 'user_id,audio_id',
      });

    if (error) {
      console.error('[Playback Progress] upsert 失败:', error);
      return NextResponse.json({ success: false, error: '保存播放进度失败' }, { status: 500 });
    }

    return NextResponse.json({ success: true });
  } catch (error) {
    console.error('[Playback Progress] PUT 异常:', error);
    return NextResponse.json({ success: false, error: '服务器错误' }, { status: 500 });
  }
}

// 删除播放进度
export async function DELETE(request: NextRequest) {
  try {
    const user = await getAuthUser();
    if (!user) {
      return NextResponse.json({ success: false, error: '请先登录' }, { status: 401 });
    }

    const { searchParams } = new URL(request.url);
    const audioId = searchParams.get('audioId');

    if (!audioId) {
      return NextResponse.json({ success: false, error: '缺少音频 ID' }, { status: 400 });
    }

    const supabase = getSupabaseClient();
    if (!supabase) {
      return NextResponse.json({ success: false, error: '服务器未配置' }, { status: 503 });
    }

    const { error } = await supabase
      .from('playback_progress')
      .delete()
      .eq('user_id', user.id)
      .eq('audio_id', audioId);

    if (error) {
      console.error('[Playback Progress] 删除失败:', error);
      return NextResponse.json({ success: false, error: '删除播放进度失败' }, { status: 500 });
    }

    return NextResponse.json({ success: true });
  } catch (error) {
    console.error('[Playback Progress] DELETE 异常:', error);
    return NextResponse.json({ success: false, error: '服务器错误' }, { status: 500 });
  }
}
