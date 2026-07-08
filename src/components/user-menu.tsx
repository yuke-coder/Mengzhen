'use client';

import { useState, useRef, useEffect, useCallback } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/lib/auth-context';
import { Spinner } from '@/components/ui/spinner';
import { cn } from '@/lib/utils';
import {
  LogOut,
  History,
  LogIn,
  UserPlus,
  User,
  MapPin,
  Calendar,
  FileText,
  Heart
} from 'lucide-react';

const GENDER_MAP: Record<string, string> = {
  male: '男',
  female: '女',
  secret: '保密',
};

function ProfileCard({ user, onEditProfile }: { user: NonNullable<ReturnType<typeof useAuth>['user']>; onEditProfile: () => void }) {
  const hasCustomAvatar = !!user.avatar_url;

  return (
    <div className="px-4 py-3 border-b border-black/[0.06] dark:border-white/[0.08]">
      <div className="flex items-center gap-3">
        <div className="w-12 h-12 rounded-full overflow-hidden border-2 border-[var(--brand-start)]/30 flex-shrink-0">
          {hasCustomAvatar && user.avatar_url ? (
            <img src={user.avatar_url} alt="头像" className="w-full h-full object-cover" />
          ) : (
            <div className="w-full h-full bg-gradient-to-br from-[var(--brand-start)]/20 to-[var(--brand-end)]/20 flex items-center justify-center">
              <User className="w-6 h-6 text-[var(--brand-end)]" />
            </div>
          )}
        </div>
        <div className="flex-1 min-w-0">
          <p className="text-sm font-semibold text-foreground truncate">
            {user.nickname || user.username}
          </p>
          {user.nickname && (
            <p className="text-xs text-muted-foreground/60 truncate">@{user.username}</p>
          )}
        </div>
      </div>

      {user.signature ? (
        <p className="mt-2 text-xs text-muted-foreground italic line-clamp-2 pl-1">{user.signature}</p>
      ) : (
        <p className="mt-2 text-xs text-muted-foreground/40 italic pl-1">暂无签名</p>
      )}

      <button
        onClick={onEditProfile}
        className="mt-2 w-full px-3 py-1.5 rounded-lg text-xs font-medium text-center transition-all border border-black/[0.08] dark:border-white/[0.1] text-muted-foreground hover:text-foreground hover:border-[var(--brand-start)]/40 bg-black/[0.02] dark:bg-white/[0.04] hover:bg-black/[0.04] dark:hover:bg-white/[0.06]"
      >
        编辑个人资料
      </button>

      <div className="mt-2.5 space-y-1.5">
        {user.location && (
          <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
            <MapPin className="w-3 h-3 flex-shrink-0 text-[var(--brand-start)]/60" />
            <span className="truncate">{user.location}</span>
          </div>
        )}
        {user.gender && user.gender !== 'secret' && (
          <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
            <Heart className="w-3 h-3 flex-shrink-0 text-[var(--brand-start)]/60" />
            <span>{GENDER_MAP[user.gender] || user.gender}</span>
          </div>
        )}
        {user.birthday && (
          <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
            <Calendar className="w-3 h-3 flex-shrink-0 text-[var(--brand-start)]/60" />
            <span>{user.birthday}</span>
          </div>
        )}
        {user.bio && (
          <div className="flex items-start gap-1.5 text-xs text-muted-foreground">
            <FileText className="w-3 h-3 flex-shrink-0 mt-0.5 text-[var(--brand-start)]/60" />
            <span className="line-clamp-3">{user.bio}</span>
          </div>
        )}
      </div>
    </div>
  );
}

