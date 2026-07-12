"use client";

import React, { useState, useCallback, useEffect, useRef } from "react";
import { Minus, Plus } from "lucide-react";
import { cn } from "@/lib/utils";

export function NumberStepperButton({
  dir,
  disabled,
  value,
  onChange,
  min = 0,
  max = 120,
  step = 1,
  className,
  iconSize = "w-3 h-3",
}: {
  dir: -1 | 1;
  disabled: boolean;
  value: number;
  onChange: (v: number) => void;
  min?: number;
  max?: number;
  step?: number;
  className?: string;
  iconSize?: string;
}) {
  const intervalRef = useRef<NodeJS.Timeout | null>(null);
  const timeoutRef = useRef<NodeJS.Timeout | null>(null);
  const valueRef = useRef(value);
  const isRepeatingRef = useRef(false);

  useEffect(() => {
    valueRef.current = value;
  }, [value]);

  const doStep = useCallback(() => {
    const newValue = Math.min(max, Math.max(min, valueRef.current + dir * step));
    onChange(newValue);
  }, [dir, min, max, step, onChange]);

  const clearTimers = useCallback(() => {
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
    }
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current);
      timeoutRef.current = null;
    }
  }, []);

  const startRepeating = useCallback(() => {
    isRepeatingRef.current = true;
    doStep();
    intervalRef.current = setInterval(doStep, 100);
  }, [doStep]);

  const handlePressStart = useCallback((e: React.MouseEvent | React.TouchEvent) => {
    if (disabled) return;
    isRepeatingRef.current = false;
    timeoutRef.current = setTimeout(startRepeating, 250);
  }, [disabled, startRepeating]);

  const handlePressEnd = useCallback((e: React.MouseEvent | React.TouchEvent) => {
    if (disabled) return;
    const wasRepeating = isRepeatingRef.current;
    clearTimers();
    if (!wasRepeating) {
      doStep();
    }
  }, [disabled, doStep, clearTimers]);

  const handleMouseLeave = useCallback(() => {
    clearTimers();
  }, [clearTimers]);

  useEffect(() => clearTimers, [clearTimers]);

  return (
    <button
      type="button"
      onMouseDown={handlePressStart}
      onMouseUp={handlePressEnd}
      onMouseLeave={handleMouseLeave}
      onTouchStart={handlePressStart}
      onTouchEnd={handlePressEnd}
      disabled={disabled}
      className={cn(
        "flex items-center justify-center border border-border/60 rounded-sm transition-all select-none hover:bg-[var(--brand-start)]/10 hover:border-[var(--brand-start)]/40 active:scale-95 disabled:opacity-40 disabled:cursor-not-allowed",
        className
      )}
    >
      {dir === -1 ? (
        <Minus className={iconSize} />
      ) : (
        <Plus className={iconSize} />
      )}
    </button>
  );
}
