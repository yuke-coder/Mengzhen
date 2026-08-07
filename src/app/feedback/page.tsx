'use client';

import { useState, useRef, useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/lib/auth-context';
import { toast } from '@/components/sonner';

// 源码内嵌 base64 资源（来自 scys.com/index.html，保持原貌）
const LOGO_BASE64 =
  'data:image/png;base64,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';

const ADD_IMAGE_ICON =
  'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADwAAAA9BAMAAAADjhfkAAAAG1BMVEUAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACUUeIgAAAACXRSTlMAMx4GLBkTJwvjyWdlAAABXklEQVQ4y9XUzW6CQBQF4ImIdnvsKF1WTdMtDU3cSkgMSzR9AO0T1PYFMHHRx+4VHM4MQ9h0oyfBCB83M8yfuvWMFibL+sEutjREk6h6MMQXNUjBVGVveCQPwWjl8Rb4Nm0XPgM5bzweAXEPh4gcC5LkhKckudYMMHH0yF46bG5NCnLXGD2TraRmAGNyf9c6P+wmeYx1HwcvymEv/+bY5eDX0ZUm1/Oc2Tx3J/QEYF/LB5bCzoSOIJk2C2Lf4gNXwBzAxOUQiAZyxWY1ZQ5vpfLyk12K9ad4SpYLk6p9XcrfvG6KPK/X5RmYSXEpu9LmMTDlXs2rh+QoNZ+8Ai7FkgNZsmnGCjOz6cm65CbQ8fVFMkf7AORtlgKTB9MPi38U8y6tu6zbG5jsr5ZBP6vtxuVIdeSIwhw9pa9Bs3aAzOcz4B17zCsw5aHZlTWPXC/2SIZdWljH/W7Rzl7dd/4A5sBDnhvWq/YAAAAASUVORK5CYII=';

const TYPE_OPTIONS = [
  { value: 'bug', label: 'Bug 缺陷' },
  { value: 'suggestion', label: '产品建议' },
] as const;

// 源码 head 内的远程 CSS 列表（保持源 URL 不变）
const SCYS_CSS_URLS = [
  'https://search01.shengcaiyoushu.com/test/assets/v2/css/index.BNAygxs7.css',
  'https://search01.shengcaiyoushu.com/test/assets/v2/css/SameActivityGuideRule.B-nCCo8n.css',
  'https://search01.shengcaiyoushu.com/test/assets/v2/css/modal.DZIEAW6w.css',
  'https://search01.shengcaiyoushu.com/test/assets/v2/css/form.D1HO4NzO.css',
  'https://search01.shengcaiyoushu.com/test/assets/v2/css/Item.OWqrZGpt.css',
  'https://search01.shengcaiyoushu.com/test/assets/v2/css/Cascader.Cfz6bAEW.css',
  'https://search01.shengcaiyoushu.com/test/assets/v2/css/VideoPlayer.C9jvaI6B.css',
  'https://search01.shengcaiyoushu.com/test/assets/v2/css/Address.Def4QXj5.css',
  'https://search01.shengcaiyoushu.com/test/assets/v2/css/Select2.BJN9_X7w.css',
  'https://search01.shengcaiyoushu.com/test/assets/v2/css/LoadingMore.uHxTqmU_.css',
  'https://search01.shengcaiyoushu.com/test/assets/v2/css/LoadingMore.DfArIBVc.css',
  'https://search01.shengcaiyoushu.com/test/assets/v2/css/index.BhWQ3-Bz.css',
  'https://search01.shengcaiyoushu.com/test/assets/v2/css/NavBack.BN9pNcen.css',
  'https://search01.shengcaiyoushu.com/test/assets/v2/css/index.DqKqZloM.css',
  'https://search01.shengcaiyoushu.com/test/assets/v2/css/index.BtlSPKSA.css',
  'https://search01.shengcaiyoushu.com/test/assets/v2/css/RecordsModal.DiuxHeAY.css',
  'https://search01.shengcaiyoushu.com/test/assets/v2/css/ModalWrapper.D-IKoL1r.css',
  'https://search01.shengcaiyoushu.com/test/assets/v2/css/ModalSlideUp.BD1G6Stl.css',
  'https://search01.shengcaiyoushu.com/test/assets/v2/css/ModalWrapper.B-VoCGhI.css',
  'https://search01.shengcaiyoushu.com/test/assets/v2/css/Success.CJ9tFzfV.css',
  'https://search01.shengcaiyoushu.com/test/assets/v2/css/index.BhW22bD7.css',
];

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

  // 注入源码 head 中的远程 CSS（保持源码引用不变）
  useEffect(() => {
    const links: HTMLLinkElement[] = [];
    for (const url of SCYS_CSS_URLS) {
      const link = document.createElement('link');
      link.rel = 'stylesheet';
      link.crossOrigin = 'anonymous';
      link.href = url;
      document.head.appendChild(link);
      links.push(link);
    }
    // 注入源码内嵌 .v-dropdown-container 样式
    const styleEl = document.createElement('style');
    styleEl.textContent =
      '.v-dropdown-trigger{display:inline-block}.v-dropdown-trigger.v-dropdown-trigger--full-width{display:block}.v-dropdown-container{-webkit-font-smoothing:subpixel-antialiased;-webkit-backface-visibility:hidden;-moz-backface-visibility:hidden;backface-visibility:hidden;display:inline-block;margin:0;padding:0;position:absolute;top:0;left:0;border:1px solid #D6D7D7;box-sizing:border-box;background-color:#fff;border-radius:.3rem;overflow:hidden;z-index:3000;will-change:opacity,transform,top,left;-webkit-box-shadow:0 15px 25px rgba(0,0,0,.2);-moz-box-shadow:0 15px 25px rgba(0,0,0,.2);box-shadow:0 15px 25px #0003}.v-dropdown-container.v-dropdown-no-border{border:0;-webkit-box-shadow:0 3px 20px rgba(0,0,0,.2);-moz-box-shadow:0 3px 20px rgba(0,0,0,.2);box-shadow:0 3px 20px #0003}.animate-down-enter-from,.animate-down-leave-to{transform:perspective(1px) translateY(-6px) translateZ(0);opacity:0}.animate-up-enter-from,.animate-up-leave-to{transform:perspective(1px) translateY(6px) translateZ(0);opacity:0}.animate-down-enter-active,.animate-up-enter-active{-webkit-transition:all .3s ease-out;transition:all .3s ease-out}.animate-down-leave-active,.animate-up-leave-active{-webkit-transition:all .2s ease-out;transition:all .2s ease-out}';
    document.head.appendChild(styleEl);
    return () => {
      for (const link of links) link.remove();
      styleEl.remove();
    };
  }, []);

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

  return (
    <>
      <link rel="stylesheet" href="/css/feedback-scys.css" />
      {/* 源码防闪烁主题脚本 */}
      <script
        dangerouslySetInnerHTML={{
          __html: `(function(){var STORAGE_KEY='theme-preference';var saved=null;try{saved=localStorage.getItem(STORAGE_KEY);}catch(e){}var theme=saved||'system';var actualTheme=theme;if(theme==='system'){actualTheme=window.matchMedia('(prefers-color-scheme: dark)').matches?'dark':'light';}document.documentElement.setAttribute('data-theme',actualTheme);})();`,
        }}
      />
      <div id="app" data-v-app="">
        <div data-v-2a33e695="" className="vc-form">
          {/* 顶部导航 */}
          <div data-v-2a33e695="" className="vc-top-nav">
            <div data-v-2a33e695="" className="min-width container title-main">
              <Link data-v-2a33e695="" href="/" className="logo">
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img data-v-2a33e695="" src={LOGO_BASE64} alt="官方logo" />
              </Link>
              <div data-v-2a33e695="" className="v-spacer" />
              <div data-v-2a33e695="" className="me">
                <span data-v-2a33e695="" className="name">
                  {user.nickname || user.username}
                </span>
              </div>
            </div>
          </div>

          {/* PC 容器 */}
          <div data-v-2a33e695="" className="vc-container-pc min-width">
            {/* 事件海报（源码 display:none，保持原样） */}
            <div data-v-2a33e695="" className="event-img" style={{ display: 'none' }}>
              <div data-v-2a33e695="" className="poster">
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img data-v-2a33e695="" className="poster_bg" src="undefined_jpeg" alt="" role="presentation" />
              </div>
              <div data-v-2a33e695="" className="btn">
                <div data-v-2a33e695="" className="pc-event-btn-box no-benefit">
                  <div data-v-2a33e695="" className="btn-2 no-benefit-btn2">
                    <div data-v-2a33e695="" className="btn-text-2">
                      立即报名
                    </div>
                    <svg
                      data-v-2a33e695=""
                      className="arrow-icon"
                      width="36"
                      height="36"
                      viewBox="0 0 36 36"
                      fill="none"
                    >
                      <path
                        data-v-2a33e695=""
                        d="M18.0002 32.9998C9.71597 32.9998 3.00024 26.284 3.00024 17.9998C3.00024 9.71548 9.71597 2.99976 18.0002 2.99976C26.2845 2.99976 33.0002 9.71548 33.0002 17.9998C33.0002 26.284 26.2845 32.9998 18.0002 32.9998Z"
                        stroke="white"
                        strokeWidth="2"
                        strokeLinejoin="round"
                      />
                      <path
                        data-v-2a33e695=""
                        d="M15.75 24.75L22.5 18L15.75 11.25"
                        stroke="white"
                        strokeWidth="2"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                      />
                    </svg>
                  </div>
                </div>
              </div>
            </div>

            {/* 反馈表单 */}
            <div className="s-form v-no-scrollbar vc-pc">
              <div className="form-list">
                {/* 1、反馈类型 */}
                <div className="input-box sform-select select2" id="field_1c603954fefa2c8f">
                  <div className="title">
                    1、请选择你要反馈的类型 <span className="required">*</span>
                    <span className="select">（最多选1项）</span>
                  </div>
                  <div className="content">
                    {TYPE_OPTIONS.map((option) => {
                      const selected = type === option.value;
                      return (
                        <div
                          key={option.value}
                          className={`option border ${selected ? 'selected' : 'false'}`}
                          onClick={() => setType(option.value)}
                        >
                          <div className="row">
                            <div className={`radio ${selected ? 'selected' : ''}`}>
                              {selected && <div className="circle" />}
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
                      );
                    })}
                  </div>
                  {!type && <div className="hint">请至少选择一项</div>}
                </div>

                {/* 2、反馈内容 */}
                <div data-v-50f01947="" className="input-box text-pic" id="field_4d19dc7f742301db">
                  <div className="title">
                    2、反馈内容 <span className="required">*</span>
                  </div>
                  <div className="desc highlightDescInput">
                    请提供详细使用场景描述+必要截图，帮助我们准确理解需求、为你提供支持~
                  </div>
                  <div className="content">
                    <div data-v-50f01947="" className="border">
                      <textarea
                        data-v-50f01947=""
                        ref={contentRef}
                        value={content}
                        onChange={handleContentChange}
                        onPaste={handlePaste}
                        placeholder="请填写..."
                        rows={1}
                        cols={20}
                        wrap="hard"
                        className="textarea"
                        style={
                          {
                            resize: 'none !important',
                            padding: '5px',
                            overflow: 'hidden !important',
                            height: '18px',
                          } as unknown as React.CSSProperties
                        }
                      />
                      <div data-v-52d60cf3="" className="images">
                        {images.length === 0 ? (
                          <>
                            <div
                              data-v-52d60cf3=""
                              className="img-box placeholder"
                              onClick={() => fileInputRef.current?.click()}
                            >
                              <div data-v-52d60cf3="" className="img">
                                {/* eslint-disable-next-line @next/next/no-img-element */}
                                <img
                                  data-v-52d60cf3=""
                                  src={ADD_IMAGE_ICON}
                                  alt="添加图片"
                                />
                              </div>
                            </div>
                            <div
                              data-v-52d60cf3=""
                              className="text_paste"
                              onClick={() => fileInputRef.current?.click()}
                            >
                              点击此区域，然后按 Ctrl+V 或 Cmd+V可粘贴图片
                            </div>
                          </>
                        ) : (
                          <>
                            {images.map((src, index) => (
                              <div
                                key={index}
                                data-v-52d60cf3=""
                                className="img-box"
                                style={{
                                  position: 'relative',
                                  width: '60px',
                                  height: '60px',
                                  borderRadius: '4px',
                                  overflow: 'hidden',
                                  display: 'inline-block',
                                  marginRight: '8px',
                                }}
                              >
                                {/* eslint-disable-next-line @next/next/no-img-element */}
                                <img
                                  src={src}
                                  alt=""
                                  style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                                />
                                <button
                                  onClick={() => removeImage(index)}
                                  style={{
                                    position: 'absolute',
                                    top: '0',
                                    right: '0',
                                    width: '20px',
                                    height: '20px',
                                    borderRadius: '50%',
                                    background: 'rgba(0,0,0,0.5)',
                                    color: 'white',
                                    border: 'none',
                                    cursor: 'pointer',
                                    fontSize: '12px',
                                    lineHeight: 1,
                                  }}
                                >
                                  ×
                                </button>
                              </div>
                            ))}
                            <div
                              data-v-52d60cf3=""
                              className="img-box placeholder"
                              onClick={() => fileInputRef.current?.click()}
                            >
                              <div data-v-52d60cf3="" className="img">
                                {/* eslint-disable-next-line @next/next/no-img-element */}
                                <img
                                  data-v-52d60cf3=""
                                  src={ADD_IMAGE_ICON}
                                  alt="添加图片"
                                />
                              </div>
                            </div>
                          </>
                        )}
                        <input
                          data-v-52d60cf3=""
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

                {/* 3、联系方式 */}
                <div className="input-box text" id="field_70f416ead95f200c">
                  <div className="title">
                    3、可以留下你的微信联系方式，方便我们跟你沟通
                  </div>
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
                      style={
                        {
                          resize: 'none !important',
                          padding: '5px',
                          overflow: 'hidden !important',
                          height: '42px',
                        } as unknown as React.CSSProperties
                      }
                    />
                  </div>
                </div>
              </div>

              {/* 隐藏的微信订阅按钮（源码 display:none，保持原样） */}
              <div
                className="submit-wrapper"
                style={{ display: 'none' }}
                dangerouslySetInnerHTML={{
                  __html: `<wx-open-subscribe id="subscribeBtn"><script type="text/wxtag-template"><button id="subscribe-btn" style="display: flex; justify-content: center; position: relative; align-items: center; background-color: rgb(54, 165, 144); border-width: medium; border-style: none; border-color: currentcolor; border-image: initial; padding: 0px; color: white; line-height: 18px; font-size: 16px; font-weight: 500;">提交表单</button></script></wx-open-subscribe>`,
                }}
              />

              {/* 实际提交按钮 */}
              <div className="submit-wrapper v-column-start">
                <div className="submit" onClick={handleSubmit}>
                  {submitting ? '提交中...' : '提交表单'}
                </div>
              </div>
            </div>

            {/* 成功页（源码 display:none，由 Vue 控制显示；React 通过 success 状态控制） */}
            <div
              data-v-1306764f=""
              data-v-2a33e695=""
              className="form-success vc-pc"
              style={success ? undefined : { display: 'none' }}
            >
              {success && (
                <div className="bg-transparent rounded-xl border border-white/20 p-8 max-w-sm w-full text-center mx-auto mt-20">
                  <div className="w-12 h-12 mx-auto rounded-full border border-white/20 bg-transparent flex items-center justify-center mb-4">
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" className="text-[#36a590]">
                      <path
                        d="M20 6L9 17l-5-5"
                        stroke="currentColor"
                        strokeWidth="2"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                      />
                    </svg>
                  </div>
                  <h2 className="text-lg font-semibold text-slate-100 drop-shadow">提交成功</h2>
                  <p className="mt-2 text-sm text-slate-300 drop-shadow">
                    感谢你的反馈，我们会尽快处理。
                  </p>
                  <button
                    onClick={() => router.push('/')}
                    className="mt-6 w-full py-2.5 rounded-lg border border-white/30 bg-transparent text-white text-sm font-medium hover:bg-white/10 transition-colors"
                  >
                    返回首页
                  </button>
                </div>
              )}
            </div>
            <div
              data-v-1306764f=""
              data-v-2a33e695=""
              className="form-success vc-pc event-poster"
              style={{ display: 'none' }}
            />

            {/* 右侧悬浮已填记录按钮 */}
            <nav data-v-2a33e695="">
              <div
                data-v-2a33e695=""
                className="vc-records-btn"
                onClick={() => router.push('/feedback/history')}
              >
                <svg
                  width="16"
                  height="16"
                  viewBox="0 0 16 16"
                  fill="none"
                  data-v-2a33e695=""
                >
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
                  <path
                    d="M5.33301 5.33331H9.99967"
                    stroke="#1E2328"
                    strokeWidth="1.33333"
                    strokeLinecap="round"
                    data-v-2a33e695=""
                  />
                  <path
                    d="M7.66699 14.6667L13.3337 7.66669"
                    stroke="#1E2328"
                    strokeWidth="1.33333"
                    strokeLinecap="round"
                    data-v-2a33e695=""
                  />
                  <path
                    d="M5.33301 8H7.99967"
                    stroke="#1E2328"
                    strokeWidth="1.33333"
                    strokeLinecap="round"
                    data-v-2a33e695=""
                  />
                </svg>
                <span data-v-2a33e695="">已填记录</span>
              </div>
            </nav>
          </div>
        </div>
      </div>
    </>
  );
}
