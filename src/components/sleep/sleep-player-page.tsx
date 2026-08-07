/**
 * 助眠播放页（1:1 整迁自 QQ音乐 Kuikly bw_main_player）
 *
 * 源码来源：BWMainPlayerPager.java + BWMainPlayerViewModel.java
 * 子组件：HealTabBackgroundView / QMNavigationBar / BWBubbleView /
 *        CountDownTimerView / TextTipView / BWTimeSelectView
 *
 * 组件树对齐 BWMainPlayerPager.a4() build 方法：
 *   ViewContainer(根)
 *   ├─ HealTabBackgroundView   → BackgroundView
 *   ├─ QMNavigationBar          → NavBar
 *   ├─ BWBubbleView             → BubbleView
 *   ├─ View(flexGrow=1)         → 主内容区
 *   │   ├─ View(width=136)      → 倒计时+设置行
 *   │   │   ├─ CountDownTimerView → CountDownTimerView
 *   │   │   └─ View(width=40,row) → 时间设置行
 *   │   │       ├─ TextView(timeTip)
 *   │   │       └─ ImageView(设置按钮)
 *   │   └─ vbind(j0())          → 播放按钮区
 *   │       ├─ vif(v0=loading)  → 加载动画
 *   │       └─ velse            → 播放/暂停按钮
 *   ├─ TextTipView(vif !svip)   → 会员引导
 *   └─ BWTimeSelectView(vif p0) → 时间选择弹层
 */

'use client';

