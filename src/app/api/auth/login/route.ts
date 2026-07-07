import { NextRequest, NextResponse } from 'next/server';
import bcrypt from 'bcryptjs';
import { getSupabaseClient } from '@/lib/supabase-client';
import { createSession, toAuthUser } from '@/lib/session';

export const dynamic = 'force-dynamic';

export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    const { username, password } = body;

    // 验证必填字段
    if (!username || !password) {
      return NextResponse.json(
        { success: false, error: '请填写用户名和密码' },
        { status: 400 }
      );
    }

    const client = getSupabaseClient();
    if (!client) {
      return NextResponse.json(
        { success: false, error: '数据库服务未配置，请联系管理员' },
        { status: 500 }
      );
    }

    // 查找用户
    const { data: user, error: findError } = await client
      .from('users')
      .select('id, username, password_hash, created_at')
      .eq('username', username)
      .maybeSingle();

    if (findError) {
      console.error('查找用户失败:', findError);
      return NextResponse.json(
        { success: false, error: '服务器错误，请稍后重试' },
        { status: 500 }
      );
    }

    if (!user) {
      return NextResponse.json(
        { success: false, error: '用户名或密码错误' },
        { status: 401 }
      );
    }

    const isValidPassword = await bcrypt.compare(password, user.password_hash);
    if (!isValidPassword) {
      return NextResponse.json(
        { success: false, error: '用户名或密码错误' },
        { status: 401 }
      );
    }

    await createSession(client, user.id);

    return NextResponse.json({
      success: true,
      message: '登录成功',
      user: toAuthUser(user),
    });
  } catch (error) {
    console.error('登录错误:', error);
    
    // 提取错误信息
    let message = '登录失败，请稍后重试';
    if (error instanceof Error) {
      // 递归获取原始错误信息
      let currentError: Error | null = error;
      while (currentError) {
        const errMsg = currentError.message || '';
        if (errMsg.includes('CONNECT_TIMEOUT') || errMsg.includes('ETIMEDOUT')) {
          message = '数据库连接超时，请检查网络后重试';
          break;
        }
        if (errMsg.includes('ECONNREFUSED')) {
          message = '数据库连接被拒绝，请稍后重试';
          break;
        }
        if (errMsg.includes('ENOTFOUND')) {
          message = '数据库服务器未找到，请检查配置';
          break;
        }
        if (errMsg.includes('Connection terminated')) {
          message = '数据库连接中断，请稍后重试';
          break;
        }
        const cause: unknown = currentError instanceof Error && "cause" in currentError
          ? currentError.cause
          : null;
        currentError = cause instanceof Error ? cause : null;
      }
    }
    
    return NextResponse.json(
      { success: false, error: message },
      { status: 500 }
    );
  }
}
