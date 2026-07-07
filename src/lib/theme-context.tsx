"use client";

import { createContext, useContext, useEffect, useState, useCallback } from "react";

export type Theme = "dark" | "light" | "system";
export type ResolvedTheme = "dark" | "light";

interface ThemeContextType {
  theme: Theme;
  resolvedTheme: ResolvedTheme;
  setTheme: (theme: Theme) => void;
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

function getResolvedTheme(theme: Theme): ResolvedTheme {
  if (theme === "system") {
    if (typeof window !== "undefined") {
      return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
    }
    return "dark";
  }
  return theme;
}

function getStoredTheme(): Theme | null {
  if (typeof window === "undefined") return null;
  try {
    const value = localStorage.getItem("theme-mode");
    if (value === "dark" || value === "light") return value;
    if (value === "auto" || value === "system") return "system";
  } catch {}
  return null;
}

function applyResolvedTheme(resolved: ResolvedTheme) {
  const root = document.documentElement;
  root.classList.remove("dark");
  if (resolved === "dark") {
    root.classList.add("dark");
    document.body.setAttribute("theme-mode", "dark");
  } else {
    document.body.removeAttribute("theme-mode");
  }
}

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const [theme, setThemeState] = useState<Theme>(() => getStoredTheme() ?? "system");
  const [resolvedTheme, setResolvedTheme] = useState<ResolvedTheme>(() =>
    getResolvedTheme(getStoredTheme() ?? "system")
  );

  const setTheme = useCallback((newTheme: Theme) => {
    setThemeState(newTheme);
    try {
      localStorage.setItem("theme-mode", newTheme === "system" ? "auto" : newTheme);
      localStorage.removeItem("theme");
    } catch {}
  }, []);

  useEffect(() => {
    const resolved = getResolvedTheme(theme);
    setResolvedTheme(resolved);
    applyResolvedTheme(resolved);
  }, [theme]);

  useEffect(() => {
    if (theme !== "system") return;

    const mq = window.matchMedia("(prefers-color-scheme: dark)");
    const handler = (e: MediaQueryListEvent) => {
      const resolved = e.matches ? "dark" : "light";
      setResolvedTheme(resolved);
      applyResolvedTheme(resolved);
    };

    mq.addEventListener("change", handler);
    return () => mq.removeEventListener("change", handler);
  }, [theme]);

  return (
    <ThemeContext.Provider value={{ theme, resolvedTheme, setTheme }}>
      {children}
    </ThemeContext.Provider>
  );
}

export function useTheme() {
  const context = useContext(ThemeContext);
  if (!context) throw new Error("useTheme must be used within a ThemeProvider");
  return context;
}
