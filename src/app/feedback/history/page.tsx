'use client';

import { useCallback, useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/lib/auth-context';
import {
  FeedbackSummary,
  feedbackStatusLabel,
  feedbackTypeLabel,
  formatFeedbackTime,
} from '@/lib/feedback';

export default function FeedbackHistoryPage() {
  const router = useRouter();
  const { user, loading: authLoading } = useAuth();
  const [records, setRecords] = useState<FeedbackSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadRecords = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const response = await fetch('/api/feedback', { cache: 'no-store' });
      if (response.status === 401) {
        router.replace('/auth/login');
        return;
      }
      const data = await response.json();
      if (!response.ok || !data.success) throw new Error(data.message || '反馈记录加载失败');
      setRecords(Array.isArray(data.feedbacks) ? data.feedbacks : []);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : '反馈记录加载失败');
    } finally {
      setLoading(false);
    }
  }, [router]);

  useEffect(() => {
    if (authLoading) return;
    if (!user) {
      router.replace('/auth/login');
      return;
    }
    void loadRecords();
  }, [authLoading, loadRecords, router, user]);

  return (
    <main className="min-h-screen bg-transparent flex items-center justify-center px-4 py-8">
      <link rel="stylesheet" href="/css/feedback-scys.css" />
      <link rel="stylesheet" href="/css/feedback-records-source.css" />
      <section className="vc-records-modal" aria-label="反馈记录">
        <header className="modal-header">
          <button type="button" onClick={() => router.back()} aria-label="返回">‹</button>
          <h1 className="title">反馈记录</h1>
          <span className="header-spacer" />
        </header>
        <div className="container">
          {loading ? (
            <div className="feedback-record-state">加载中...</div>
          ) : error ? (
            <div className="feedback-record-state">
              <span>{error}</span>
              <button type="button" onClick={() => void loadRecords()}>重新加载</button>
            </div>
          ) : records.length === 0 ? (
            <div className="feedback-record-state">
              <span>暂无反馈记录</span>
              <button type="button" onClick={() => router.push('/feedback')}>去反馈</button>
            </div>
          ) : (
            records.map((record) => (
              <button
                type="button"
                className="form-item"
                key={record.id}
                onClick={() => router.push(`/feedback/${encodeURIComponent(record.id)}`)}
              >
                <span className="state"><span>{feedbackStatusLabel(record.status)}</span></span>
                <span className="record-copy">
                  <strong>{record.category || feedbackTypeLabel(record.type)}</strong>
                  <span>{formatFeedbackTime(record.created_at)}</span>
                </span>
                <span className="record-arrow" aria-hidden="true">›</span>
              </button>
            ))
          )}
        </div>
      </section>
    </main>
  );
}
