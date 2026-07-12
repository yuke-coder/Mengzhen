'use client';

import { useState, useRef, useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/lib/auth-context';
import { toast } from '@/components/sonner';

const TYPE_OPTIONS = [
  { value: 'bug', label: 'Bug 缺陷' },
  { value: 'suggestion', label: '产品建议' },
] as const;

export default function FeedbackPage() {
  const { user, loading } = useAuth();
  const router = useRouter();

  const [type, setType] = useState<string>('');
  const [content, setContent] = useState('');
  const [contact, setContact] = useState('');
  const [images, setImages] = useState<string[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [success, setSuccess] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const contentRef = useRef<HTMLTextAreaElement>(null);
  const contactRef = useRef<HTMLTextAreaElement>(null);

  const autoResize = (el: HTMLTextAreaElement | null) => {
    if (!el) return;
    el.style.height = 'auto';
    el.style.height = `${el.scrollHeight}px`;
  };

  useEffect(() => {
    autoResize(contentRef.current);
  }, [content]);

  useEffect(() => {
    autoResize(contactRef.current);
  }, [contact]);

  const handleContentChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setContent(e.target.value);
  };

  const handleContactChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setContact(e.target.value);
  };

  const addImage = (file: File): Promise<void> => {
    return new Promise((resolve) => {
      const reader = new FileReader();
      reader.onload = () => {
        const result = reader.result as string;
        setImages((prev) => [...prev, result]);
        resolve();
      };
      reader.readAsDataURL(file);
    });
  };

  const handlePaste = async (e: React.ClipboardEvent) => {
    const items = e.clipboardData.items;
    for (const item of Array.from(items)) {
      if (item.type.startsWith('image/')) {
        const file = item.getAsFile();
        if (file) await addImage(file);
      }
    }
  };

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files) return;
    for (const file of Array.from(files)) {
      await addImage(file);
    }
    e.target.value = '';
  };

  const removeImage = (index: number) => {
    setImages((prev) => prev.filter((_, i) => i !== index));
  };

  const handleSubmit = async () => {
    if (!type) {
      toast.error('请至少选择一项');
      return;
    }
    if (!content.trim()) {
      toast.error('请填写反馈内容');
      return;
    }

    setSubmitting(true);
    try {
      const res = await fetch('/api/feedback', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          type,
          content: content.trim(),
          contact: contact.trim() || null,
          images: images.length > 0 ? images : null,
        }),
      });

      const data = await res.json();
      if (data.success) {
        setSuccess(true);
      } else {
        toast.error(data.message || '提交失败');
      }
    } catch {
      toast.error('网络异常，请稍后重试');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-transparent flex items-center justify-center">
        <div className="text-sm text-slate-200 drop-shadow">加载中...</div>
      </div>
    );
  }

  if (!user) {
    return (
      <div className="min-h-screen bg-transparent flex flex-col items-center justify-center px-4">
        <div className="text-base font-medium text-slate-200 drop-shadow">请先登录后再提交反馈</div>
        <Link
          href="/auth/login"
          className="mt-4 px-5 py-2 rounded-lg border border-white/30 bg-transparent text-white text-sm font-medium hover:bg-white/10 transition-colors"
        >
          去登录
        </Link>
      </div>
    );
  }

  if (success) {
    return (
      <div className="min-h-screen bg-transparent flex flex-col items-center justify-center px-4">
        <div className="bg-transparent rounded-xl border border-white/20 p-8 max-w-sm w-full text-center">
          <div className="w-12 h-12 mx-auto rounded-full border border-white/20 bg-transparent flex items-center justify-center mb-4">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" className="text-[#36a590]">
              <path d="M20 6L9 17l-5-5" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
          </div>
          <h2 className="text-lg font-semibold text-slate-100 drop-shadow">提交成功</h2>
          <p className="mt-2 text-sm text-slate-300 drop-shadow">感谢你的反馈，我们会尽快处理。</p>
          <button
            onClick={() => router.push('/')}
            className="mt-6 w-full py-2.5 rounded-lg border border-white/30 bg-transparent text-white text-sm font-medium hover:bg-white/10 transition-colors"
          >
            返回首页
          </button>
        </div>
      </div>
    );
  }

  return (
    <>
      <link rel="stylesheet" href="/css/feedback-scys.css" />
      <div data-v-2a33e695="" className="vc-form">
        <div data-v-2a33e695="" className="vc-top-nav">
          <div data-v-2a33e695="" className="min-width container title-main">
            <Link data-v-2a33e695="" href="/" className="logo">
              <span data-v-2a33e695="" className="font-bold text-lg text-[#36a590]">
                梦枕
              </span>
            </Link>
            <div data-v-2a33e695="" className="me">
              <span data-v-2a33e695="" className="name">
                {user.nickname || user.username}
              </span>
            </div>
          </div>
        </div>

        <div data-v-2a33e695="" className="vc-container-pc min-width">
          <div data-v-2a33e695="" className="s-form v-no-scrollbar vc-pc">
            <div className="form-list">
              <div className="input-box sform-select select2" id="field_type">
                <div className="title">
                  1、请选择你要反馈的类型
                  <span className="required">*</span>
                  <span className="select">（最多选1项）</span>
                </div>
                <div className="content">
                  {TYPE_OPTIONS.map((option) => (
                    <div
                      key={option.value}
                      className={`option border ${type === option.value ? 'selected' : ''}`}
                      onClick={() => setType(option.value)}
                    >
                      <div className="row">
                        <div className={`radio ${type === option.value ? 'selected' : ''}`}>
                          <div className="circle" />
                        </div>
                        <div className="option-text">
                          <div>
                            <span>{option.label}</span>
                          </div>
                        </div>
                        <div className="commodity-value">
                          <span />
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
                {!type && <div className="hint">请至少选择一项</div>}
              </div>

              <div className="input-box text-pic" id="field_content">
                <div className="title">
                  2、反馈内容
                  <span className="required">*</span>
                </div>
                <div className="desc highlightDescInput">
                  请提供详细使用场景描述+必要截图，帮助我们准确理解需求、为你提供支持~
                </div>
                <div className="content">
                  <div className="border">
                    <textarea
                      ref={contentRef}
                      value={content}
                      onChange={handleContentChange}
                      onPaste={handlePaste}
                      placeholder="请填写..."
                      rows={1}
                      cols={20}
                      wrap="hard"
                      className="textarea"
                      style={{ resize: 'none', overflow: 'hidden' }}
                    />
                    <div className="images">
                      {images.length === 0 ? (
                        <>
                          <div
                            className="img-box placeholder"
                            onClick={() => fileInputRef.current?.click()}
                          >
                            <div className="img">
                              <img
                                src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADwAAAA9BAMAAAADjhfkAAAAG1BMVEUAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACUUeIgAAAACXRSTlMAMx4GLBkTJwvjyWdlAAABXklEQVQ4y9XUzW6CQBQF4ImIdnvsKF1WTdMtDU3cSkgMSzR9AO0T1PYFMHHRx+4VHM4MQ9h0oyfBCB83M8yfuvWMFibL+sEutjREk6h6MMQXNUjBVGVveCQPwWjl8Rb4Nm0XPgM5bzweAXEPh4gcC5LkhKckudYMMHH0yF46bG5NCnLXGD2TraRmAGNyf9c6P+wmeYx1HwcvymEv/+bY5eDX0ZUm1/Oc2Tx3J/QEYF/LB5bCzoSOIJk2C2Lf4gNXwBzAxOUQiAZyxWY1ZQ5vpfLyk12K9ad4SpYLk6p9XcrfvG6KPK/X5RmYSXEpu9LmMTDlXs2rh+QoNZ+8Ai7FkgNZsmnGCjOz6cm65CbQ8fVFMkf7AORtlgKTB9MPi38U8y6tu6zbG5jsr5ZBP6vtxuVIdeSIwhw9pa9Bs3aAzOcz4B17zCsw5aHZlTWPXC/2SIZdWljH/W7Rzl7dd/4A5sBDnhvWq/YAAAAASUVORK5CYII="
                                alt="添加图片"
                              />
                            </div>
                          </div>
                          <div className="text_paste" onClick={() => fileInputRef.current?.click()}>
                            点击此区域，然后按 Ctrl+V 或 Cmd+V可粘贴图片
                          </div>
                        </>
                      ) : (
                        <div className="flex flex-wrap gap-3 mt-3">
                          {images.map((src, index) => (
                            <div
                              key={index}
                              className="relative w-[60px] h-[60px] rounded-lg overflow-hidden border border-[#dbdbdb] group"
                            >
                              <img src={src} alt="" className="w-full h-full object-cover" />
                              <button
                                onClick={() => removeImage(index)}
                                className="absolute top-0.5 right-0.5 w-5 h-5 rounded-full bg-black/50 text-white flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity text-xs"
                              >
                                ×
                              </button>
                            </div>
                          ))}
                          <div
                            className="img-box placeholder"
                            onClick={() => fileInputRef.current?.click()}
                          >
                            <div className="img">
                              <img
                                src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADwAAAA9BAMAAAADjhfkAAAAG1BMVEUAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACUUeIgAAAACXRSTlMAMx4GLBkTJwvjyWdlAAABXklEQVQ4y9XUzW6CQBQF4ImIdnvsKF1WTdMtDU3cSkgMSzR9AO0T1PYFMHHRx+4VHM4MQ9h0oyfBCB83M8yfuvWMFibL+sEutjREk6h6MMQXNUjBVGVveCQPwWjl8Rb4Nm0XPgM5bzweAXEPh4gcC5LkhKckudYMMHH0yF46bG5NCnLXGD2TraRmAGNyf9c6P+wmeYx1HwcvymEv/+bY5eDX0ZUm1/Oc2Tx3J/QEYF/LB5bCzoSOIJk2C2Lf4gNXwBzAxOUQiAZyxWY1ZQ5vpfLyk12K9ad4SpYLk6p9XcrfvG6KPK/X5RmYSXEpu9LmMTDlXs2rh+QoNZ+8Ai7FkgNZsmnGCjOz6cm65CbQ8fVFMkf7AORtlgKTB9MPi38U8y6tu6zbG5jsr5ZBP6vtxuVIdeSIwhw9pa9Bs3aAzOcz4B17zCsw5aHZlTWPXC/2SIZdWljH/W7Rzl7dd/4A5sBDnhvWq/YAAAAASUVORK5CYII="
                                alt="添加图片"
                              />
                            </div>
                          </div>
                        </div>
                      )}
                      <input
                        ref={fileInputRef}
                        className="hidden"
                        type="file"
                        multiple
                        accept="image/*"
                        onChange={handleFileChange}
                      />
                    </div>
                  </div>
                </div>
              </div>

              <div className="input-box text" id="field_contact">
                <div className="title">3、可以留下你的微信联系方式，方便我们跟你沟通</div>
                <div className="content">
                  <textarea
                    ref={contactRef}
                    value={contact}
                    onChange={handleContactChange}
                    placeholder="请填写..."
                    rows={1}
                    cols={20}
                    wrap="hard"
                    className="textarea border"
                    style={{ resize: 'none', overflow: 'hidden' }}
                  />
                </div>
              </div>
            </div>

            <div className="submit-wrapper v-column-start">
              <div className="submit" onClick={handleSubmit}>
                {submitting ? '提交中...' : '提交表单'}
              </div>
            </div>
          </div>

          <nav data-v-2a33e695="">
            <div data-v-2a33e695="" className="vc-records-btn" onClick={() => toast.info('已填记录功能开发中')}>
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none" data-v-2a33e695="">
                <path
                  d="M13.3333 11V14C13.3333 14.3682 13.0349 14.6667 12.6667 14.6667H10.5"
                  stroke="#1E2328"
                  strokeWidth="1.33333"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  data-v-2a33e695=""
                />
                <path
                  d="M13.3337 5.33331V1.99998C13.3337 1.63179 13.0352 1.33331 12.667 1.33331H3.33366C2.96547 1.33331 2.66699 1.63179 2.66699 1.99998V14C2.66699 14.3682 2.96547 14.6666 3.33366 14.6666H5.33366"
                  stroke="#1E2328"
                  strokeWidth="1.33333"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  data-v-2a33e695=""
                />
                <path d="M5.33301 5.33331H9.99967" stroke="#1E2328" strokeWidth="1.33333" strokeLinecap="round" data-v-2a33e695="" />
                <path
                  d="M7.66699 14.6667L13.3337 7.66669"
                  stroke="#1E2328"
                  strokeWidth="1.33333"
                  strokeLinecap="round"
                  data-v-2a33e695=""
                />
                <path d="M5.33301 8H7.99967" stroke="#1E2328" strokeWidth="1.33333" strokeLinecap="round" data-v-2a33e695="" />
              </svg>
              <span data-v-2a33e695="">已填记录</span>
            </div>
          </nav>
        </div>
      </div>
    </>
  );
}
