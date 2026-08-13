/**
 * Cloudflare Turnstile 服务端验证工具
 */

interface TurnstileVerifyResponse {
  success: boolean;
  'error-codes'?: string[];
  challenge_ts?: string;
  hostname?: string;
  action?: string;
  cdata?: string;
}

export class TurnstileServiceUnavailableError extends Error {
  constructor(message: string, cause?: unknown) {
    super(message, { cause });
    this.name = 'TurnstileServiceUnavailableError';
  }
}

/**
 * 验证 Turnstile token
 * @param token 前端传来的 Turnstile token
 * @returns 验证是否通过
 */
export async function verifyTurnstileToken(token: string): Promise<boolean> {
  const secretKey = process.env.TURNSTILE_SECRET_KEY;
  if (!secretKey) {
    throw new TurnstileServiceUnavailableError('TURNSTILE_SECRET_KEY 未配置');
  }

  try {
    const response = await fetch(
      'https://challenges.cloudflare.com/turnstile/v0/siteverify',
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams({
          secret: secretKey,
          response: token,
        }),
      }
    );

    if (!response.ok) {
      throw new TurnstileServiceUnavailableError(`Turnstile 服务返回 ${response.status}`);
    }
    const data: TurnstileVerifyResponse = await response.json();
    if (!data.success || data.action !== 'auth') return false;

    const configuredHost = new URL(
      process.env.NEXT_PUBLIC_SITE_URL || 'https://driftcue.com',
    ).hostname;
    const allowedHosts = new Set([
      configuredHost,
      'driftcue.com',
      'www.driftcue.com',
      'localhost',
      '127.0.0.1',
    ]);
    return Boolean(data.hostname && allowedHosts.has(data.hostname));
  } catch (error) {
    if (error instanceof TurnstileServiceUnavailableError) throw error;
    console.error('Turnstile 验证请求失败:', error);
    throw new TurnstileServiceUnavailableError('Turnstile 服务暂时不可用', error);
  }
}
