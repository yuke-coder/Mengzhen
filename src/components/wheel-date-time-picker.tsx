"use client";

import React, { useEffect, useCallback, useRef, useMemo } from "react";
import { cn } from "@/lib/utils";
import { useIsMobile } from "@/hooks/use-mobile";
import { useTheme } from "@/lib/theme-context";

export interface DateTimeValue {
  year: number;
  month: number;
  day: number;
  hour: number;
  minute: number;
  second: number;
}

export interface WheelDateTimePickerProps {
  value: DateTimeValue;
  onChange: (value: DateTimeValue) => void;
  label: string;
}

const ITEM_HEIGHT = 40;
const VISIBLE_COUNT = 5;
const centerOffset = (VISIBLE_COUNT * ITEM_HEIGHT) / 2 - ITEM_HEIGHT / 2;

interface WheelPickerColumnProps {
  items: { value: number; label: string }[];
  value: number;
  onChange: (value: number) => void;
  label: string;
  isDark?: boolean;
}

const formatDateTime = (v: DateTimeValue) =>
  `${v.year}-${String(v.month).padStart(2, "0")}-${String(v.day).padStart(2, "0")} ${String(v.hour).padStart(2, "0")}:${String(v.minute).padStart(2, "0")}:${String(v.second).padStart(2, "0")}`;
const daysInMonth = (year: number, month: number) => new Date(year, month, 0).getDate();
const makeOptions = (count: number, start = 0, pad = 2) =>
  Array.from({ length: count }, (_, i) => ({ value: start + i, label: String(start + i).padStart(pad, "0") }));
const clampIndex = (index: number, length: number) => Math.max(0, Math.min(length - 1, index));

