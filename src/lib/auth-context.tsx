'use client';

import React, { createContext, useContext, useState, useEffect, useCallback, useRef } from 'react';

export interface User {
  id: string | number;
  username: string;
  createdAt: string;
  // 用户资料字段
  avatar_url?: string | null;
  nickname?: string | null;
  gender?: 'male' | 'female' | 'secret' | null;
  birthday?: string | null;
  location?: string | null;
  signature?: string | null;
  bio?: string | null;
}

interface AuthContextType {
  user: User | null;
  loading: boolean;
  login: (username: string, password: string) => Promise<{ success: boolean; message: string }>;
  register: (username: string, password: string) => Promise<{ success: boolean; message: string }>;
  loginOrRegister: (username: string, password: string) => Promise<{ success: boolean; message: string }>;
  logout: () => Promise<void>;
  checkAuth: () => Promise<void>;
  updateUser: (userData: Partial<User>) => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const checkAuthAbortRef = useRef<AbortController | null>(null);

  const checkAuth = useCallback(async () => {
    // 取消上一次未完成的请求
    if (checkAuthAbortRef.current) {
      checkAuthAbortRef.current.abort();
    }
    const abortController = new AbortController();
    checkAuthAbortRef.current = abortController;

    try {
      const res = await fetch('/api/auth/me', {
        credentials: 'include',
        cache: 'no-store',
        signal: abortController.signal,
      });
      const data = await res.json();
      if (data.success && data.user) {
        setUser(data.user);
      } else {
        setUser(null);
      }
    } catch (err) {
      // 被取消的请求不更新状态
      if (err instanceof DOMException && err.name === 'AbortError') return;
      setUser(null);
    } finally {
      if (!abortController.signal.aborted) {
        setLoading(false);
      }
    }
  }, []);

  // 组件挂载后检查认证状态
  useEffect(() => {
    checkAuth();
  }, [checkAuth]);

  const submitAuth = async (
    url: string,
    username: string,
    password: string,
    successMessage: string,
    failureMessage: string
  ) => {
    try {
      const res = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ username, password }),
      });
      const data = await res.json();
      if (data.success && data.user) {
        // 立即更新状态
        setUser(data.user);
        setLoading(false);
        return { success: true, message: data.message || successMessage };
      }
      return { success: false, message: data.error || failureMessage };
    } catch {
      return { success: false, message: '网络错误，请重试' };
    }
  };

  const login = (username: string, password: string) =>
    submitAuth('/api/auth/login', username, password, '登录成功', '登录失败');

  const register = (username: string, password: string) =>
    submitAuth('/api/auth/register', username, password, '注册成功', '注册失败');

  const loginOrRegister = (username: string, password: string) =>
    submitAuth('/api/auth/entry', username, password, '登录成功', '登录失败');

  const logout = async () => {
    try {
      await fetch('/api/auth/logout', { method: 'POST' });
    } finally {
      setUser(null);
      setLoading(false);
    }
  };

  const updateUser = (userData: Partial<User>) => {
    setUser(prev => prev ? { ...prev, ...userData } : null);
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, register, loginOrRegister, logout, checkAuth, updateUser }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
