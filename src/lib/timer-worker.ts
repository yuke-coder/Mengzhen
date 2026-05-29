/**
 * 后台计时 Worker
 * 
 * PWA 在后台时浏览器会节流 setInterval（最低 1 分钟一次），
 * 但 Web Worker 中的定时器不受此限制，可以保持精确计时。
 * 
 * 消息协议：
 * - 主线程 → Worker: { type: 'start', endTime: number (timestamp ms) }
 * - 主线程 → Worker: { type: 'startCountdown', targetTime: number (timestamp ms), interval?: number (ms) }
 * - 主线程 → Worker: { type: 'stop' }
 * - Worker → 主线程: { type: 'tick', remainingMs: number }
 * - Worker → 主线程: { type: 'countdown', remainingMs: number, remainingSec: number }
 * - Worker → 主线程: { type: 'ended' }
 * - Worker → 主线程: { type: 'countdownEnded' }
 */

let endTimer: ReturnType<typeof setInterval> | null = null;
let countdownTimer: ReturnType<typeof setInterval> | null = null;

self.onmessage = (e: MessageEvent) => {
  const { type } = e.data;

  if (type === 'start') {
    // 清理旧的定时器
    if (endTimer !== null) {
      clearInterval(endTimer);
      endTimer = null;
    }

    const { endTime } = e.data;

    // 立即检查一次
    const now = Date.now();
    if (now >= endTime) {
      self.postMessage({ type: 'ended' });
      return;
    }

    self.postMessage({ type: 'tick', remainingMs: endTime - now });

    // 每秒检查
    endTimer = setInterval(() => {
      const now = Date.now();
      const remainingMs = endTime - now;
      
      if (remainingMs <= 0) {
        if (endTimer !== null) {
          clearInterval(endTimer);
          endTimer = null;
        }
        self.postMessage({ type: 'ended' });
      } else {
        self.postMessage({ type: 'tick', remainingMs });
      }
    }, 1000);
  }

  if (type === 'startCountdown') {
    // 清理旧的倒计时
    if (countdownTimer !== null) {
      clearInterval(countdownTimer);
      countdownTimer = null;
    }

    const { targetTime, interval = 1000 } = e.data;

    // 立即检查一次
    const now = Date.now();
    const remainingMs = targetTime - now;
    const remainingSec = Math.ceil(remainingMs / 1000);

    if (remainingSec <= 0) {
      self.postMessage({ type: 'countdownEnded' });
      return;
    }

    self.postMessage({ type: 'countdown', remainingMs, remainingSec });

    countdownTimer = setInterval(() => {
      const now = Date.now();
      const remainingMs = targetTime - now;
      const remainingSec = Math.ceil(remainingMs / 1000);

      if (remainingSec <= 0) {
        if (countdownTimer !== null) {
          clearInterval(countdownTimer);
          countdownTimer = null;
        }
        self.postMessage({ type: 'countdownEnded' });
      } else {
        self.postMessage({ type: 'countdown', remainingMs, remainingSec });
      }
    }, interval);
  }

  if (type === 'stopCountdown') {
    if (countdownTimer !== null) {
      clearInterval(countdownTimer);
      countdownTimer = null;
    }
  }

  if (type === 'stopEndTimer') {
    if (endTimer !== null) {
      clearInterval(endTimer);
      endTimer = null;
    }
  }

  if (type === 'stop') {
    if (endTimer !== null) {
      clearInterval(endTimer);
      endTimer = null;
    }
    if (countdownTimer !== null) {
      clearInterval(countdownTimer);
      countdownTimer = null;
    }
  }
};
