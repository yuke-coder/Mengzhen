"use client";

import { cn } from "@/lib/utils";
import { useEffect, useRef } from "react";

interface RippleButtonProps {
  children: React.ReactNode;
  className?: string;
  onClick?: () => void;
}

export function RippleButton({ children, className, onClick }: RippleButtonProps) {
  const buttonRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    const button = buttonRef.current;
    if (!button) return;

    const handleClick = (e: MouseEvent) => {
      const rect = button.getBoundingClientRect();
      const size = Math.max(rect.width, rect.height);
      const x = e.clientX - rect.left - size / 2;
      const y = e.clientY - rect.top - size / 2;

      const ripple = document.createElement("span");
      ripple.style.cssText = `
        position: absolute;
        border-radius: 50%;
        background: rgba(255, 255, 255, 0.6);
        width: ${size}px;
        height: ${size}px;
        left: ${x}px;
        top: ${y}px;
        transform: scale(0);
        animation: ripple 0.6s linear;
        pointer-events: none;
      `;

      button.appendChild(ripple);

      setTimeout(() => ripple.remove(), 600);
    };

    button.addEventListener("click", handleClick);
    return () => button.removeEventListener("click", handleClick);
  }, []);

  return (
    <button
      ref={buttonRef}
      onClick={onClick}
      className={cn(
        "relative overflow-hidden transition-all duration-200",
        className
      )}
    >
      {children}
    </button>
  );
}

// 全局 CSS 动画
if (typeof document !== "undefined") {
  const style = document.createElement("style");
  style.textContent = `
    @keyframes ripple {
      to {
        transform: scale(4);
        opacity: 0;
      }
    }
  `;
  if (!document.querySelector('[data-ripple-style]')) {
    style.setAttribute("data-ripple-style", "true");
    document.head.appendChild(style);
  }
}
