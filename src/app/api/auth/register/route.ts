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

    // 密码长度验证
    if (password.length < 6) {
      return NextResponse.json(
        { success: false, error: '密码长度不能少于 6 位' },
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

    // 检查用户名是否已存在
    const { data: existingUser, error: checkError } = await client
      .from('users')
      .select('id')
      .eq('username', username)
      .maybeSingle();

    if (checkError) {
      console.error('检查用户名失败:', checkError);
      return NextResponse.json(
        { success: false, error: '服务器错误，请稍后重试' },
        { status: 500 }
      );
    }

    if (existingUser) {
      return NextResponse.json(
        { success: false, error: '用户名已被注册' },
        { status: 400 }
      );
    }

    // 哈希密码
    const passwordHash = await bcrypt.hash(password, 12);

    // 创建用户（不传递 id，让数据库自动生成 serial id）
    const { data: newUser, error: insertError } = await client
      .from('users')
      .insert({
        username: username,
        password_hash: passwordHash,
      })
      .select('id, username, created_at')
      .single();

    if (insertError) {
      console.error('创建用户失败:', insertError);
      
      // 检查唯一约束冲突
      if (insertError.code === '23505') {
        return NextResponse.json(
          { success: false, error: '用户名已被注册' },
          { status: 400 }
        );
      }
      
      return NextResponse.json(
        { success: false, error: '注册失败，请稍后重试' },
        { status: 500 }
      );
    }

    await createSession(client, newUser.id);

    return NextResponse.json({
      success: true,
      message: '注册成功',
      user: toAuthUser(newUser),
    });
  } catch (error) {
    console.error('注册错误:', error);
    
    // 提取错误信息
    let message = '注册失败，请稍后重试';
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
