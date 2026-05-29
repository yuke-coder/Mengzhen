"use client";

import React, { useCallback, useMemo } from "react";
import { cn } from "@/lib/utils";
import { Minus, Plus, Clock } from "lucide-react";
import { useIsMobile } from "@/hooks/use-mobile";

interface DurationSetterProps {
  value: number;
  onChange: (minutes: number) => void;
  min?: number;
  max?: number;
  step?: number;
}

export function DurationSetter({
  value,
  onChange,
  min = 0,
  max = 240,
}: DurationSetterProps) {
  const isMobile = useIsMobile();
  const buttonStep = 1;

  const clampedValue = useMemo(
    () => Math.min(max, Math.max(min, value)),
    [value, min, max]
  );

  const handleSliderInput = useCallback(
    (e: React.FormEvent<HTMLInputElement>) => {
      const v = parseInt((e.target as HTMLInputElement).value, 10);
      onChange(Math.min(max, Math.max(min, v)));
    },
    [onChange, min, max]
  );

  const handleInputChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const raw = e.target.value;
      if (raw === "" || raw === "-") {
        onChange(min);
        return;
      }
      const v = parseInt(raw, 10);
      if (!isNaN(v)) {
        onChange(Math.min(max, Math.max(min, v)));
      }
    },
    [onChange, min, max]
  );

  const handleDecrement = useCallback(() => {
    const next = Math.max(min, clampedValue - buttonStep);
    onChange(next);
  }, [clampedValue, min, onChange]);

  const handleIncrement = useCallback(() => {
    const next = Math.min(max, clampedValue + buttonStep);
    onChange(next);
  }, [clampedValue, max, onChange]);

  const sliderPercent = ((clampedValue - min) / (max - min)) * 100;

  const formatDisplay = (mins: number) => {
    if (mins < 60) return `${mins}分钟`;
    const h = Math.floor(mins / 60);
    const m = mins % 60;
    return m > 0 ? `${h}小时${m}分钟` : `${h}小时`;
  };

  const inputGroup = (
    <div className="flex items-center gap-1.5">
      <button
        onClick={handleDecrement}
        disabled={clampedValue <= min}
        className={cn(
          "flex-shrink-0 w-8 h-8 rounded-lg border border-border/60 flex items-center justify-center transition-all cursor-pointer",
          clampedValue <= min
            ? "opacity-40 cursor-not-allowed"
            : "hover:bg-[var(--brand-start)]/10 hover:border-[var(--brand-start)]/40 active:scale-95"
        )}
      >
        <Minus className="w-3.5 h-3.5" />
      </button>

      <div className="relative w-20">
        <input
          type="number"
          min={min}
          max={max}
          value={clampedValue}
          onChange={handleInputChange}
          className="w-full h-8 rounded-lg border border-border/60 bg-muted/30 text-center text-sm font-mono font-medium tabular-nums focus:outline-none focus:border-[var(--brand-start)]/60 focus:ring-1 focus:ring-[var(--brand-start)]/30 transition-all [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
        />
        <span className="absolute right-2 top-1/2 -translate-y-1/2 text-[10px] text-muted-foreground pointer-events-none">
          分
        </span>
      </div>

      <button
        onClick={handleIncrement}
        disabled={clampedValue >= max}
        className={cn(
          "flex-shrink-0 w-8 h-8 rounded-lg border border-border/60 flex items-center justify-center transition-all cursor-pointer",
          clampedValue >= max
            ? "opacity-40 cursor-not-allowed"
            : "hover:bg-[var(--brand-start)]/10 hover:border-[var(--brand-start)]/40 active:scale-95"
        )}
      >
        <Plus className="w-3.5 h-3.5" />
      </button>
    </div>
  );

  const sliderElement = (
    <input
      type="range"
      min={min}
      max={max}
      value={clampedValue}
      step={1}
      onInput={handleSliderInput}
      className="w-full h-2.5 rounded-full appearance-none bg-border/30 cursor-pointer [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:w-6 [&::-webkit-slider-thumb]:h-6 [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:bg-white [&::-webkit-slider-thumb]:shadow-md [&::-webkit-slider-thumb]:cursor-pointer [&::-webkit-slider-thumb]:border-2 [&::-webkit-slider-thumb]:border-[var(--brand-start)] [&::-webkit-slider-thumb]:active:scale-95 sm:[&::-webkit-slider-thumb]:w-5 sm:[&::-webkit-slider-thumb]:h-5"
      style={{
        background: `linear-gradient(to right, var(--brand-start) ${sliderPercent}%, rgba(128,128,128,0.2) ${sliderPercent}%)`,
      }}
    />
  );

  if (isMobile) {
    return (
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <label className="text-xs font-medium text-muted-foreground flex items-center gap-1.5">
            <Clock className="w-3.5 h-3.5" />
            播放时长
          </label>
          <span className="text-sm font-mono font-semibold tabular-nums text-foreground">
            {formatDisplay(clampedValue)}
          </span>
        </div>

        <div className="relative pt-1 pb-1">
          {sliderElement}
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={handleDecrement}
            disabled={clampedValue <= min}
            className={cn(
              "flex-shrink-0 w-10 h-10 rounded-lg border border-border/60 flex items-center justify-center transition-all cursor-pointer",
              clampedValue <= min
                ? "opacity-40 cursor-not-allowed"
                : "hover:bg-[var(--brand-start)]/10 hover:border-[var(--brand-start)]/40 active:scale-95 active:bg-[var(--brand-start)]/15"
            )}
          >
            <Minus className="w-4 h-4" />
          </button>

          <div className="flex-1 relative">
            <input
              type="number"
              min={min}
              max={max}
              value={clampedValue}
              onChange={handleInputChange}
              className="w-full h-10 rounded-lg border border-border/60 bg-muted/30 text-center text-sm font-mono font-medium tabular-nums focus:outline-none focus:border-[var(--brand-start)]/60 focus:ring-1 focus:ring-[var(--brand-start)]/30 transition-all [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
            />
            <span className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-muted-foreground pointer-events-none">
              分钟
            </span>
          </div>

          <button
            onClick={handleIncrement}
            disabled={clampedValue >= max}
            className={cn(
              "flex-shrink-0 w-10 h-10 rounded-lg border border-border/60 flex items-center justify-center transition-all cursor-pointer",
              clampedValue >= max
                ? "opacity-40 cursor-not-allowed"
                : "hover:bg-[var(--brand-start)]/10 hover:border-[var(--brand-start)]/40 active:scale-95 active:bg-[var(--brand-start)]/15"
            )}
          >
            <Plus className="w-4 h-4" />
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between">
        <label className="text-xs font-medium text-muted-foreground flex items-center gap-1.5">
          <Clock className="w-3.5 h-3.5" />
          播放时长
        </label>
        <span className="text-sm font-mono font-semibold tabular-nums text-foreground">
          {formatDisplay(clampedValue)}
        </span>
      </div>

      <div className="flex items-center gap-3">
        <div className="flex-1 pt-0.5 pb-0.5">
          {sliderElement}
        </div>
        {inputGroup}
      </div>
    </div>
  );
}
