'use client';

import { useState, useRef, useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/lib/auth-context';
import { cn } from '@/lib/utils';
import { 
  LogOut, 
  History, 
  Settings,
  Lock,
  LogIn,
  UserPlus,
  Loader2
} from 'lucide-react';

export function UserMenu() {
  const { user, loading, logout } = useAuth();
  const router = useRouter();
  const [isOpen, setIsOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);
  const hideTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  const delayedHide = () => {
    hideTimeoutRef.current = setTimeout(() => setIsOpen(false), 200);
  };

  const cancelDelayedHide = () => {
    if (hideTimeoutRef.current) {
      clearTimeout(hideTimeoutRef.current);
      hideTimeoutRef.current = null;
    }
  };

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleLogout = async () => {
    await logout();
    setIsOpen(false);
    router.push('/');
  };

  if (loading) {
    return (
      <div className="flex items-center gap-2 px-4 py-2">
        <Loader2 className="w-4 h-4 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (user) {
    return (
      <div 
        className="relative" 
        ref={menuRef}
        onMouseLeave={delayedHide}
      >
        {/* 圆形头像 — hover 触发下拉菜单 */}
        <button
          onMouseEnter={() => { cancelDelayedHide(); setIsOpen(true); }}
          className={cn(
            "w-9 h-9 rounded-full overflow-hidden",
            "bg-gradient-to-br from-pink-500 to-purple-500",
            "ring-2 ring-white/20 shadow-md",
            "hover:ring-4 hover:shadow-lg hover:shadow-pink-500/20",
            "hover:scale-110 active:scale-95",
            "transition-all duration-200 cursor-pointer"
          )}
        >
          {user.avatar_url ? (
            <img 
              src={user.avatar_url} 
              alt={user.username} 
              className="w-full h-full object-cover"
            />
          ) : (
            <div className="w-full h-full flex items-center justify-center">
              <Lock className="w-4 h-4 text-white/80" />
            </div>
          )}
        </button>

        {/* 下拉菜单 */}
        <div 
          className={cn(
            "absolute right-0 top-full mt-2 py-2 min-w-[180px]",
            "rounded-xl bg-background/95 backdrop-blur-xl",
            "border border-border/50 shadow-xl shadow-black/10",
            "transition-all duration-200 ease-out",
            isOpen 
              ? "opacity-100 scale-100 translate-y-0 pointer-events-auto" 
              : "opacity-0 scale-95 -translate-y-1 pointer-events-none"
          )}
          onMouseEnter={cancelDelayedHide}
        >
          {/* 用户信息区 */}
          <div className="px-4 py-3 border-b border-border/50 flex items-center gap-3">
            <div className="w-10 h-10 rounded-full bg-gradient-to-br from-pink-500 to-purple-500 flex items-center justify-center overflow-hidden shadow-md ring-2 ring-white/20">
              {user.avatar_url ? (
                <img 
                  src={user.avatar_url} 
                  alt={user.username} 
                  className="w-full h-full object-cover"
                />
              ) : (
                <Lock className="w-4 h-4 text-white/80" />
              )}
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-sm font-medium text-foreground truncate">
                {user.nickname || user.username}
              </p>
              {user.signature && (
                <p className="text-xs text-muted-foreground truncate">
                  {user.signature}
                </p>
              )}
            </div>
          </div>
          
          <Link
            href="/profile"
            onClick={() => setIsOpen(false)}
            className={cn(
              "flex items-center gap-3 w-full px-4 py-2.5 text-left",
              "text-sm text-muted-foreground hover:text-foreground",
              "hover:bg-muted/50 transition-colors duration-150"
            )}
          >
            <Settings className="w-4 h-4" />
            <span>编辑资料</span>
          </Link>

          <Link
            href="/history"
            onClick={() => setIsOpen(false)}
            className={cn(
              "flex items-center gap-3 w-full px-4 py-2.5 text-left",
              "text-sm text-muted-foreground hover:text-foreground",
              "hover:bg-muted/50 transition-colors duration-150"
            )}
          >
            <History className="w-4 h-4" />
            <span>历史记录</span>
          </Link>
          
          <button
            onClick={handleLogout}
            className={cn(
              "flex items-center gap-3 w-full px-4 py-2.5 text-left",
              "text-sm text-destructive hover:text-destructive",
              "hover:bg-destructive/10 transition-colors duration-150"
            )}
          >
            <LogOut className="w-4 h-4" />
            <span>退出登录</span>
          </button>
        </div>
      </div>
    );
  }

  // 未登录状态
  return (
    <div className="flex items-center gap-2">
      <Link
        href="/auth/login"
        className={cn(
          "flex items-center gap-2 px-4 py-2 rounded-full",
          "text-sm text-muted-foreground hover:text-foreground",
          "hover:bg-pink-500/5 transition-all duration-200"
        )}
      >
        <LogIn className="w-4 h-4" />
        <span>登录</span>
      </Link>
      <Link
        href="/auth/register"
        className={cn(
          "flex items-center gap-2 px-4 py-2 rounded-full",
          "text-sm font-medium text-white",
          "bg-gradient-to-r from-pink-500 to-purple-500",
          "hover:shadow-lg hover:shadow-pink-500/30",
          "hover:-translate-y-0.5 hover:scale-105",
          "active:scale-95",
          "transition-all duration-200"
        )}
      >
        <UserPlus className="w-4 h-4" />
        <span>注册</span>
      </Link>
    </div>
  );
}
