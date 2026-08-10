import { NextResponse } from "next/server";

export const dynamic = "force-dynamic";

function escapeHtml(value: string) {
  return value
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

export function GET() {
  const siteKey = process.env.NEXT_PUBLIC_TURNSTILE_SITE_KEY || "";
  const safeSiteKey = escapeHtml(siteKey);
  const html = `<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title>安全验证</title>
  <style>
    html,body{margin:0;width:100%;height:100%;overflow:hidden;background:transparent}
    body{display:grid;place-items:center;color:#52525b;font:14px system-ui,-apple-system,"Segoe UI",sans-serif}
    #challenge{min-height:65px;display:grid;place-items:center}
    #status{display:none}
  </style>
</head>
<body>
  <div id="challenge"></div>
  <div id="status" role="status"></div>
  <script>
    const bridge = window.MengzhenTurnstile;
    const siteKey = "${safeSiteKey}";
    const status = document.getElementById("status");
    const reportError = (message) => {
      if (status) status.textContent = message;
      if (bridge && bridge.onError) bridge.onError(message);
    };
    window.onMengzhenTurnstileLoad = function () {
      if (!siteKey) { reportError("安全验证服务未配置"); return; }
      window.turnstile.render("#challenge", {
        sitekey: siteKey,
        action: "auth",
        appearance: "interaction-only",
        "before-interactive-callback": function () {
          if (bridge && bridge.onInteractive) bridge.onInteractive();
        },
        callback: function (token) {
          if (bridge && bridge.onToken) bridge.onToken(token);
        },
        "expired-callback": function () { reportError("安全验证已过期，请重试"); },
        "error-callback": function () { reportError("安全验证加载失败，请重试"); },
        "unsupported-callback": function () { reportError("当前 WebView 不支持安全验证"); }
      });
    };
  </script>
  <script src="https://challenges.cloudflare.com/turnstile/v0/api.js?onload=onMengzhenTurnstileLoad&render=explicit" async defer></script>
</body>
</html>`;
  return new NextResponse(html, {
    headers: {
      "Content-Type": "text/html; charset=utf-8",
      "Cache-Control": "no-store, max-age=0",
    },
  });
}
