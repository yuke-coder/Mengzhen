"use client";

import { Activity } from "lucide-react";
import { NumberStepperButton } from "@/components/number-stepper";
import { cn } from "@/lib/utils";

interface FadeControlsProps {
  enabled: boolean;
  fadeInDuration: number;
  fadeOutDuration: number;
  onEnabledChange: (enabled: boolean) => void;
  onFadeInDurationChange: (duration: number) => void;
  onFadeOutDurationChange: (duration: number) => void;
  showHint?: boolean;
  className?: string;
}

interface DurationControlProps {
  label: string;
  value: number;
  onChange: (value: number) => void;
}

function DurationControl({ label, value, onChange }: DurationControlProps) {
  return (
    <div className="space-y-1.5">
      <div className="flex items-center justify-between">
        <span className="text-xs text-muted-foreground/60 font-medium uppercase tracking-wider">{label}</span>
        <span className="text-sm font-mono text-foreground tabular-nums">{value}s</span>
      </div>
      <div className="py-0.5 flex items-center gap-1 sm:gap-2.5">
        <NumberStepperButton
          dir={-1}
          ariaLabel={`减少${label}时长（每次1秒）`}
          disabled={value <= 0}
          value={value}
          onChange={onChange}
          min={0}
          max={120}
          step={1}
          className="w-7 h-7 sm:w-8 sm:h-8"
          iconSize="w-2.5 h-2.5 sm:w-3 sm:h-3"
        />
        <input
          type="range"
          min={0}
          max={120}
          value={value}
          onChange={event => onChange(Number.parseInt(event.target.value, 10))}
          className="flex-1 h-2 rounded-full appearance-none bg-border/30 cursor-pointer [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:w-6 [&::-webkit-slider-thumb]:h-6 [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:bg-white [&::-webkit-slider-thumb]:shadow-md [&::-webkit-slider-thumb]:cursor-pointer [&::-webkit-slider-thumb]:transition-transform [&::-webkit-slider-thumb]:hover:scale-110 [&::-webkit-slider-thumb]:active:scale-95 [&::-webkit-slider-thumb]:border-2 [&::-webkit-slider-thumb]:border-[var(--brand-start)] sm:[&::-webkit-slider-thumb]:w-5 sm:[&::-webkit-slider-thumb]:h-5 sm:[&::-webkit-slider-thumb]:hover:scale-125"
          style={{
            background: `linear-gradient(to right, var(--brand-start) ${(value / 120) * 100}%, rgba(128,128,128,0.2) ${(value / 120) * 100}%)`,
          }}
        />
        <NumberStepperButton
          dir={1}
          ariaLabel={`增加${label}时长（每次1秒）`}
          disabled={value >= 120}
          value={value}
          onChange={onChange}
          min={0}
          max={120}
          step={1}
          className="w-7 h-7 sm:w-8 sm:h-8"
          iconSize="w-2.5 h-2.5 sm:w-3 sm:h-3"
        />
      </div>
    </div>
  );
}

export function FadeControls({
  enabled,
  fadeInDuration,
  fadeOutDuration,
  onEnabledChange,
  onFadeInDurationChange,
  onFadeOutDurationChange,
  showHint = false,
  className,
}: FadeControlsProps) {
  return (
    <div className={cn("space-y-4", className)}>
      <div className="flex items-center justify-between gap-3">
        <span className="text-sm font-medium text-foreground">音量渐入渐出</span>
        <button
          type="button"
          role="switch"
          aria-checked={enabled}
          aria-label="音量渐入渐出"
          onClick={() => onEnabledChange(!enabled)}
          className={cn(
            "inline-flex items-center gap-1.5 px-2.5 py-1.5 rounded-md text-xs font-semibold transition-colors shrink-0",
            enabled
              ? "bg-emerald-500/15 text-emerald-700 dark:text-emerald-300 border border-emerald-500/30"
              : "bg-emerald-500/15 text-emerald-700 dark:text-emerald-300 border border-emerald-500/30 opacity-50",
          )}
        >
          <Activity className="w-3.5 h-3.5" strokeWidth={2.5} />
          <span>{enabled ? "启用" : "已禁用"}</span>
        </button>
      </div>

      {enabled && (
        <>
          <DurationControl label="音量渐入" value={fadeInDuration} onChange={onFadeInDurationChange} />
          <DurationControl label="音量渐出" value={fadeOutDuration} onChange={onFadeOutDurationChange} />
          {showHint && (
            <div className="p-2.5 bg-muted/20 rounded-lg">
              <p className="text-xs text-muted-foreground leading-relaxed">💡 渐入将在开始时间前开始播放，渐出将在结束时间后完成。实际播放时段 = 目标音量时段。</p>
            </div>
          )}
        </>
      )}
    </div>
  );
}
