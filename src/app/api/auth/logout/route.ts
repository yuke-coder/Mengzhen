import { NextResponse } from 'next/server';

const SESSION_COOKIE_NAME = 'mindmap_session';

export async function POST() {
  const response = NextResponse.json({
    success: true,
    message: '退出登录成功',
  });

  // 清除会话 Cookie
  response.cookies.set(SESSION_COOKIE_NAME, '', {
    httpOnly: true,
    secure: process.env.NODE_ENV === 'production',
    sameSite: 'lax',
    maxAge: 0,
    path: '/',
  });

  return response;
}
