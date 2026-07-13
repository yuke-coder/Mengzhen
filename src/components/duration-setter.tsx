"use client";

import React, { useCallback, useId, useState } from "react";
import { Clock } from "lucide-react";
import { NumberStepperButton } from "@/components/number-stepper";
import { cn } from "@/lib/utils";

type DurationUnit = "min" | "hour";

function clampMinutes(value: number, min: number, max: number) {
  const finiteValue = Number.isFinite(value) ? value : min;
  return Math.min(max, Math.max(min, Math.round(finiteValue)));
}

function hoursFromMinutes(minutes: number) {
  return Number((minutes / 60).toFixed(2));
}

function formatDuration(minutes: number) {
  if (minutes < 60) return `${minutes}分钟`;
  const hours = Math.floor(minutes / 60);
  const remainingMinutes = minutes % 60;
  return `${hours}小时${remainingMinutes ? `${remainingMinutes}分钟` : ""}`;
}

export function DurationSetter({
  value,
  onChange,
  min = 0,
  max = 300,
}: {
  value: number;
  onChange: (minutes: number) => void;
  min?: number;
  max?: number;
}) {
  const labelId = useId();
  const [unit, setUnit] = useState<DurationUnit>("min");
  const [tempValue, setTempValue] = useState<string | null>(null);
  const clamped = clampMinutes(value, min, max);
  const range = max - min;
  const percentage = range > 0 ? ((clamped - min) / range) * 100 : 0;
  const isHours = unit === "hour";
  const step = isHours ? 60 : 1;
  const unitText = isHours ? "时" : "分";
  const unitName = isHours ? "小时" : "分钟";
  const inputValue = tempValue ?? String(isHours ? hoursFromMinutes(clamped) : clamped);
  const inputMin = isHours ? hoursFromMinutes(min) : min;
  const inputMax = isHours ? hoursFromMinutes(max) : max;

  const handleNumberChange = useCallback((event: React.ChangeEvent<HTMLInputElement>) => {
    const rawValue = event.target.value;
    setTempValue(rawValue);
    if (rawValue === "" || rawValue === "-") return;

    const parsedValue = Number(rawValue);
    if (!Number.isFinite(parsedValue)) return;
    const minutes = unit === "hour" ? parsedValue * 60 : parsedValue;
    onChange(clampMinutes(minutes, min, max));
  }, [max, min, onChange, unit]);

  const toggleUnit = useCallback(() => {
    setTempValue(null);
    setUnit(current => current === "min" ? "hour" : "min");
  }, []);

  return (
    <div role="group" aria-labelledby={labelId} className="min-w-0 space-y-2">
      <div className="flex min-w-0 items-center justify-between gap-3">
        <label id={labelId} className="flex min-w-0 items-center gap-1.5 text-xs font-medium text-muted-foreground">
          <Clock className="h-3.5 w-3.5 shrink-0" />
          播放时长
        </label>
        <output className="shrink-0 whitespace-nowrap text-right font-mono text-sm font-semibold tabular-nums text-foreground">
          {formatDuration(clamped)}
        </output>
      </div>

      <div className="flex min-w-0 items-center gap-2.5 md:gap-3">
        <div className="min-w-0 flex-1 py-1 md:py-0.5">
          <input
            type="range"
            aria-label="播放时长滑杆（分钟）"
            min={min}
            max={max}
            value={clamped}
            step={1}
            onInput={event => onChange(Number.parseInt(event.currentTarget.value, 10))}
            className={cn(
              "h-2 w-full cursor-pointer appearance-none rounded-full bg-border/30 md:h-2.5",
              "[&::-webkit-slider-thumb]:h-5 [&::-webkit-slider-thumb]:w-5 [&::-webkit-slider-thumb]:cursor-pointer [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:border-2 [&::-webkit-slider-thumb]:border-[var(--brand-start)] [&::-webkit-slider-thumb]:bg-white [&::-webkit-slider-thumb]:shadow-md [&::-webkit-slider-thumb]:transition-transform [&::-webkit-slider-thumb]:active:scale-95",
            )}
            style={{
              background: `linear-gradient(to right,var(--brand-start) ${percentage}%,rgba(128,128,128,0.2) ${percentage}%)`,
            }}
          />
        </div>

        <div className="flex shrink-0 items-center gap-0 md:gap-1.5">
          <NumberStepperButton
            dir={-1}
            ariaLabel={`减少播放时长（每次${isHours ? "1小时" : "1分钟"}）`}
            disabled={clamped <= min}
            value={clamped}
            onChange={onChange}
            min={min}
            max={max}
            step={step}
            className="h-6 w-6 border-border/50 text-muted-foreground hover:bg-[var(--brand-start)]/8 hover:text-[var(--brand-start)] md:h-7 md:w-7"
            iconSize="h-2.5 w-2.5 md:h-3 md:w-3"
          />

          <div className="flex h-6 w-[4.5rem] min-w-0 items-center overflow-hidden border-y border-border/50 bg-muted/20 transition-all focus-within:border-[var(--brand-start)]/60 focus-within:ring-1 focus-within:ring-[var(--brand-start)]/30 md:h-7 md:w-24 md:rounded-lg md:border">
            <input
              type="number"
              aria-label={`播放时长（${unitName}）`}
              min={inputMin}
              max={inputMax}
              step={isHours ? 0.01 : 1}
              value={inputValue}
              onChange={handleNumberChange}
              onBlur={() => setTempValue(null)}
              className="h-full w-0 min-w-0 flex-1 appearance:textfield bg-transparent px-1 text-center font-mono text-[13px] font-medium tabular-nums focus:bg-[var(--brand-start)]/5 focus:outline-none [&::-webkit-inner-spin-button]:appearance-none [&::-webkit-outer-spin-button]:appearance-none md:px-2 md:text-sm"
            />
            <button
              type="button"
              aria-label={`切换为${isHours ? "分钟" : "小时"}`}
              onClick={toggleUnit}
              className="h-full min-w-6 shrink-0 cursor-pointer select-none pr-1.5 text-[9px] font-medium leading-none text-muted-foreground/70 transition-colors hover:text-[var(--brand-start)] md:pr-2 md:text-[10px]"
            >
              {unitText}
            </button>
          </div>

          <NumberStepperButton
            dir={1}
            ariaLabel={`增加播放时长（每次${isHours ? "1小时" : "1分钟"}）`}
            disabled={clamped >= max}
            value={clamped}
            onChange={onChange}
            min={min}
            max={max}
            step={step}
            className="h-6 w-6 border-border/50 text-muted-foreground hover:bg-[var(--brand-start)]/8 hover:text-[var(--brand-start)] md:h-7 md:w-7"
            iconSize="h-2.5 w-2.5 md:h-3 md:w-3"
          />
        </div>
      </div>
    </div>
  );
}
