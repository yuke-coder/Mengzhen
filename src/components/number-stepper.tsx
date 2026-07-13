"use client";

import React, { useCallback, useEffect, useRef } from "react";
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
  ariaLabel,
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
  ariaLabel: string;
  className?: string;
  iconSize?: string;
}) {
  const intervalRef = useRef<NodeJS.Timeout | null>(null);
  const timeoutRef = useRef<NodeJS.Timeout | null>(null);
  const valueRef = useRef(value);
  const activePointerRef = useRef<number | null>(null);
  const didRepeatRef = useRef(false);

  useEffect(() => {
    valueRef.current = value;
  }, [value]);

  const doStep = useCallback(() => {
    const newValue = Math.min(max, Math.max(min, valueRef.current + dir * step));
    valueRef.current = newValue;
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
    didRepeatRef.current = true;
    doStep();
    intervalRef.current = setInterval(doStep, 100);
  }, [doStep]);

  const handlePointerDown = useCallback((event: React.PointerEvent<HTMLButtonElement>) => {
    if (disabled || !event.isPrimary || (event.pointerType === "mouse" && event.button !== 0)) return;
    activePointerRef.current = event.pointerId;
    didRepeatRef.current = false;
    event.currentTarget.setPointerCapture(event.pointerId);
    timeoutRef.current = setTimeout(startRepeating, 250);
  }, [disabled, startRepeating]);

  const finishPointer = useCallback((event: React.PointerEvent<HTMLButtonElement>, cancelled: boolean) => {
    if (activePointerRef.current !== event.pointerId) return;
    activePointerRef.current = null;
    clearTimers();
    if (cancelled) didRepeatRef.current = false;
    if (event.currentTarget.hasPointerCapture(event.pointerId)) {
      event.currentTarget.releasePointerCapture(event.pointerId);
    }
  }, [clearTimers]);

  useEffect(() => {
    return clearTimers;
  }, [clearTimers]);

  const handleContextMenu = useCallback((e: React.MouseEvent) => {
    e.preventDefault();
  }, []);

  const handleClick = useCallback(() => {
    if (didRepeatRef.current) {
      didRepeatRef.current = false;
      return;
    }
    doStep();
  }, [doStep]);

  return (
    <button
      type="button"
      aria-label={ariaLabel}
      onPointerDown={handlePointerDown}
      onPointerUp={event => finishPointer(event, false)}
      onPointerCancel={event => finishPointer(event, true)}
      onLostPointerCapture={clearTimers}
      onClick={handleClick}
      onContextMenu={handleContextMenu}
      disabled={disabled}
      className={cn(
        "flex items-center justify-center border border-border/60 rounded-sm transition-all select-none hover:bg-[var(--brand-start)]/10 hover:border-[var(--brand-start)]/40 active:scale-95 disabled:opacity-40 disabled:cursor-not-allowed",
        className
      )}
      style={{
        WebkitTouchCallout: "none",
        WebkitUserSelect: "none",
        touchAction: "manipulation",
      }}
    >
      {dir === -1 ? (
        <Minus className={iconSize} />
      ) : (
        <Plus className={iconSize} />
      )}
    </button>
  );
}