import { useState, useEffect, useMemo, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { useSleepPlayer } from '@/lib/sleep/player';
import { SLEEP_SCENES, TIMER_PRESETS } from '@/lib/sleep/config';
import { cn } from '@/lib/utils';
import { ArrowLeft, Play, Pause, Timer, Loader2, Crown, Infinity as InfinityIcon } from 'lucide-react';

/* ========================================================================== */
/*  工具函数（对齐 CountDownTimerView.S3 / U3 / X3）                          */
/* ========================================================================== */

/** 格式化倒计时为 HH:MM:SS（对齐 S3/U3/X3 方法） */
function formatCountDown(ms: number): string {
  const totalSec = Math.max(0, Math.ceil(ms / 1000));
  const h = Math.floor(totalSec / 3600);
  const m = Math.floor((totalSec % 3600) / 60);
  const s = totalSec % 60;
  const hh = h <= 0 ? '00' : h >= 10 ? String(h) : '0' + h;
  const mm = m <= 0 ? '00' : m >= 10 ? String(m) : '0' + m;
  const ss = s <= 0 ? '00' : s >= 10 ? String(s) : '0' + s;
  return `${hh}:${mm}:${ss}`;
}

/* ========================================================================== */
/*  背景层（HealTabBackgroundView 迁移）                                       */
/*  源码：K3(imageBg) / L3(videoBg)                                           */
/* ========================================================================== */

function BackgroundView({ imageBg }: { imageBg: string }) {
  return (
    <div className="absolute inset-0 z-0 overflow-hidden">
      {imageBg ? (
        <img
          src={imageBg}
          alt=""
          className="w-full h-full object-cover"
        />
      ) : (
        <div className="w-full h-full bg-gradient-to-b from-[#0d1b3e] via-[#1a237e] to-[#0a0e27]" />
      )}
      {/* 深色渐变叠加，保证文字可读性 */}
      <div className="absolute inset-0 bg-gradient-to-b from-black/30 via-transparent to-black/50" />
    </div>
  );
}

/* ========================================================================== */
/*  导航栏（QMNavigationBar 迁移）                                             */
/*  源码：t4(title) / o4(exit_btn) / h4(color) / L1→i4()                      */
/* ========================================================================== */

function NavBar({ title, onClose }: { title: string; onClose: () => void }) {
  return (
    <div
      className={cn(
        'relative z-20 flex items-center px-4',
        'h-11 min-h-11',
        'bg-white/5 backdrop-blur-xl backdrop-saturate-105',
        '-webkit-backdrop-blur-xl -webkit-backdrop-saturate-105'
      )}
    >
      <button
        onClick={onClose}
        className={cn(
          'flex items-center justify-center w-8 h-8 rounded-full',
          'text-white/90 hover:text-white',
          'hover:bg-white/10 active:scale-95 transition-all duration-200'
        )}
      >
        <ArrowLeft className="w-5 h-5" />
      </button>
      <span className="ml-2 text-[15px] font-medium text-white truncate">
        {title}
      </span>
    </div>
  );
}

/* ========================================================================== */
/*  气泡提示（BWBubbleView 迁移）                                             */
/*  源码：g2(j(0,0,0,0.2)) / 文字22dp / K3("定时关闭设置全站播放生效中")       */
/*        条件 J3() && I3().length>0 / L1→P0(false)                          */
/* ========================================================================== */

function BubbleView({ text, onClose }: { text: string; onClose: () => void }) {
  if (!text || text.length === 0) return null;
  return (
    <div className="relative z-20 flex justify-center px-4 mt-2">
      <div
        onClick={onClose}
        className={cn(
          'inline-flex items-center cursor-pointer',
          'bg-black/20 rounded-full',
          'px-5 py-2.5',
          'backdrop-blur-sm',
          'transition-all duration-200 active:scale-95'
        )}
      >
        <span className="text-[13px] text-white/90 select-none">{text}</span>
      </div>
    </div>
  );
}

/* ========================================================================== */
/*  倒计时圆环（CountDownTimerView 迁移）                                     */
/*  源码：H3(durationMs) / L3(currentMs) / S3/U3/X3格式化 / size=136dp       */
/* ========================================================================== */

function CountDownTimerView({
  durationMs,
  currentMs,
}: {
  durationMs: number;
  currentMs: number;
}) {
  const size = 136;
  const strokeWidth = 3;
  const radius = (size - strokeWidth * 2) / 2;
  const circumference = 2 * Math.PI * radius;
  const progress = durationMs > 0 ? Math.max(0, Math.min(1, currentMs / durationMs)) : 0;
  const strokeDashoffset = circumference * (1 - progress);

  return (
    <div
      className="relative flex items-center justify-center"
      style={{ width: size, height: size }}
    >
      <svg width={size} height={size} className="-rotate-90">
        {/* 背景圆环 */}
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          stroke="rgba(255,255,255,0.1)"
          strokeWidth={strokeWidth}
        />
        {/* 进度圆环 */}
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          stroke="rgba(255,255,255,0.8)"
          strokeWidth={strokeWidth}
          strokeLinecap="round"
          strokeDasharray={circumference}
          strokeDashoffset={strokeDashoffset}
          className="transition-all duration-1000 ease-linear"
        />
      </svg>
      {/* 中间时间显示 */}
      <span
        className="absolute text-white font-light tabular-nums tracking-wider"
        style={{ fontSize: 18 }}
      >
        {formatCountDown(currentMs)}
      </span>
    </div>
  );
}

/* ========================================================================== */
/*  会员引导（TextTipView 迁移）                                              */
/*  源码：K3("体验中，开通会员畅享疗愈内容") / J3("去开通") / L1→X0()         */
/*  容器：marginTop=100, marginBottom=60, marginLeft=60, width=60            */
/* ========================================================================== */

function TextTipView({
  title,
  buttonText,
  onClick,
}: {
  title: string;
  buttonText: string;
  onClick: () => void;
}) {
  return (
    <div className={cn('flex items-center gap-2', 'px-4 py-2', 'mt-[100px]')}>
      <span className="text-[12px] text-white/50">{title}</span>
      <button
        onClick={onClick}
        className={cn(
          'inline-flex items-center gap-1 px-3 py-1 rounded-full',
          'text-[12px] text-white/90 font-medium',
          'border border-white/20 bg-white/10',
          'hover:bg-white/15 active:scale-95 transition-all duration-200'
        )}
      >
        <Crown className="w-3 h-3" />
        {buttonText}
      </button>
    </div>
  );
}

/* ========================================================================== */
/*  时间选择弹层（BWTimeSelectView 迁移）                                     */
/*  源码：ModalView / 遮罩 j(0,0,0,0.4) / M3(list) / K3(selectedIndex)        */
/*        确认→d2(1, v3()) / 关闭→c2()                                        */
/*  预设：[15, 30, 60, 90]                                                    */
/* ========================================================================== */

function TimeSelectView({
  presets,
  selectedIndex,
  onSelect,
  onConfirm,
  onClose,
}: {
  presets: number[];
  selectedIndex: number;
  onSelect: (index: number) => void;
  onConfirm: () => void;
  onClose: () => void;
}) {
  return (
    <div
      className="fixed inset-0 z-50 flex items-end justify-center"
      onClick={onClose}
    >
      {/* 遮罩层：对齐 j(0,0,0,0.4) */}
      <div className="absolute inset-0 bg-black/40" />

      {/* 底部面板 */}
      <div
        onClick={(e) => e.stopPropagation()}
        className={cn(
          'relative w-full max-w-md',
          'bg-white/10 backdrop-blur-2xl backdrop-saturate-105',
          '-webkit-backdrop-blur-2xl -webkit-backdrop-saturate-105',
          'rounded-t-3xl',
          'pb-[env(safe-area-inset-bottom)]',
          'transition-transform duration-300 ease-out'
        )}
      >
        {/* 顶部拖拽指示器 */}
        <div className="flex justify-center pt-3 pb-1">
          <div className="w-9 h-1 rounded-full bg-white/20" />
        </div>

        {/* 标题 */}
        <div className="px-6 pt-2 pb-4">
          <span className="text-[15px] font-medium text-white">定时关闭</span>
        </div>

        {/* 时间选项列表 */}
        <div className="px-4 pb-4 space-y-1.5">
          {presets.map((minutes, index) => (
            <button
              key={minutes}
              onClick={() => onSelect(index)}
              className={cn(
                'flex items-center justify-between w-full px-4 py-3 rounded-xl',
                'transition-all duration-200 active:scale-[0.98]',
                selectedIndex === index
                  ? 'bg-white/20 text-white'
                  : 'bg-white/5 text-white/60 hover:bg-white/10 hover:text-white/80'
              )}
            >
              <span className="text-[14px]">{minutes} 分钟</span>
              {selectedIndex === index && (
                <span className="w-2 h-2 rounded-full bg-white" />
              )}
            </button>
          ))}
          {/* 无限模式 */}
          <button
            onClick={() => onSelect(-1)}
            className={cn(
              'flex items-center justify-between w-full px-4 py-3 rounded-xl',
              'transition-all duration-200 active:scale-[0.98]',
              selectedIndex === -1
                ? 'bg-white/20 text-white'
                : 'bg-white/5 text-white/60 hover:bg-white/10 hover:text-white/80'
            )}
          >
            <span className="text-[14px] flex items-center gap-2">
              <InfinityIcon className="w-4 h-4" />
              无限播放
            </span>
            {selectedIndex === -1 && (
              <span className="w-2 h-2 rounded-full bg-white" />
            )}
          </button>
        </div>

        {/* 确认/取消按钮 */}
        <div className="flex gap-3 px-4 pb-4 pt-2">
          <button
            onClick={onClose}
            className={cn(
              'flex-1 py-3 rounded-xl text-[14px] font-medium',
              'bg-white/5 text-white/60',
              'hover:bg-white/10 active:scale-95 transition-all duration-200'
            )}
          >
            取消
          </button>
          <button
            onClick={onConfirm}
            className={cn(
              'flex-1 py-3 rounded-xl text-[14px] font-medium',
              'bg-white/20 text-white',
              'hover:bg-white/25 active:scale-95 transition-all duration-200'
            )}
          >
            确认
          </button>
        </div>
      </div>
    </div>
  );
}

/* ========================================================================== */
/*  主页面（BWMainPlayerPager 迁移）                                          */
/*  源码：a4() build 方法 / e3() created / i4() closePage                     */
/* ========================================================================== */

export function SleepPlayerPage() {
  const router = useRouter();
  const player = useSleepPlayer();

  // ViewModel 状态字段（对齐 BWMainPlayerViewModel）
  const [showTimeSelect, setShowTimeSelect] = useState(false); // p0() timeSelectPanel
  const [showBubble, setShowBubble] = useState(false); // l0() showBubble
  const [selectedTimeIndex, setSelectedTimeIndex] = useState(0); // o0().second
  const [isVip] = useState(false); // n0() svip

  // 场景背景（对齐页面参数 image_bg）
  const scene = SLEEP_SCENES[0]; // ocean

  /* ---- ViewModel 生命周期对齐 ---- */

  // created → e3()：初始化（对齐 super.e3() → z5().I0()）
  // onPageOpened → N3()：页面打开通知（对齐 z5().d1()）
  useEffect(() => {
    // 对齐 ViewModel.I0()：注册 playStateChange / userPrivilegeChanged 事件
    // 在 React 中由 useSleepPlayer 内部处理，无需额外注册
  }, []);

  // 对齐 ViewModel.e1() 中 P0(h0() > 0) 逻辑：
  // 定时器运行时显示气泡
  useEffect(() => {
    if (player.timer.isRunning) {
      setShowBubble(true);
    }
  }, [player.timer.isRunning]);

  /* ---- ViewModel 状态映射 ---- */

  // j0() playState → player.playState
  // v0() = (j0()==1) → loading
  const isLoading = player.playState === 'loading';
  // x0() = (j0()==2) → playing
  const isPlaying = player.playState === 'playing';

  // h0() durationTime → 总时长 ms
  const durationMs = player.timer.timerMinutes * 60 * 1000;
  // g0() currentTime → 剩余时长 ms
  const currentMs = player.timer.remainingMs;

  // q0() timeTip → 对齐 ViewModel.e1() 中的文本生成逻辑
  const timeTip = useMemo(() => {
    const minutes = player.timer.timerMinutes;
    if (minutes <= 0) {
      return '自定义关闭时间';
    }
    if (player.timer.isRunning) {
      return `倒计时${minutes}分钟后关闭`;
    }
    return '自定义关闭时间';
  }, [player.timer.timerMinutes, player.timer.isRunning]);

  // o0() 时间选项列表 + 选中索引
  // 对齐 ViewModel.o0()：list=[15,30,60,90]，index 基于当前 timerMinutes
  const timePresets = useMemo(() => TIMER_PRESETS, []);
  const currentTimeIndex = useMemo(() => {
    const idx = timePresets.indexOf(player.timer.timerMinutes);
    return idx >= 0 ? idx : 0;
  }, [player.timer.timerMinutes, timePresets]);

  /* ---- 事件处理（对齐 ViewModel 方法） ---- */

  // 播放按钮点击 → 对齐 velse 分支 ImageView L1→N1 事件
  //   w0() → playState !== 'idle'
  //   u0() (j0()==4) → J0() restart
  //   x0() (j0()==2) → G0() pause
  //   else → K0() resume
  const handlePlayClick = useCallback(() => {
    if (isLoading) return; // v0() loading 时不响应
    if (isPlaying) {
      player.pause(); // G0()
    } else if (player.playState === 'paused') {
      player.resume(); // K0()
    } else {
      player.play(); // J0() restart / 首次播放
    }
  }, [isLoading, isPlaying, player]);

  // 设置按钮点击 → 对齐 ImageView L1→N1:
  //   if (z0()) Y0(true) → showTimeSelect = true
  const handleTimeSetupClick = useCallback(() => {
    setSelectedTimeIndex(currentTimeIndex);
    setShowTimeSelect(true); // Y0(true)
  }, [currentTimeIndex]);

  // 时间选择确认 → 对齐 BWTimeSelectView d2(1, v3()) 回调
  //   f1(vm, minutes*60*1000, 0, false) + Y0(false)
  const handleTimeConfirm = useCallback(() => {
    if (selectedTimeIndex === -1) {
      // 无限模式
      player.startTimer(0);
    } else {
      const minutes = timePresets[selectedTimeIndex];
      player.startTimer(minutes); // f1() → e1(minutes*60*1000, ...)
    }
    setShowTimeSelect(false); // Y0(false)
  }, [selectedTimeIndex, timePresets, player]);

  // 气泡点击 → 对齐 BWBubbleView L1→P0(false)
  const handleBubbleClose = useCallback(() => {
    setShowBubble(false); // P0(false)
  }, []);

  // 导航栏关闭 → 对齐 QMNavigationBar L1→i4() closePage
  //   i4() → if (q0) z5().a1() stop
  const handleClose = useCallback(() => {
    player.stop(); // a1() stop
    router.back(); // i4() closePage
  }, [player, router]);

  // 会员引导点击 → 对齐 TextTipView L1→X0()
  const handleVipClick = useCallback(() => {
    // X0()：跳转开通会员（简化为路由跳转）
    router.push('/profile');
  }, [router]);

  /* ---- 渲染（对齐 a4() build 方法组件树） ---- */

  return (
    <div className="relative w-full h-[100dvh] overflow-hidden flex flex-col">
      {/* 背景层 HealTabBackgroundView */}
      <BackgroundView imageBg={scene.imageBg} />

      {/* 导航栏 QMNavigationBar */}
      <NavBar title="助眠" onClose={handleClose} />

      {/* 气泡提示 BWBubbleView */}
      {/* 条件：J3() && I3().length > 0 → showBubble && isRunning */}
      {showBubble && player.timer.isRunning && (
        <BubbleView
          text="定时关闭设置全站播放生效中"
          onClose={handleBubbleClose}
        />
      )}

      {/* 主内容区 View(flexGrow=1) */}
      <div className="relative z-10 flex flex-col flex-1 items-center justify-center px-[60px]">
        {/* 倒计时+设置行容器 View(width=136) */}
        <div className="flex flex-col items-center" style={{ marginTop: 32 }}>
          {/* 倒计时圆环 CountDownTimerView */}
          {durationMs > 0 ? (
            <CountDownTimerView durationMs={durationMs} currentMs={currentMs} />
          ) : (
            <div
              className="flex items-center justify-center text-white/40"
              style={{ width: 136, height: 136 }}
            >
              <InfinityIcon className="w-8 h-8" />
            </div>
          )}

          {/* 时间设置行 View(width=40, flexDirection=row) */}
          <div className="flex flex-row items-center mt-5" style={{ minHeight: 40 }}>
            {/* TextView(timeTip) */}
            <span
              className="text-white/80 text-[13px] mr-7 ml-4 select-none whitespace-nowrap"
            >
              {timeTip}
            </span>
            {/* ImageView(设置按钮) 96x40dp */}
            <button
              onClick={handleTimeSetupClick}
              className={cn(
                'flex items-center justify-center',
                'rounded-full bg-white/10 backdrop-blur-sm',
                'hover:bg-white/15 active:scale-95 transition-all duration-200'
              )}
              style={{ width: 96, height: 40 }}
            >
              <Timer className="w-4 h-4 text-white/80" />
            </button>
          </div>
        </div>

        {/* 播放按钮区 vbind(j0()) */}
        <div className="mt-12">
          {/* vif(v0=loading) → 加载动画（旋转360°+缩放1.3x） */}
          {isLoading ? (
            <div
              className="flex items-center justify-center"
              style={{ width: 126, height: 126 }}
            >
              <Loader2
                className="w-12 h-12 text-white animate-spin"
                style={{ animationDuration: '0.8s' }}
              />
            </div>
          ) : (
            /* velse → 播放/暂停按钮 ImageView 126x126dp */
            <button
              onClick={handlePlayClick}
              className={cn(
                'flex items-center justify-center rounded-full',
                'bg-white/10 backdrop-blur-sm',
                'hover:bg-white/15 active:scale-95 transition-all duration-200'
              )}
              style={{ width: 126, height: 126 }}
            >
              {isPlaying ? (
                <Pause className="w-12 h-12 text-white" fill="currentColor" />
              ) : (
                <Play
                  className="w-12 h-12 text-white ml-1"
                  fill="currentColor"
                />
              )}
            </button>
          )}
        </div>

        {/* 会员引导 TextTipView vif(!n0()) */}
        {!isVip && (
          <TextTipView
            title="体验中，开通会员畅享疗愈内容"
            buttonText="去开通"
            onClick={handleVipClick}
          />
        )}
      </div>

      {/* 时间选择弹层 BWTimeSelectView vif(p0()) */}
      {showTimeSelect && (
        <TimeSelectView
          presets={timePresets}
          selectedIndex={selectedTimeIndex}
          onSelect={setSelectedTimeIndex}
          onConfirm={handleTimeConfirm}
          onClose={() => setShowTimeSelect(false)}
        />
      )}
    </div>
  );
}
