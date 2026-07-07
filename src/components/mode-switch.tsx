"use client";

import React from "react";
import { PlayMode } from "@/lib/task-types";
import { cn } from "@/lib/utils";
import { Settings, CalendarClock } from "lucide-react";

interface ModeSwitchProps {
  mode: PlayMode;
  onModeChange: (mode: PlayMode) => void;
}

export function ModeSwitch({ mode, onModeChange }: ModeSwitchProps) {
  return (
    <div className="flex items-center gap-0.5 sm:gap-1 p-0.5 sm:p-1 rounded-xl w-fit mx-auto max-w-full">
      <button
        onClick={() => onModeChange("default")}
        className={cn(
          "flex-none flex items-center gap-1 sm:gap-1.5 px-2 sm:px-3 py-1.5 sm:py-2 rounded-lg text-xs sm:text-sm font-medium whitespace-nowrap transition-all duration-200 cursor-pointer",
          mode === "default"
            ? "bg-[var(--brand-start)] text-white shadow-md shadow-[var(--brand-start)]/20"
            : "text-muted-foreground hover:text-foreground hover:bg-muted/80"
        )}
      >
        <Settings className="w-3 h-3 sm:w-3.5 sm:h-3.5 flex-none" />
        <span>默认设置</span>
      </button>
      <button
        onClick={() => onModeChange("custom")}
        className={cn(
          "flex-none flex items-center gap-1 sm:gap-1.5 px-2 sm:px-3 py-1.5 sm:py-2 rounded-lg text-xs sm:text-sm font-medium whitespace-nowrap transition-all duration-200 cursor-pointer",
          mode === "custom"
            ? "bg-[var(--brand-start)] text-white shadow-md shadow-[var(--brand-start)]/20"
            : "text-muted-foreground hover:text-foreground hover:bg-muted/80"
        )}
      >
        <CalendarClock className="w-3 h-3 sm:w-3.5 sm:h-3.5 flex-none" />
        <span>自定义任务</span>
      </button>
    </div>
  );
}
