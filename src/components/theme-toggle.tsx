"use client";

import { useState, useEffect } from "react";
import { Sun, Moon, Monitor } from "lucide-react";
import { cn } from "@/lib/utils";
import { useTheme, type Theme } from "@/lib/theme-context";

export function ThemeToggle() {
  const { theme, setTheme } = useTheme();
  const [mounted, setMounted] = useState(false);
  const [showTip, setShowTip] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  const themes: { value: Theme; label: string; icon: typeof Sun }[] = [
    { value: "light", label: "亮色模式", icon: Sun },
    { value: "dark", label: "暗色模式", icon: Moon },
    { value: "system", label: "跟随系统", icon: Monitor },
  ];

  const currentIndex = themes.findIndex((t) => t.value === theme);
  const nextTheme = themes[(currentIndex + 1) % 3];

  const cycleTheme = () => {
    setTheme(nextTheme.value);
  };

  if (!mounted) {
    return <div className="w-9 h-9 rounded-lg bg-muted/50 border border-border/50" aria-hidden="true" />;
  }

  return (
    <div className="relative" onMouseEnter={() => setShowTip(true)} onMouseLeave={() => setShowTip(false)}>
      <button
        onClick={cycleTheme}
        className={`relative w-9 h-9 rounded-lg flex items-center justify-center bg-muted/50 hover:bg-muted active:bg-muted/80 border border-border/50 hover:border-border transition-all duration-200 group overflow-hidden focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background`}
        title={`当前：${themes[currentIndex].label}（点击切换为 ${nextTheme.label}）`}
      >
        <div className="relative w-[18px] h-[18px]">
          {themes.map(({ value, icon: Icon }) => (
            <Icon
              key={value}
              className={cn(
                "absolute inset-0 h-[18px] w-[18px] transition-all duration-300 ease-out",
                theme === value
                  ? "rotate-0 scale-100 opacity-100 text-primary"
                  : value === nextTheme.value
                    ? "rotate-45 scale-50 opacity-0 text-primary/40"
                    : "-rotate-45 scale-0 opacity-0 text-primary/20"
              )}
            />
          ))}
        </div>
        <div className="absolute inset-0 rounded-lg bg-gradient-to-br from-primary/10 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-200" />
        <div className="absolute bottom-[3px] left-1/2 -translate-x-1/2 flex gap-[3px]">
          {themes.map(({ value }) => (
            <span
              key={value}
              className={cn(
                "w-[3px] h-[3px] rounded-full transition-all duration-300",
                theme === value ? "bg-primary scale-100" : "bg-muted-foreground/30 scale-75"
              )}
            />
          ))}
        </div>
      </button>
      <div
        className={cn(
          "absolute top-full mt-2 left-1/2 -translate-x-1/2 px-2.5 py-1 rounded-md text-xs font-medium",
          "bg-popover text-popover-foreground border border-border shadow-lg",
          "pointer-events-none whitespace-nowrap z-50",
          "transition-opacity duration-150 origin-bottom",
          showTip ? "opacity-100 translate-y-0" : "opacity-0 translate-y-1"
        )}
      >
        {themes[currentIndex].label}
        <span className="text-muted-foreground ml-1">→ {nextTheme.label}</span>
        <div className="absolute -top-1 left-1/2 -translate-x-1/2 w-2 h-2 rotate-45 bg-popover border-l border-t border-border" />
      </div>
    </div>
  );
}
