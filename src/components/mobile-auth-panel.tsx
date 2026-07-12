"use client";

import { useState } from "react";
import type { FormEvent, ReactNode } from "react";
import { useRouter } from "next/navigation";
import { Eye, EyeOff, LogIn } from "lucide-react";
import { Spinner } from "@/components/ui/spinner";
import { useAuth } from "@/lib/auth-context";

export function MobileAuthPanel() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const router = useRouter();
  const auth = useAuth();

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setError("");

    if (!username.trim() || !password) {
      setError("请填写用户名和密码");
      return;
    }

    setLoading(true);
    const result = await auth.loginOrRegister(username.trim(), password);
    setLoading(false);

    if (result.success) router.push("/");
    else setError(result.message);
  };

  return (
    <main className="relative min-h-[calc(100vh-3rem)] sm:min-h-[calc(100vh-3.5rem)] overflow-hidden bg-gradient-to-r from-[#ee7752] via-[#e73c7e] to-[#23a6d5] bg-[length:400%_400%] animate-gradient px-4 py-6">
      <div className="mx-auto flex min-h-full w-full max-w-[420px] items-center">
        <section className="w-full rounded-2xl bg-black/10 p-5 text-white backdrop-blur-sm">
          <div className="mb-7 text-center">
            <h1 className="mb-2 text-3xl font-bold tracking-tight">登录梦枕</h1>
            <p className="text-sm text-white/70">
              未注册账号将自动创建，用于同步保存你的助眠音频
            </p>
          </div>

          <form onSubmit={submit} className="space-y-4">
            <Field label="用户名">
              <input
                value={username}
                onChange={(event) => setUsername(event.target.value)}
                autoComplete="username"
                disabled={loading}
                required
                className="h-12 w-full rounded-xl border border-white/30 bg-transparent px-4 text-white outline-none transition placeholder:text-white/50 focus:border-white focus:ring-2 focus:ring-white/20"
                placeholder="请输入用户名"
              />
            </Field>

            <Field label="密码">
              <PasswordInput
                value={password}
                onChange={setPassword}
                shown={showPassword}
                onToggle={() => setShowPassword((shown) => !shown)}
                placeholder="请输入密码"
                autoComplete="current-password"
                disabled={loading}
              />
            </Field>

            {error && (
              <p className="rounded-xl border border-red-300/30 bg-red-950/20 p-3 text-sm text-red-100">
                {error}
              </p>
            )}

            <button
              type="submit"
              disabled={loading}
              className="flex h-12 w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-[var(--brand-start)] via-[var(--brand-mid)] to-[var(--brand-end)] text-base font-medium text-white shadow-lg shadow-[var(--brand-start)]/25 transition active:scale-[0.98] disabled:opacity-60"
            >
              {loading ? (
                <Spinner size="sm" className="h-5 w-5 text-white" />
              ) : (
                <>
                  <LogIn className="size-5" />
                  <span>登录 / 注册</span>
                </>
              )}
            </button>
          </form>
        </section>
      </div>
    </main>
  );
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="block text-sm font-medium">
      <span className="mb-1 block">{label}</span>
      {children}
    </label>
  );
}

function PasswordInput({
  value,
  onChange,
  shown,
  onToggle,
  placeholder,
  autoComplete,
  disabled,
}: {
  value: string;
  onChange: (value: string) => void;
  shown: boolean;
  onToggle: () => void;
  placeholder: string;
  autoComplete: string;
  disabled: boolean;
}) {
  return (
    <span className="relative block">
      <input
        type={shown ? "text" : "password"}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        autoComplete={autoComplete}
        disabled={disabled}
        required
        className="h-12 w-full rounded-xl border border-white/30 bg-transparent px-4 pr-11 text-white outline-none transition placeholder:text-white/50 focus:border-white focus:ring-2 focus:ring-white/20"
        placeholder={placeholder}
      />
      <button
        type="button"
        onClick={onToggle}
        className="absolute right-3 top-1/2 -translate-y-1/2 text-white/70"
        aria-label={shown ? "隐藏密码" : "显示密码"}
      >
        {shown ? <EyeOff className="size-5" /> : <Eye className="size-5" />}
      </button>
    </span>
  );
}
