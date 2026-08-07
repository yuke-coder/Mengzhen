'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { useAuth } from '@/lib/auth-context';
import { toast } from '@/components/sonner';
import {
  FeedbackReply,
  FeedbackSummary,
  feedbackStatusLabel,
  feedbackTypeLabel,
  formatFeedbackTime,
} from '@/lib/feedback';

interface FeedbackDetailResponse {
  success: boolean;
  message?: string;
  feedback?: FeedbackSummary;
  replies?: FeedbackReply[];
}

export default function FeedbackRecordPage() {
  const router = useRouter();
  const params = useParams<{ id: string }>();
  const { user, loading: authLoading } = useAuth();
  const feedbackId = useMemo(() => decodeURIComponent(params.id || ''), [params.id]);
  const [record, setRecord] = useState<FeedbackSummary | null>(null);
  const [replies, setReplies] = useState<FeedbackReply[]>([]);
  const [reply, setReply] = useState('');
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState('');

  const loadRecord = useCallback(async () => {
    if (!feedbackId) return;
    setLoading(true);
    setError('');
    try {
      const response = await fetch(`/api/feedback?id=${encodeURIComponent(feedbackId)}`, {
        cache: 'no-store',
      });
      if (response.status === 401) {
        router.replace('/auth/login');
        return;
      }
      const data = (await response.json()) as FeedbackDetailResponse;
      if (!response.ok || !data.success || !data.feedback) {
        throw new Error(data.message || '反馈详情加载失败');
      }
      setRecord(data.feedback);
      setReplies(Array.isArray(data.replies) ? data.replies : []);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : '反馈详情加载失败');
    } finally {
      setLoading(false);
    }
  }, [feedbackId, router]);

  useEffect(() => {
    if (authLoading) return;
    if (!user) {
      router.replace('/auth/login');
      return;
    }
    void loadRecord();
  }, [authLoading, loadRecord, router, user]);

  const sendReply = async () => {
    const content = reply.trim();
    if (!content || sending) return;
    setSending(true);
    try {
      const response = await fetch('/api/feedback', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ feedbackId, content }),
      });
      const data = await response.json();
      if (!response.ok || !data.success) throw new Error(data.message || '回复失败');
      setReply('');
      await loadRecord();
    } catch (reason) {
      toast.error(reason instanceof Error ? reason.message : '回复失败');
    } finally {
      setSending(false);
    }
  };

  const messages = record
    ? [{
        id: record.id,
        sender_role: 'user' as const,
        sender: '我',
        content: record.content,
        images: record.images,
        created_at: record.created_at,
      }, ...replies]
    : [];

  return (
    <main className="min-h-screen bg-transparent flex items-center justify-center px-4 py-8">
      <link rel="stylesheet" href="/css/feedback-scys.css" />
      <link rel="stylesheet" href="/css/feedback-records-source.css" />
      <section className="vc-records-modal" aria-label="反馈详情">
        <header className="modal-header">
          <button type="button" onClick={() => router.back()} aria-label="返回">‹</button>
          <h1 className="title">反馈详情</h1>
          <span className="header-spacer" />
        </header>
        <div className="container">
          {loading ? (
            <div className="feedback-record-state">加载中...</div>
          ) : error || !record ? (
            <div className="feedback-record-state">
              <span>{error || '反馈详情加载失败'}</span>
              <button type="button" onClick={() => void loadRecord()}>重新加载</button>
            </div>
          ) : (
            <>
              <dl className="feedback-detail-meta">
                <dt>问题类型</dt><dd>{feedbackTypeLabel(record.type)}</dd>
                <dt>具体场景</dt><dd>{record.category || '—'}</dd>
                <dt>处理状态</dt><dd>{feedbackStatusLabel(record.status)}</dd>
                <dt>提交时间</dt><dd>{formatFeedbackTime(record.created_at)}</dd>
                <dt>处理人员</dt><dd>{record.op_name || record.op_group || '—'}</dd>
                <dt>处理时间</dt><dd>{formatFeedbackTime(record.processed_at)}</dd>
                <dt>联系方式</dt><dd>{record.contact || '—'}</dd>
              </dl>
              {messages.map((message) => (
                <article
                  className={`feedback-message ${message.sender_role === 'support' ? 'support' : ''}`}
                  key={message.id}
                >
                  <div className="feedback-message-head">
                    <strong>{message.sender || (message.sender_role === 'support' ? '客服' : '我')}</strong>
                    <time>{formatFeedbackTime(message.created_at)}</time>
                  </div>
                  <p>{message.content}</p>
                  {message.images && message.images.length > 0 && (
                    <div className="feedback-images">
                      {message.images.map((image, index) => (
                        <a href={image} target="_blank" rel="noreferrer" key={`${message.id}-${index}`}>
                          {/* eslint-disable-next-line @next/next/no-img-element */}
                          <img src={image} alt={`反馈图片 ${index + 1}`} />
                        </a>
                      ))}
                    </div>
                  )}
                </article>
              ))}
            </>
          )}
        </div>
        {record && record.status !== 3 && (
          <div className="feedback-reply">
            <textarea
              value={reply}
              maxLength={200}
              placeholder="补充反馈内容"
              onChange={(event) => setReply(event.target.value)}
            />
            <button
              type="button"
              className="feedback-send"
              disabled={!reply.trim() || sending}
              onClick={() => void sendReply()}
            >
              {sending ? '发送中' : '发送'}
            </button>
          </div>
        )}
      </section>
    </main>
  );
}
