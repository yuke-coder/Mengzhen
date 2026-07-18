"use client";

import React, { useRef, useState, useLayoutEffect } from "react";
import { PlayMode } from "@/lib/task-types";
import { cn } from "@/lib/utils";
import { Settings, CalendarClock } from "lucide-react";

interface ModeSwitchProps {
  mode: PlayMode;
  onModeChange: (mode: PlayMode) => void;
}

const MODES: { key: PlayMode; label: string; icon: React.ComponentType<{ className?: string }> }[] = [
  { key: "default", label: "默认设置", icon: Settings },
  { key: "custom", label: "自定义任务", icon: CalendarClock },
];

export function ModeSwitch({ mode, onModeChange }: ModeSwitchProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const btnRefs = useRef<Record<string, HTMLButtonElement | null>>({});
  const [indicator, setIndicator] = useState({ x: 0, w: 0, opacity: 0 });

  useLayoutEffect(() => {
    const update = () => {
      const btn = btnRefs.current[mode];
      const container = containerRef.current;
      if (!btn || !container) return;
      const br = btn.getBoundingClientRect();
      const cr = container.getBoundingClientRect();
      setIndicator({ x: br.left - cr.left, w: br.width, opacity: 1 });
    };
    update();
    window.addEventListener("resize", update);
    return () => window.removeEventListener("resize", update);
  }, [mode]);

  const btnBase = "relative z-[1] flex-none flex items-center gap-1 sm:gap-1.5 px-3 sm:px-4 py-1.5 sm:py-2 rounded-lg text-xs sm:text-sm font-medium whitespace-nowrap transition-colors duration-300 cursor-pointer select-none active:scale-[0.96] touch-manipulation";

  return (
    <div
      ref={containerRef}
      className="relative inline-flex items-center gap-0.5 sm:gap-1 p-1 rounded-xl"
    >
      <div
        className="absolute rounded-lg bg-gradient-to-br from-[var(--brand-start)] to-[var(--brand-end)] shadow-md shadow-[var(--brand-start)]/25 transition-all duration-300 ease-out pointer-events-none"
        style={{ transform: `translateX(${indicator.x}px)`, width: indicator.w, top: 4, bottom: 4, opacity: indicator.opacity }}
      />
      {MODES.map(({ key, label, icon: Icon }) => (
        <button
          key={key}
          ref={el => { btnRefs.current[key] = el; }}
          onClick={() => onModeChange(key)}
          className={cn(btnBase, mode === key ? "text-white" : "text-muted-foreground hover:text-foreground")}
        >
          <Icon className="w-3 h-3 sm:w-3.5 sm:h-3.5 flex-none" />
          <span>{label}</span>
        </button>
      ))}
    </div>
  );
}
