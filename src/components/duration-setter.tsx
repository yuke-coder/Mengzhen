"use client";

import React, { useState, useRef, useEffect } from "react";
import { cn } from "@/lib/utils";
import { Minus, Plus, Clock } from "lucide-react";
import { useIsMobile } from "@/hooks/use-mobile";

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
  const clamped = Math.min(max, Math.max(min, value));
  const pct = ((clamped - min) / (max - min)) * 100;
  const step = unit === "hour" ? 60 : 1;

  const stateRef = useRef({ clamped, step, min, max });
  useEffect(() => { stateRef.current = { clamped, step, min, max }; }, [clamped, step, min, max]);

  const delayTimerRef = useRef<NodeJS.Timeout | null>(null);
  const repeatTimerRef = useRef<NodeJS.Timeout | null>(null);
  const isLongPressRef = useRef(false);

  const executeStep = (direction: -1 | 1) => {
    const { clamped, step, min, max } = stateRef.current;
    if (direction === -1 && clamped > min) onChange(clamped - step);
    if (direction === 1 && clamped < max) onChange(clamped + step);
  };

  const handlePressStart = (direction: -1 | 1) => {
    isLongPressRef.current = false;
    delayTimerRef.current = setTimeout(() => {
      isLongPressRef.current = true;
      executeStep(direction);
      repeatTimerRef.current = setInterval(() => executeStep(direction), 150);
    }, 300);
  };

  const handlePressEnd = (direction: -1 | 1) => {
    if (delayTimerRef.current) clearTimeout(delayTimerRef.current);
    if (repeatTimerRef.current) clearInterval(repeatTimerRef.current);
    if (!isLongPressRef.current) executeStep(direction);
  };

  useEffect(() => () => {
    if (delayTimerRef.current) clearTimeout(delayTimerRef.current);
    if (repeatTimerRef.current) clearInterval(repeatTimerRef.current);
  }, []);

  const handleNumberChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const raw = e.target.value;
    if (raw === "" || raw === "-") return onChange(min);
    const v = unit === "hour" ? parseFloat(raw) * 60 : parseInt(raw, 10);
    if (!isNaN(v)) onChange(Math.min(max, Math.max(min, Math.round(v))));
  };

  const fmt = (m: number) => m < 60 ? `${m}分钟` : `${Math.floor(m / 60)}小时${m % 60 ? `${m % 60}分钟` : ''}`;
  const btnSize = isMobile ? { width: '24px', height: '24px' } : { width: '28px', height: '28px' };
  const displayVal = unit === "hour" ? clamped / 60 : clamped;
  const inputMax = unit === "hour" ? max / 60 : max;
  const unitText = unit === "min" ? "分" : "时";

  const Btn = ({ dir, disabled }: { dir: -1 | 1; disabled: boolean }) => (
    <button
      onMouseDown={() => !disabled && handlePressStart(dir)}
      onMouseUp={() => handlePressEnd(dir)}
      onMouseLeave={() => handlePressEnd(dir)}
      onTouchStart={() => !disabled && handlePressStart(dir)}
      onTouchEnd={() => handlePressEnd(dir)}
      disabled={disabled}
      style={btnSize}
      className={cn(
        "flex items-center justify-center border transition-all select-none rounded-sm",
        isMobile ? "border-border/50 text-muted-foreground" : "border-border/60",
        disabled ? (isMobile ? "opacity-35 cursor-not-allowed" : "opacity-40 cursor-not-allowed") :
          isMobile ? "hover:bg-[var(--brand-start)]/8 hover:text-[var(--brand-start)] active:scale-95" :
            "hover:bg-[var(--brand-start)]/10 hover:border-[var(--brand-start)]/40 active:scale-95"
      )}>
      {dir === -1 ? <Minus className={isMobile ? "w-2.5 h-2.5" : "w-3 h-3"} /> :
        <Plus className={isMobile ? "w-2.5 h-2.5" : "w-3 h-3"} />}
    </button>
  );

  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between">
        <label className="text-xs font-medium text-muted-foreground flex items-center gap-1.5">
          <Clock className="w-3.5 h-3.5" />播放时长
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
            <Btn dir={-1} disabled={clamped <= min} />
            <div className="flex items-center border-y border-border/50 bg-muted/20 w-[52px] h-6">
              <input type="number" min={0} max={inputMax} step={1} value={displayVal} onChange={handleNumberChange}
                className="flex-1 h-full bg-transparent text-center font-mono font-medium tabular-nums focus:outline-none focus:bg-[var(--brand-start)]/5 appearance:textfield [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none text-[13px] pl-1" />
              <button type="button" onClick={() => setUnit(u => u === "min" ? "hour" : "min")}
                className="shrink-0 text-muted-foreground/70 hover:text-[var(--brand-start)] cursor-pointer transition-colors select-none font-medium leading-none text-[9px] pr-1.5">{unitText}</button>
            </div>
            <Btn dir={1} disabled={clamped >= max} />
          </div>
        ) : (
          <div className="flex items-center gap-1.5">
            <Btn dir={-1} disabled={clamped <= min} />
            <div className="relative w-24 h-7 flex items-center rounded-lg border border-border/60 bg-muted/30 focus-within:border-[var(--brand-start)]/60 focus-within:ring-1 focus-within:ring-[var(--brand-start)]/30 transition-all overflow-hidden">
              <input type="number" min={0} max={inputMax} step={1} value={displayVal} onChange={handleNumberChange}
                className="flex-1 h-full bg-transparent text-center text-sm font-mono font-medium tabular-nums focus:outline-none appearance:textfield [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none px-2" />
              <button type="button" onClick={() => setUnit(u => u === "min" ? "hour" : "min")}
                className="shrink-0 text-muted-foreground/70 hover:text-[var(--brand-start)] cursor-pointer transition-colors select-none font-medium leading-none text-[10px] pr-2">{unitText}</button>
            </div>
            <Btn dir={1} disabled={clamped >= max} />
          </div>
        )}
      </div>
    </div>
  );
}
