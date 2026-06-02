"use client";

import Link from "next/link";
import { Save, Loader2, X } from "lucide-react";
import { UserMenu } from "@/components/user-menu";
import Navbar, { type NavbarProps } from "@/components/navbar";
import { ProfileProvider, useProfile } from "@/lib/profile-context";
import { usePathname } from "next/navigation";

function SaveButton() {
  const { saving, submitHandler, cancelHandler } = useProfile();
  if (!submitHandler) return null;
  return (
    <>
      <button
        onClick={cancelHandler || undefined}
        className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg font-medium text-sm transition-all hover:bg-muted/80"
        style={{ color: "var(--muted-foreground)" }}
      >
        <X className="w-3.5 h-3.5" />
        取消
      </button>
      <button
        onClick={submitHandler}
        disabled={saving}
        className="flex items-center gap-2 px-4 py-1.5 rounded-lg font-medium text-sm transition-all hover:-translate-y-0.5 hover:shadow-lg active:scale-[0.98]"
        style={{
          background: "linear-gradient(135deg, var(--brand-start), var(--brand-end))",
          color: "white",
          opacity: saving ? 0.7 : 1
        }}
      >
        {saving ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Save className="w-3.5 h-3.5" />}
        保存
      </button>
    </>
  );
}

function ProfileNavbar() {
  return (
    <header className="fixed top-0 left-0 right-0 z-[400] bg-background/80 backdrop-blur-xl border-b border-border/50">
      <div className="max-w-5xl mx-auto px-6 h-14 flex items-center justify-between relative">
        <div className="flex items-center gap-4 z-30">
          <Link href="/" className="flex items-center gap-3 group">
            <img
              src="/logo.png"
              alt="梦枕"
              className="w-9 h-9 rounded-lg shadow-lg shadow-[inset_0_2px_6px_rgba(0,0,0,0.35)] transition-transform duration-300 group-hover:scale-110 z-30"
            />
            <span className="font-bold text-xl tracking-tight">
              <span className="bg-gradient-to-r from-purple-500 via-purple-600 to-fuchsia-500 bg-clip-text text-transparent">
                梦枕
              </span>
            </span>
          </Link>
          <div className="hidden md:block w-px h-6 bg-gradient-to-b from-transparent via-[var(--brand-start)]/30 to-transparent" />
          <div className="hidden md:flex items-center">
            <span className="relative text-sm font-medium tracking-wide">
              <span className="bg-gradient-to-r from-[var(--brand-start)]/70 via-[var(--brand-mid)]/80 to-[var(--brand-end)]/70 bg-clip-text text-transparent">
                星河入眠
              </span>
              <span className="mx-1.5 text-[var(--brand-glow)]/50">·</span>
              <span className="bg-gradient-to-r from-[var(--brand-mid)]/80 via-[var(--brand-end)]/90 to-[var(--brand-end)] bg-clip-text text-transparent">
                伴你梦枕
              </span>
            </span>
          </div>
        </div>
        <nav className="hidden md:flex items-center gap-2 absolute left-1/2 -translate-x-1/2">
          <Link href="/" className="group relative text-sm text-muted-foreground hover:text-foreground transition-all duration-200 px-4 py-2 rounded-full hover:bg-[var(--brand-start)]/5">
            <span className="relative z-10">首页</span>
            <span className="absolute bottom-1 left-1/2 -translate-x-1/2 w-0 h-0.5 bg-gradient-to-r from-[var(--brand-start)] to-[var(--brand-end)] rounded-full transition-all duration-300 group-hover:w-full" />
          </Link>
          <span className="text-sm text-foreground/60 ml-3 px-4 py-2 rounded-full bg-[var(--brand-start)]/10 cursor-default">编辑资料</span>
        </nav>
        <div className="z-10 flex items-center gap-3">
          <SaveButton />
          <UserMenu />
        </div>
      </div>
    </header>
  );
}

function ProfileLayoutInner({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen">
      <ProfileNavbar />
      <main className="pt-14 relative z-10">{children}</main>
    </div>
  );
}

export default function ProfileLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <ProfileProvider>
      <ProfileLayoutInner>{children}</ProfileLayoutInner>
    </ProfileProvider>
  );
}
