"use client";

import { useState, useEffect, useRef, type PointerEvent } from "react";
import { Sun, Moon, Monitor } from "lucide-react";
import { cn } from "@/lib/utils";
import { useTheme, type Theme } from "@/lib/theme-context";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

const themes: { value: Theme; label: string; description: string; icon: typeof Sun }[] = [
  { value: "light", label: "浅色模式", description: "始终使用浅色主题", icon: Sun },
  { value: "dark", label: "深色模式", description: "始终使用深色主题", icon: Moon },
  { value: "system", label: "自动模式", description: "跟随系统主题设置", icon: Monitor },
];

const triggerClass =
  "inline-flex h-8 w-8 sm:h-9 sm:w-9 items-center justify-center rounded-full border-0 bg-black/[0.05] text-current hover:bg-black/[0.09] focus-visible:bg-black/[0.09] dark:bg-white/[0.12] dark:hover:bg-white/[0.16] dark:focus-visible:bg-white/[0.16] focus-visible:outline-none";

const menuClass =
  "w-[154px] overflow-hidden rounded-md border border-black/[0.08] bg-white p-0 text-zinc-950 shadow-[0_0_1px_rgba(0,0,0,.3),0_4px_14px_rgba(0,0,0,.1)] dark:border-white/[0.08] dark:bg-[rgb(67,68,74)] dark:text-[rgb(249,249,249)] dark:shadow-[inset_0_0_0_1px_rgba(255,255,255,.1),0_4px_14px_rgba(0,0,0,.25)]";

const itemBaseClass =
  "rounded-none px-4 py-2 text-current hover:bg-black/[0.05] focus:bg-black/[0.05] active:bg-black/[0.09] dark:hover:bg-white/[0.12] dark:focus:bg-white/[0.12] dark:active:bg-white/[0.16]";

export function ThemeToggle() {
  const { theme, resolvedTheme, setTheme } = useTheme();
  const [mounted, setMounted] = useState(false);
  const [open, setOpen] = useState(false);
  const closeTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    setMounted(true);
    return () => {
      if (closeTimer.current) clearTimeout(closeTimer.current);
    };
  }, []);

  const currentTheme = themes.find((item) => item.value === theme) ?? themes[2];
  const Icon = currentTheme.icon;
  const resolvedLabel = resolvedTheme === "dark" ? "深色" : "浅色";
  const openMenu = (event: PointerEvent) => {
    if (event.pointerType !== "mouse") return;
    if (closeTimer.current) clearTimeout(closeTimer.current);
    setOpen(true);
  };
  const closeMenu = (event: PointerEvent) => {
    if (event.pointerType === "mouse") closeTimer.current = setTimeout(() => setOpen(false), 120);
  };

  if (!mounted) {
    return <div className="w-8 h-8 sm:w-9 sm:h-9 rounded-full bg-[#2f3037]" aria-hidden="true" />;
  }

  return (
    <DropdownMenu open={open} onOpenChange={setOpen} modal={false}>
      <div
        className="relative"
        onPointerEnter={openMenu}
        onPointerLeave={closeMenu}
      >
        <DropdownMenuTrigger asChild>
          <button
            type="button"
            className={triggerClass}
            style={{ transform: "none" }}
            title={`当前：${currentTheme.label}`}
            aria-label="切换页面模式"
          >
            <Icon className="h-[18px] w-[18px]" />
          </button>
        </DropdownMenuTrigger>
        <DropdownMenuContent
          align="end"
          sideOffset={8}
          className={menuClass}
          onPointerEnter={openMenu}
          onPointerLeave={closeMenu}
        >
          <div className="py-1">
            {themes.map(({ value, label, description, icon: ItemIcon }) => (
              <DropdownMenuItem
                key={value}
                onSelect={() => {
                  setTheme(value);
                  setOpen(false);
                }}
                className={cn(
                  itemBaseClass,
                  "items-start gap-[9px]",
                  theme === value && "bg-[rgb(239,248,255)] font-semibold hover:bg-[rgb(239,248,255)] focus:bg-[rgb(239,248,255)] dark:bg-[rgba(84,169,255,.2)] dark:hover:bg-[rgba(84,169,255,.2)] dark:focus:bg-[rgba(84,169,255,.2)]"
                )}
              >
                <ItemIcon className="mt-0.5 size-[18px] shrink-0 text-current" />
                <span className="min-w-0">
                  <span className="block whitespace-nowrap text-[14px] leading-[18px]">{label}</span>
                  <span className="mt-0.5 block whitespace-nowrap text-[12px] font-normal leading-4 text-zinc-950/60 dark:text-[rgba(249,249,249,.6)]">{description}</span>
                </span>
              </DropdownMenuItem>
            ))}
          </div>
          {theme === "system" && (
            <>
              <div className="h-px bg-black/[0.08] dark:bg-white/[0.08]" />
              <div className="px-3 py-2 text-xs leading-4 text-zinc-950/60 dark:text-[rgba(249,249,249,.6)]">
                当前跟随系统：<span className="text-zinc-950/80 dark:text-[rgba(249,249,249,.8)]">{resolvedLabel}</span>
              </div>
            </>
          )}
        </DropdownMenuContent>
      </div>
    </DropdownMenu>
  );
}
