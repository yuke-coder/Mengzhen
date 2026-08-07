'use client';

import { Turnstile } from '@marsidev/react-turnstile';
import { useEffect, useState } from 'react';

type NativeTurnstileBridge = {
  onToken: (token: string) => void;
  onError: (message: string) => void;
};

function nativeBridge(): NativeTurnstileBridge | undefined {
  return (window as Window & { MengzhenTurnstile?: NativeTurnstileBridge })
    .MengzhenTurnstile;
}

export default function NativeTurnstilePage() {
  const siteKey = process.env.NEXT_PUBLIC_TURNSTILE_SITE_KEY;
  const [status, setStatus] = useState('正在进行安全验证…');

  useEffect(() => {
    if (!siteKey) {
      const message = '安全验证服务未配置';
      setStatus(message);
      nativeBridge()?.onError(message);
    }
  }, [siteKey]);

  return (
    <main className="flex min-h-screen items-center justify-center bg-white px-6 text-center text-neutral-700">
      <div className="flex flex-col items-center gap-4">
        {siteKey ? (
          <Turnstile
            siteKey={siteKey}
            onSuccess={(token) => {
              setStatus('验证完成');
              nativeBridge()?.onToken(token);
            }}
            onExpire={() => setStatus('验证已过期，请重新完成验证')}
            onError={() => {
              const message = '安全验证加载失败，请重试';
              setStatus(message);
              nativeBridge()?.onError(message);
            }}
          />
        ) : null}
        <p role="status" className="text-sm">
          {status}
        </p>
      </div>
    </main>
  );
}
