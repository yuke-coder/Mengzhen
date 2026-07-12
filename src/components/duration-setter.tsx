"use client";

import React, { useState, useEffect, useCallback } from "react";
import { cn } from "@/lib/utils";
import { Clock } from "lucide-react";
import { useIsMobile } from "@/hooks/use-mobile";
import { NumberStepperButton } from "@/components/number-stepper";

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
  const isMobile = useIsMobile();
  const [unit, setUnit] = useState<"min" | "hour">("min");
  const [tempValue, setTempValue] = useState<string | null>(null);
  const clamped = Math.min(max, Math.max(min, value));
  const pct = ((clamped - min) / (max - min)) * 100;
  const step = unit === "hour" ? 60 : 1;

  const displayVal = unit === "hour" ? clamped / 60 : clamped;
  const inputValue = tempValue !== null ? tempValue : String(displayVal);

  const handleNumberChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const raw = e.target.value;
    setTempValue(raw);

    if (raw === "" || raw === "-") return;

    let v = unit === "hour" ? parseFloat(raw) * 60 : parseInt(raw, 10);
    if (!isNaN(v)) {
      v = Math.min(max, Math.max(min, Math.round(v)));
      if (unit === "hour") v = Math.round(v / 60) * 60;
      onChange(v);
    }
  }, [unit, min, max, onChange]);

  const handleBlur = useCallback(() => {
    setTempValue(null);
  }, []);

  const fmt = (m: number) => m < 60 ? `${m}分钟` : `${Math.floor(m / 60)}小时${m % 60 ? `${m % 60}分钟` : ""}`;
  const unitText = unit === "min" ? "分" : "时";

  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between">
        <label className="text-xs font-medium text-muted-foreground flex items-center gap-1.5">
          <Clock className="w-3.5 h-3.5" />
          播放时长
        </label>
        <span className="text-sm font-mono font-semibold tabular-nums text-foreground">{fmt(clamped)}</span>
      </div>
      <div className={cn("flex items-center", isMobile ? "gap-2.5" : "gap-3")}>
        <div className={cn("flex-1", isMobile ? "py-1" : "py-0.5")}>
          <input type="range" min={min} max={max} value={clamped} step={1}
            onInput={(e) => onChange(parseInt(e.currentTarget.value, 10))}
            className={cn(
              "w-full rounded-full appearance-none bg-border/30 cursor-pointer",
              "[&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:w-5 [&::-webkit-slider-thumb]:h-5 [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:bg-white [&::-webkit-slider-thumb]:shadow-md [&::-webkit-slider-thumb]:border-2 [&::-webkit-slider-thumb]:border-[var(--brand-start)] [&::-webkit-slider-thumb]:cursor-pointer [&::-webkit-slider-thumb]:active:scale-95 [&::-webkit-slider-thumb]:transition-transform",
              isMobile ? "h-2" : "h-2.5"
            )}
            style={{ background: `linear-gradient(to right,var(--brand-start) ${pct}%,rgba(128,128,128,0.2) ${pct}%)` }} />
        </div>

        {isMobile ? (
          <div className="flex items-center shrink-0">
            <NumberStepperButton
              dir={-1}
              disabled={clamped <= min}
              value={clamped}
              onChange={onChange}
              min={min}
              max={max}
              step={step}
              className="w-6 h-6 border-border/50 text-muted-foreground hover:bg-[var(--brand-start)]/8 hover:text-[var(--brand-start)]"
              iconSize="w-2.5 h-2.5"
            />
            <div className="flex items-center border-y border-border/50 bg-muted/20 h-6">
              <input type="number" min={unit === "hour" ? min / 60 : min} max={unit === "hour" ? max / 60 : max} step={1} value={inputValue} onChange={handleNumberChange} onBlur={handleBlur}
                className="w-[40px] h-full bg-transparent text-center font-mono font-medium tabular-nums focus:outline-none focus:bg-[var(--brand-start)]/5 appearance:textfield [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none text-[13px] px-1" />
              <button type="button" onClick={() => setUnit(u => u === "min" ? "hour" : "min")}
                className="shrink-0 text-muted-foreground/70 hover:text-[var(--brand-start)] cursor-pointer transition-colors select-none font-medium leading-none text-[9px] pr-1.5">{unitText}</button>
            </div>
            <NumberStepperButton
              dir={1}
              disabled={clamped >= max}
              value={clamped}
              onChange={onChange}
              min={min}
              max={max}
              step={step}
              className="w-6 h-6 border-border/50 text-muted-foreground hover:bg-[var(--brand-start)]/8 hover:text-[var(--brand-start)]"
              iconSize="w-2.5 h-2.5"
            />
          </div>
        ) : (
          <div className="flex items-center gap-1.5">
            <NumberStepperButton
              dir={-1}
              disabled={clamped <= min}
              value={clamped}
              onChange={onChange}
              min={min}
              max={max}
              step={step}
              className="w-7 h-7"
            />
            <div className="relative w-24 h-7 flex items-center rounded-lg border border-border/60 bg-muted/30 focus-within:border-[var(--brand-start)]/60 focus-within:ring-1 focus-within:ring-[var(--brand-start)]/30 transition-all overflow-hidden">
              <input type="number" min={unit === "hour" ? min / 60 : min} max={unit === "hour" ? max / 60 : max} step={1} value={inputValue} onChange={handleNumberChange} onBlur={handleBlur}
                className="flex-1 h-full bg-transparent text-center text-sm font-mono font-medium tabular-nums focus:outline-none appearance:textfield [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none px-2" />
              <button type="button" onClick={() => setUnit(u => u === "min" ? "hour" : "min")}
                className="shrink-0 text-muted-foreground/70 hover:text-[var(--brand-start)] cursor-pointer transition-colors select-none font-medium leading-none text-[10px] pr-2">{unitText}</button>
            </div>
            <NumberStepperButton
              dir={1}
              disabled={clamped >= max}
              value={clamped}
              onChange={onChange}
              min={min}
              max={max}
              step={step}
              className="w-7 h-7"
            />
          </div>
        )}
      </div>
    </div>
  );
}
