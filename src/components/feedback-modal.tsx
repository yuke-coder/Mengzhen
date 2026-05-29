"use client";

import React, { useEffect, useState } from "react";
import { cn } from "@/lib/utils";
import { CheckCircle2, XCircle, AlertTriangle, Info } from "lucide-react";

export type FeedbackType = "success" | "error" | "warning" | "info";

interface FeedbackModalProps {
  visible: boolean;
  type: FeedbackType;
  title: string;
  message?: string;
  confirmText?: string;
  cancelText?: string;
  onConfirm?: () => void;
  onCancel?: () => void;
  showCancel?: boolean;
}

const ICON_MAP: Record<FeedbackType, React.ElementType> = {
  success: CheckCircle2,
  error: XCircle,
  warning: AlertTriangle,
  info: Info,
};

const COLOR_MAP: Record<FeedbackType, { icon: string; border: string; bg: string }> = {
  success: { icon: "text-emerald-400", border: "border-emerald-500/30", bg: "bg-emerald-500/10" },
  error: { icon: "text-red-400", border: "border-red-500/30", bg: "bg-red-500/10" },
  warning: { icon: "text-amber-400", border: "border-amber-500/30", bg: "bg-amber-500/10" },
  info: { icon: "text-blue-400", border: "border-blue-500/30", bg: "bg-blue-500/10" },
};

export function FeedbackModal({
  visible,
  type,
  title,
  message,
  confirmText = "确认",
  cancelText = "取消",
  onConfirm,
  onCancel,
  showCancel = false,
}: FeedbackModalProps) {
  const [animating, setAnimating] = useState(false);
  const [show, setShow] = useState(false);

  useEffect(() => {
    if (visible) {
      setShow(true);
      requestAnimationFrame(() => setAnimating(true));
    } else {
      setAnimating(false);
      const timer = setTimeout(() => setShow(false), 300);
      return () => clearTimeout(timer);
    }
  }, [visible]);

  if (!show) return null;

  const Icon = ICON_MAP[type];
  const colors = COLOR_MAP[type];

  return (
    <div
      className={cn(
        "fixed inset-0 z-[200] flex items-center justify-center transition-all duration-300",
        animating ? "bg-black/40 backdrop-blur-sm" : "bg-black/0"
      )}
      onClick={onCancel}
    >
      <div
        className={cn(
          "relative max-w-sm w-full mx-4 rounded-2xl border overflow-hidden transition-all duration-300",
          "bg-background/95 backdrop-blur-xl shadow-2xl",
          colors.border,
          animating ? "scale-100 opacity-100" : "scale-95 opacity-0"
        )}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="p-6 space-y-4">
          <div className="flex items-start gap-4">
            <div className={cn("flex-shrink-0 p-2 rounded-xl", colors.bg)}>
              <Icon className={cn("w-6 h-6", colors.icon)} />
            </div>
            <div className="flex-1 min-w-0">
              <h3 className="text-base font-semibold text-foreground">{title}</h3>
              {message && (
                <p className="mt-1.5 text-sm text-muted-foreground leading-relaxed">
                  {message}
                </p>
              )}
            </div>
          </div>

          <div className="flex items-center justify-end gap-2 pt-2">
            {showCancel && (
              <button
                onClick={onCancel}
                className="px-4 py-2 rounded-lg text-sm font-medium text-muted-foreground hover:text-foreground hover:bg-muted/80 border border-border/60 transition-all cursor-pointer"
              >
                {cancelText}
              </button>
            )}
            <button
              onClick={onConfirm}
              className={cn(
                "px-4 py-2 rounded-lg text-sm font-medium text-white transition-all cursor-pointer",
                type === "success" && "bg-emerald-500 hover:bg-emerald-600",
                type === "error" && "bg-red-500 hover:bg-red-600",
                type === "warning" && "bg-amber-500 hover:bg-amber-600",
                type === "info" && "bg-blue-500 hover:bg-blue-600"
              )}
            >
              {confirmText}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