function WheelPickerColumn({ items, value, onChange, label, isDark = false }: WheelPickerColumnProps) {
  const scrollerRef = useRef<HTMLDivElement>(null);
  const commitTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const selectedIndex = items.findIndex(item => item.value === value);
  const currentIndex = selectedIndex >= 0 ? selectedIndex : Math.floor(items.length / 2);

  useEffect(() => {
    const el = scrollerRef.current;
    if (el && Math.round(el.scrollTop / ITEM_HEIGHT) !== currentIndex) {
      el.scrollTop = currentIndex * ITEM_HEIGHT;
    }
  }, [currentIndex]);

  const commitIndex = useCallback((index: number) => {
    const item = items[index];
    if (!item) return;
    const el = scrollerRef.current;
    if (el) el.scrollTop = index * ITEM_HEIGHT;
    if (item.value !== value) onChange(item.value);
  }, [items, onChange, value]);

  const handleScroll = useCallback((e: React.UIEvent<HTMLDivElement>) => {
    const index = clampIndex(Math.round(e.currentTarget.scrollTop / ITEM_HEIGHT), items.length);
    if (commitTimerRef.current) clearTimeout(commitTimerRef.current);
    commitTimerRef.current = setTimeout(() => commitIndex(index), 90);
  }, [commitIndex, items.length]);

  useEffect(() => {
    return () => {
      if (commitTimerRef.current) clearTimeout(commitTimerRef.current);
    };
  }, []);

  return (
    <div className="flex flex-col items-center min-w-0 sm:w-20 sm:min-w-20 flex-1 sm:flex-initial" suppressHydrationWarning>
      <div className={cn("text-xs mb-1", isDark ? "text-zinc-500" : "text-zinc-400")}>{label}</div>
      <div
        className="relative overflow-hidden rounded-lg select-none w-full"
        style={{
          height: ITEM_HEIGHT * VISIBLE_COUNT,
          background: isDark
            ? 'linear-gradient(to bottom, #18181b, #27272a, #18181b)'
            : 'linear-gradient(to bottom, #f4f4f5, #e4e4e7, #f4f4f5)',
        }}
      >
        <div
          className="absolute top-1/2 left-0 right-0 -translate-y-1/2 z-10 pointer-events-none"
          style={{
            height: ITEM_HEIGHT,
            background: isDark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.05)',
            borderTop: isDark ? '1px solid rgba(255,255,255,0.1)' : '1px solid rgba(0,0,0,0.1)',
            borderBottom: isDark ? '1px solid rgba(255,255,255,0.1)' : '1px solid rgba(0,0,0,0.1)',
            borderRadius: 4,
          }}
        />

        <div
          ref={scrollerRef}
          className="h-full overflow-y-auto snap-y snap-mandatory"
          onScroll={handleScroll}
          style={{
            paddingTop: centerOffset,
            paddingBottom: centerOffset,
            scrollbarWidth: "none",
            WebkitOverflowScrolling: "touch",
          }}
          suppressHydrationWarning
        >
          {items.map((item, index) => {
            const isSelected = item.value === value;
            return (
              <div
                key={item.value}
                className={cn(
                  "flex w-full snap-center items-center justify-center",
                  isSelected
                    ? (isDark ? "text-white font-semibold text-lg" : "text-black font-semibold text-lg")
                    : (isDark ? "text-zinc-500 text-base" : "text-zinc-400 text-base")
                )}
                style={{
                  height: ITEM_HEIGHT,
                }}
                onClick={() => commitIndex(index)}
                suppressHydrationWarning
              >
                {item.label}
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}

export const WheelDateTimePicker = React.memo(function WheelDateTimePicker({ value, onChange, label }: WheelDateTimePickerProps) {
  const now = new Date();
  const currentYear = now.getFullYear();
  const currentMonth = now.getMonth() + 1;
  const currentDay = now.getDate();
  const isMobile = useIsMobile();
  const { resolvedTheme } = useTheme();
  const isDark = resolvedTheme === "dark";

  const safeValue = useMemo(() => ({
    year: value?.year ?? currentYear,
    month: value?.month ?? currentMonth,
    day: value?.day ?? currentDay,
    hour: value?.hour ?? 0,
    minute: value?.minute ?? 0,
    second: value?.second ?? 0
  }), [value]);

  const handleChange = (key: keyof DateTimeValue) => (val: number) => {
    const next = { ...safeValue, [key]: val };
    if (key === 'year' || key === 'month') next.day = Math.min(next.day, daysInMonth(next.year, next.month));
    onChange(next);
  };

  const yearOptions = useMemo(() => makeOptions(21, currentYear - 10, 0), [currentYear]);
  const monthOptions = useMemo(() => makeOptions(12, 1), []);
  const dayOptions = useMemo(() => makeOptions(daysInMonth(safeValue.year, safeValue.month), 1), [safeValue.year, safeValue.month]);
  const hourOptions = useMemo(() => makeOptions(24), []);
  const minuteOptions = useMemo(() => makeOptions(60), []);
  const secondOptions = useMemo(() => makeOptions(60), []);

  // 桌面端：年、月、日、时、分、秒
  const desktopColumns = [
    { options: yearOptions, key: 'year' as const, label: '年' },
    { options: monthOptions, key: 'month' as const, label: '月' },
    { options: dayOptions, key: 'day' as const, label: '日' },
    { options: hourOptions, key: 'hour' as const, label: '时' },
    { options: minuteOptions, key: 'minute' as const, label: '分' },
    { options: secondOptions, key: 'second' as const, label: '秒' },
  ];

  const mobileColumns = [
    { options: dayOptions, key: 'day' as const, label: '日' },
    { options: hourOptions, key: 'hour' as const, label: '时' },
    { options: minuteOptions, key: 'minute' as const, label: '分' },
  ];
  const columns = isMobile ? mobileColumns : desktopColumns;
  const displayValue = formatDateTime(safeValue);

  return (
    <div className="w-full">
      <div className={cn("text-sm mb-2 flex justify-between items-center", isDark ? "text-zinc-400" : "text-zinc-500")}>
        <span>{label}</span>
        <span className="font-mono text-xs tabular-nums opacity-70" suppressHydrationWarning>{displayValue}</span>
      </div>
      <div className="flex items-center gap-1">
        {columns.map(col => (
          <WheelPickerColumn
            key={col.key}
            items={col.options}
            value={safeValue[col.key]}
            onChange={handleChange(col.key)}
            label={col.label}
            isDark={isDark}
          />
        ))}
      </div>
    </div>
  );
})
