"use client";

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
      <div className="py-0.5 flex items-center gap-3">
        <NumberStepperButton
          dir={-1}
          ariaLabel={`减少${label}时长（每次1秒）`}
          disabled={value <= 0}
          value={value}
          onChange={onChange}
          min={0}
          max={120}
          step={1}
          className="w-8 h-8"
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
          className="w-8 h-8"
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
      <div className="flex items-center justify-between p-3 rounded-lg bg-muted/20 border border-border/30">
        <div className="flex items-center gap-2">
          <button
            type="button"
            role="switch"
            aria-checked={enabled}
            onClick={() => onEnabledChange(!enabled)}
            className={cn(
              "relative inline-flex h-5 w-9 flex-shrink-0 rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none",
              enabled ? "bg-[var(--brand-start)]" : "bg-muted",
            )}
          >
            <span
              aria-hidden="true"
              className={cn(
                "pointer-events-none inline-block h-4 w-4 rounded-full bg-white shadow-sm ring-0 transition duration-200 ease-in-out",
                enabled ? "translate-x-4" : "translate-x-0",
              )}
            />
          </button>
          <span className="text-sm font-medium text-foreground">启用音量渐入渐出</span>
        </div>
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
