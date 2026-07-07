'use client';

import Link from 'next/link';
import { useRouter, usePathname } from 'next/navigation';
import { UserMenu } from '@/components/user-menu';
import { ThemeToggle } from '@/components/theme-toggle';
import RippleButton from '@/components/RippleButton';
import {
  ChevronRight,
} from 'lucide-react';

/* ── Navbar Props ── */
export interface NavbarProps {
  /** 当前页面标识，影响导航高亮和 CTA 按钮文案 */
  activePage?: 'home' | 'settings' | string;
  /** 首页滚动到指定 section 的回调（首页专用） */
  onScrollToSection?: (id: string) => void;
}

/* ── 共享导航栏 ── */
export default function Navbar({ activePage, onScrollToSection }: NavbarProps) {
  const router = useRouter();
  const pathname = usePathname();
  // 自动识别当前页面：未传 activePage 时根据路径判断
  const resolvedPage = activePage || (pathname === '/' || pathname === '/settings' ? (pathname === '/' ? 'home' : 'settings') : 'home');
  const isHome = resolvedPage === 'home';
  // 登录/注册页面隐藏主题切换按钮
  const isAuthPage = pathname?.startsWith('/auth/');

  const scrollToSection = (id: string) => {
    if (onScrollToSection) {
      onScrollToSection(id);
    } else if (pathname === '/') {
      document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' });
    } else {
      router.push(`/#${id}`);
    }
  };

  return (
    <header id="main-navbar" className="fixed top-0 left-0 right-0 z-[9999] isolation-isolate bg-background/80 backdrop-blur-xl border-b border-border/50">
      <div className="max-w-5xl mx-auto px-3 sm:px-6 h-12 sm:h-14 flex items-center justify-between relative">
        {/* 左侧：Logo + 品牌名 */}
        <div className="flex items-center gap-3 sm:gap-4 z-30">
          <Link href="/" className="flex items-center gap-2 sm:gap-3 group">
            <img
              src="/logo.png"
              alt="梦枕"
              className="w-7 h-7 sm:w-9 sm:h-9 rounded-lg shadow-lg shadow-[inset_0_2px_6px_rgba(0,0,0,0.35)] transition-transform duration-300 group-hover:scale-110 z-30"
            />
            <span className="font-bold text-lg sm:text-xl tracking-tight">
              <span className="bg-gradient-to-r from-purple-500 via-purple-600 to-fuchsia-500 bg-clip-text text-transparent" suppressHydrationWarning>
                梦枕
              </span>
            </span>
          </Link>

          <div className="hidden md:block w-px h-6 bg-gradient-to-b from-transparent via-[var(--brand-start)]/30 to-transparent" />

          <div className="hidden md:flex items-center">
            <span className="relative text-sm font-medium tracking-wide" suppressHydrationWarning>
              <span className="bg-gradient-to-r from-[var(--brand-start)]/70 via-[var(--brand-mid)]/80 to-[var(--brand-end)]/70 bg-clip-text text-transparent drop-shadow-[0_1px_2px_rgba(0,0,0,0.3)]" suppressHydrationWarning>
                星河入眠
              </span>
              <span className="mx-1.5 text-[var(--brand-glow)]/50" suppressHydrationWarning>·</span>
              <span className="bg-gradient-to-r from-[var(--brand-mid)]/80 via-[var(--brand-end)]/90 to-[var(--brand-end)] bg-clip-text text-transparent drop-shadow-[0_1px_2px_rgba(0,0,0,0.3)]" suppressHydrationWarning>
                伴你梦枕
              </span>
            </span>
          </div>
        </div>

        {/* 中间：导航链接 */}
        <nav className="hidden md:flex items-center gap-2 absolute left-1/2 -translate-x-1/2">
          {/* 首页 */}
          {isHome ? (
            <span className="text-sm text-foreground/60 px-4 py-2 rounded-full bg-[var(--brand-start)]/5 cursor-default" suppressHydrationWarning>
              首页
            </span>
          ) : (
            <Link
              href="/"
              className="group relative text-sm text-muted-foreground hover:text-foreground transition-all duration-200 px-4 py-2 rounded-full hover:bg-[var(--brand-start)]/5"
            >
              <span className="relative z-10">首页</span>
              <span className="absolute bottom-1 left-1/2 -translate-x-1/2 w-0 h-0.5 bg-gradient-to-r from-[var(--brand-start)] to-[var(--brand-end)] rounded-full transition-all duration-300 group-hover:w-full" />
            </Link>
          )}

          {/* 功能 */}
          <button
            onClick={() => scrollToSection('features')}
            className="group relative text-sm text-muted-foreground hover:text-foreground transition-all duration-200 px-4 py-2 rounded-full hover:bg-[var(--brand-start)]/5"
          >
            <span className="relative z-10" suppressHydrationWarning>功能</span>
            <span className="absolute bottom-1 left-1/2 -translate-x-1/2 w-0 h-0.5 bg-gradient-to-r from-[var(--brand-start)] to-[var(--brand-end)] rounded-full transition-all duration-300 group-hover:w-full" />
          </button>

          {/* 展示模式 */}
          <button
            onClick={() => scrollToSection('display-mode')}
            className="group relative text-sm text-muted-foreground hover:text-foreground transition-all duration-200 px-4 py-2 rounded-full hover:bg-[var(--brand-start)]/5"
          >
            <span className="relative z-10" suppressHydrationWarning>展示模式</span>
            <span className="absolute bottom-1 left-1/2 -translate-x-1/2 w-0 h-0.5 bg-gradient-to-r from-[var(--brand-start)] to-[var(--brand-end)] rounded-full transition-all duration-300 group-hover:w-full" />
          </button>

          {/* CTA 按钮 */}
          {isHome ? (
            <div className="group relative ml-3">
              <span className="absolute -inset-1.5 bg-gradient-to-r from-[var(--brand-start)] via-[var(--brand-mid)] to-[var(--brand-end)] rounded-full blur-md opacity-0 group-hover:opacity-60 transition-all duration-300" />
              <RippleButton
                onClick={() => router.replace('/settings')}
                className="relative flex items-center gap-2 px-5 py-2 rounded-full bg-gradient-to-r from-[var(--brand-start)] via-[var(--brand-mid)] to-[var(--brand-end)] text-white font-semibold text-sm shadow-lg shadow-[var(--brand-start)]/30 transition-all duration-300 group-hover:shadow-xl group-hover:shadow-[var(--brand-start)]/50 group-hover:-translate-y-0.5"
              >
                <span className="absolute inset-0 -translate-x-full group-hover:translate-x-full transition-transform duration-500 ease-out">
                  <span className="absolute inset-0 bg-gradient-to-r from-transparent via-white/30 to-transparent" />
                </span>
                <img src="/logo.png" alt="梦枕" className="w-4 h-4 relative z-10 rounded shadow-md transition-transform duration-300 group-hover:scale-110" />
                <span suppressHydrationWarning className="relative z-10">免费体验</span>
                <span className="relative z-10 flex items-center">
                  <ChevronRight className="w-4 h-4 transition-all duration-300 group-hover:translate-x-1.5" />
                </span>
              </RippleButton>
            </div>
          ) : (
            <span className="text-sm text-foreground/60 ml-3 px-4 py-2 rounded-full bg-[var(--brand-start)]/10 cursor-default" suppressHydrationWarning>
              音频工作台
            </span>
          )}
        </nav>

         {/* 右侧：用户菜单 + 主题切换（登录/注册页隐藏） */}
         <div className="z-10 flex items-center gap-2 sm:gap-3">
            {!isAuthPage && <UserMenu />}
           {!isAuthPage && <ThemeToggle />}
         </div>
      </div>
    </header>
  );
}
