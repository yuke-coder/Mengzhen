"use client";

import { useEffect } from "react";
import { Brain, X } from "lucide-react";
import Link from "next/link";

interface SaveRecordModalProps {
  isOpen: boolean;
  onClose: () => void;
  isLoggedIn: boolean;
  onLogin: () => void;
  onSaveAnyway?: () => void;
  message: string;
  type: "success" | "login-prompt";
}

export function SaveRecordModal({
  isOpen,
  onClose,
  isLoggedIn,
  onLogin,
  message,
  type,
}: SaveRecordModalProps) {
  // ESC 键关闭
  useEffect(() => {
    const handleEsc = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    if (isOpen) {
      document.addEventListener("keydown", handleEsc);
      document.body.style.overflow = "hidden";
    }
    return () => {
      document.removeEventListener("keydown", handleEsc);
      document.body.style.overflow = "";
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center">
      {/* 背景遮罩 */}
      <div
        className="absolute inset-0 bg-black/60 backdrop-blur-sm"
        onClick={onClose}
      />

      {/* 弹窗内容 */}
      <div className="relative bg-card border border-border rounded-2xl shadow-2xl w-[90%] max-w-[400px] p-6 animate-in fade-in zoom-in-95 duration-200">
        {/* 关闭按钮 */}
        <button
          onClick={onClose}
          className="absolute top-4 right-4 p-2 rounded-lg text-muted-foreground hover:text-foreground hover:bg-muted transition-colors"
        >
          <X className="w-5 h-5" />
        </button>

        {/* 图标 */}
        <div className="flex justify-center mb-4">
          {type === "success" ? (
            <div className="w-16 h-16 rounded-full bg-[var(--brand-start)]/20 flex items-center justify-center">
              <Brain className="w-8 h-8 text-[var(--brand-start)]" />
            </div>
          ) : (
            <div className="w-16 h-16 rounded-full bg-[var(--brand-start)]/20 flex items-center justify-center">
              <Brain className="w-8 h-8 text-[var(--brand-start)]" />
            </div>
          )}
        </div>

        {/* 标题 */}
        <h3 className="text-xl font-bold text-center mb-2">
          {type === "success" ? "保存成功" : "登录提示"}
        </h3>

        {/* 消息 */}
        <p className="text-muted-foreground text-center mb-6">{message}</p>

        {/* 操作按钮 */}
        <div className="flex flex-col gap-3">
          {type === "success" ? (
            <>
              <Link
                href="/history"
                className="w-full py-3 px-4 bg-[var(--brand-start)] text-white rounded-xl font-medium text-center hover:opacity-90 transition-opacity"
                onClick={onClose}
              >
                查看我的记录
              </Link>
              <button
                onClick={onClose}
                className="w-full py-3 px-4 bg-muted text-foreground rounded-xl font-medium hover:bg-muted/80 transition-colors"
              >
                继续创作
              </button>
            </>
          ) : (
            <>
              <button
                onClick={onLogin}
                className="w-full py-3 px-4 bg-[var(--brand-start)] text-white rounded-xl font-medium hover:opacity-90 transition-opacity"
              >
                登录
              </button>
              <button
                onClick={onClose}
                className="w-full py-3 px-4 bg-muted text-foreground rounded-xl font-medium hover:bg-muted/80 transition-colors"
              >
                暂不登录
              </button>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
