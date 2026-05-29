"use client";

import { useState, useCallback, MouseEvent, ReactNode, forwardRef } from "react";
import { cn } from "@/lib/utils";

interface Ripple {
  x: number;
  y: number;
  size: number;
  key: number;
}

interface RippleButtonProps {
  children: ReactNode;
  className?: string;
  onClick?: (e: MouseEvent<HTMLButtonElement>) => void;
  disabled?: boolean;
  type?: "button" | "submit" | "reset";
}

const RippleButton = forwardRef<HTMLButtonElement, RippleButtonProps>(
  function RippleButton(
    { children, className, onClick, disabled = false, type = "button" },
    ref
  ) {
    const [ripples, setRipples] = useState<Ripple[]>([]);

    const handleClick = useCallback(
      (e: MouseEvent<HTMLButtonElement>) => {
        if (disabled) return;

        const button = e.currentTarget;
        const rect = button.getBoundingClientRect();
        const size = Math.max(rect.width, rect.height) * 2;
        const x = e.clientX - rect.left - size / 2;
        const y = e.clientY - rect.top - size / 2;

        const newRipple = { x, y, size, key: Date.now() + Math.random() };
        setRipples((prev) => [...prev, newRipple]);

        // Remove ripple after animation
        setTimeout(() => {
          setRipples((prev) => prev.filter((r) => r.key !== newRipple.key));
        }, 600);

        if (onClick) {
          onClick(e);
        }
      },
      [disabled, onClick]
    );

    return (
      <button
        ref={ref}
        type={type}
        onClick={handleClick}
        disabled={disabled}
        className={cn(
          "relative overflow-hidden cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed",
          className
        )}
      >
        {children}
        {ripples.map((ripple) => (
          <span
            key={ripple.key}
            style={{
              position: "absolute",
              borderRadius: "50%",
              backgroundColor: "rgba(255, 255, 255, 0.6)",
              width: `${ripple.size}px`,
              height: `${ripple.size}px`,
              left: `${ripple.x}px`,
              top: `${ripple.y}px`,
              transform: "scale(0)",
              animation: "ripple 0.6s linear forwards",
              pointerEvents: "none",
            }}
          />
        ))}
      </button>
    );
  }
);

export default RippleButton;