export function UserMenu() {
  const { user, loading, logout } = useAuth();
  const router = useRouter();
  const [isOpen, setIsOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);
  const hideTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  const delayedHide = useCallback(() => {
    hideTimeoutRef.current = setTimeout(() => setIsOpen(false), 300);
  }, []);

  const cancelDelayedHide = useCallback(() => {
    if (hideTimeoutRef.current) {
      clearTimeout(hideTimeoutRef.current);
      hideTimeoutRef.current = null;
    }
  }, []);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  useEffect(() => {
    return () => {
      if (hideTimeoutRef.current) clearTimeout(hideTimeoutRef.current);
    };
  }, []);

  const handleLogout = async () => {
    await logout();
    setIsOpen(false);
    router.push('/');
  };

  if (loading) {
    return (
      <div className="shrink-0 w-9 h-9 flex items-center justify-center">
        <Spinner size="sm" className="h-4 w-4 text-muted-foreground" />
      </div>
    );
  }

  if (user) {
    const hasCustomAvatar = !!user.avatar_url;

    return (
      <div className="relative" ref={menuRef}>
        <button
          onMouseEnter={() => { cancelDelayedHide(); setIsOpen(true); }}
          onClick={() => setIsOpen(!isOpen)}
          className={cn(
            "shrink-0 w-9 h-9 rounded-full overflow-hidden",
            "border-2 border-[var(--brand-start)]/30",
            "transition-all duration-200 active:scale-95",
            "hover:border-[var(--brand-start)]/60 hover:shadow-lg hover:shadow-[var(--brand-start)]/25",
            "focus:outline-none focus:ring-2 focus:ring-[var(--brand-start)]/40 focus:ring-offset-2 focus:ring-offset-background"
          )}
        >
          {hasCustomAvatar && user.avatar_url ? (
            <img src={user.avatar_url} alt="头像" className="block w-full h-full object-cover" />
          ) : (
            <div className="w-full h-full bg-gradient-to-br from-[var(--brand-start)]/20 to-[var(--brand-end)]/20 flex items-center justify-center">
              <User className="w-1/2 h-1/2 text-[var(--brand-end)]" />
            </div>
          )}
        </button>

        <div
          className={cn(
            "absolute right-0 top-full pt-1.5 z-[10000] isolation-isolate",
            isOpen ? "pointer-events-auto" : "pointer-events-none"
          )}
          onMouseEnter={cancelDelayedHide}
          onMouseLeave={delayedHide}
        >
          <div
            data-user-menu-dropdown
            className={cn(
              "py-2 min-w-[240px] max-w-[300px]",
              "rounded-xl bg-white/60 dark:bg-black/40",
              "backdrop-blur-[8px] backdrop-saturate-115",
              "-webkit-backdrop-blur-[8px] -webkit-backdrop-saturate-115",
              "border border-white/20 dark:border-white/10",
              "shadow-lg shadow-black/[0.08]",
              "transition-all duration-200 ease-out origin-top-right will-change-[transform,opacity]",
              isOpen
                ? "opacity-100 scale-100 translate-y-0"
                : "opacity-0 scale-95 -translate-y-1"
            )}
          >
          <ProfileCard user={user} onEditProfile={() => { setIsOpen(false); router.push('/profile'); }} />

          <div className="py-1 px-2">
            <Link
              href="/history"
              onClick={() => setIsOpen(false)}
              className={cn(
                "flex items-center gap-3 w-full px-3 py-2 text-left rounded-lg",
                "text-sm text-muted-foreground hover:text-foreground",
                "hover:bg-black/[0.04] dark:hover:bg-white/[0.06] transition-colors duration-150"
              )}
            >
              <History className="w-4 h-4" />
              <span>我的音频</span>
            </Link>
          </div>

          <div className="border-t border-black/[0.06] dark:border-white/[0.08] pt-1 pb-1 px-2">
            <button
              onClick={handleLogout}
              className={cn(
                "flex items-center gap-3 w-full px-3 py-2 text-left rounded-lg",
                "text-sm text-destructive hover:text-destructive",
                "hover:bg-destructive/10 transition-colors duration-150"
              )}
            >
              <LogOut className="w-4 h-4" />
              <span>退出登录</span>
            </button>
          </div>
        </div>
      </div>
    </div>
    );
  }

  return (
    <div className="flex items-center gap-2">
      <Link
        href="/auth/login"
        title="登录"
        aria-label="登录"
        className={cn(
          "md:hidden flex shrink-0 w-9 h-9 items-center justify-center rounded-full",
          "text-muted-foreground hover:text-foreground",
          "hover:bg-black/[0.05] dark:hover:bg-white/[0.08] active:bg-black/[0.09] dark:active:bg-white/[0.12]",
          "active:scale-95 transition-all duration-200"
        )}
      >
        <LogIn className="w-[18px] h-[18px]" />
      </Link>
      <Link
        href="/auth/login"
        className={cn(
          "hidden md:flex items-center gap-2 px-4 py-2 rounded-full",
          "text-sm text-muted-foreground hover:text-foreground",
          "hover:bg-black/[0.05] dark:hover:bg-white/[0.05] transition-all duration-200"
        )}
      >
        <LogIn className="w-4 h-4" />
        <span>登录</span>
      </Link>
      <Link
        href="/auth/register"
        className={cn(
          "hidden md:flex items-center gap-2 px-4 py-2 rounded-full",
          "text-sm font-medium text-white",
          "bg-gradient-to-r from-[var(--brand-start)] to-[var(--brand-end)]",
          "hover:shadow-lg hover:shadow-[var(--brand-start)]/30",
          "hover:-translate-y-0.5 active:scale-95",
          "transition-all duration-200"
        )}
      >
        <UserPlus className="w-4 h-4" />
        <span>注册</span>
      </Link>
    </div>
  );
}
