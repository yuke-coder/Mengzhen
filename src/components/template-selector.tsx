"use client";

import {
  CalendarClock,
  Check,
  Clock,
  Cloud,
  Database,
  FileAudio,
  HardDrive,
  Headphones,
  ListMusic,
  PlayCircle,
  Repeat,
  ShieldCheck,
  SlidersHorizontal,
  Smartphone,
  Upload,
  Volume2,
  type LucideIcon,
} from "lucide-react";

const colors = [
  ["#9ED2BE", "#7BC4A8", "#5BB892", "rgba(158,210,190,0.34)"],
  ["#A5C4E4", "#7BA8D4", "#528CC4", "rgba(165,196,228,0.34)"],
  ["#F5D0A9", "#F0B878", "#E8A04E", "rgba(245,208,169,0.34)"],
  ["#E8B4D4", "#D48CB8", "#C0649C", "rgba(232,180,212,0.34)"],
  ["#B8E0B8", "#90C890", "#68B068", "rgba(184,224,184,0.34)"],
  ["#D4C4F0", "#B8A4E0", "#9C84D0", "rgba(212,196,240,0.34)"],
  ["#FFB88C", "#FF9860", "#FF7838", "rgba(255,184,140,0.34)"],
  ["#FFC4C4", "#FF9898", "#FF6C6C", "rgba(255,196,196,0.34)"],
  ["#C4E0F0", "#98C4E0", "#6CA8D0", "rgba(196,224,240,0.34)"],
  ["#E0C4F0", "#C498E0", "#A86CD0", "rgba(224,196,240,0.34)"],
  ["#F0D4B8", "#E0B890", "#D09C68", "rgba(240,212,184,0.34)"],
  ["#B8D4F0", "#90B8E0", "#689CD0", "rgba(184,212,240,0.34)"],
  ["#BFE6FF", "#86C8F2", "#4AA8DF", "rgba(191,230,255,0.34)"],
  ["#D7F5C8", "#A9DF8E", "#7CC762", "rgba(215,245,200,0.34)"],
  ["#FFD6A5", "#FFB86B", "#F4973A", "rgba(255,214,165,0.34)"],
  ["#CFC7FF", "#AFA2F2", "#8674E6", "rgba(207,199,255,0.34)"],
] as const;

const cards: [string, string, LucideIcon][] = [
  ["私人音频", "导入你熟悉的助眠声音", Headphones],
  ["多格式上传", "适配 mp3、wav、ogg 等格式", Upload],
  ["本地持久", "游客音频可存入浏览器本地", HardDrive],
  ["云端同步", "登录后保存到云端音频库", Cloud],
  ["精准定时", "按年月日时分秒创建任务", CalendarClock],
  ["播放时长", "控制每次播放持续多久", Clock],
  ["淡入淡出", "减少突然启停带来的惊醒", Volume2],
  ["重复规则", "一次、每天、工作日、节假日", Repeat],
  ["任务列表", "查看播放状态并随时编辑", ListMusic],
  ["PWA辅助", "配合独立窗口和媒体控制", Smartphone],
  ["状态恢复", "页面回到前台后同步任务", Database],
  ["安静执行", "围绕夜间使用减少打扰", ShieldCheck],
  ["试听预览", "上传后先试听再加入任务", FileAudio],
  ["醒后续播", "半夜醒来一键接着播放", PlayCircle],
  ["参数微调", "音量与渐变秒数可细调", SlidersHorizontal],
];

const cardClass =
  "group relative overflow-hidden rounded-xl border border-border/45 bg-transparent p-3 text-left shadow-lg shadow-foreground/5 transition-all duration-300 ease-out will-change-transform hover:-translate-x-1.5 hover:-translate-y-2 hover:scale-[1.02] hover:border-[var(--brand-glow)]/40 hover:shadow-[16px_28px_62px_rgba(15,23,42,0.18),12px_20px_46px_var(--card-shadow),5px_9px_20px_var(--card-shadow)] dark:hover:shadow-[16px_28px_62px_rgba(0,0,0,0.42),12px_20px_46px_var(--card-shadow),5px_9px_20px_var(--card-shadow)]";

export function TemplateSelector() {
  return (
    <div className="grid grid-cols-2 gap-2.5 sm:grid-cols-3 sm:gap-3 lg:grid-cols-4">
      {cards.map(([title, desc, Icon], i) => {
        const [from, via, to, shadow] = colors[i];
        return (
          <div
            key={title}
            className={cardClass}
            style={{
              ["--card-shadow" as string]: shadow,
              ["--card-hover-light" as string]: `radial-gradient(ellipse at 16% 14%, ${from}8a 0%, transparent 56%), radial-gradient(ellipse at 86% 20%, ${via}78 0%, transparent 54%), radial-gradient(ellipse at 48% 96%, ${to}64 0%, transparent 62%), linear-gradient(135deg, transparent 0%, ${from}48 34%, ${via}3a 58%, ${to}24 100%)`,
              ["--card-hover-dark" as string]: `radial-gradient(ellipse at 18% 16%, ${from}68 0%, transparent 58%), radial-gradient(ellipse at 84% 22%, ${via}5c 0%, transparent 56%), radial-gradient(ellipse at 50% 96%, ${to}50 0%, transparent 64%), linear-gradient(135deg, transparent 0%, ${from}34 36%, ${via}2a 58%, transparent 100%)`,
            }}
          >
            <div className="pointer-events-none absolute inset-0 opacity-0 transition-opacity duration-300 group-hover:opacity-100 dark:hidden" style={{ background: "var(--card-hover-light)" }} />
            <div className="pointer-events-none absolute inset-0 hidden opacity-0 transition-opacity duration-300 dark:block dark:group-hover:opacity-75" style={{ background: "var(--card-hover-dark)" }} />
            <div
              className="pointer-events-none absolute right-2.5 top-2.5 flex h-6 w-6 translate-y-1 scale-75 items-center justify-center rounded-full opacity-0 shadow-md transition-all duration-300 group-hover:translate-y-0 group-hover:scale-100 group-hover:opacity-100"
              style={{ background: `linear-gradient(135deg, ${from}, ${to})`, boxShadow: `0 8px 18px ${shadow}` }}
            >
              <Check className="h-3.5 w-3.5 text-black/55" />
            </div>
            <div className="relative z-10">
              <div
                className="mb-2.5 flex h-11 w-11 items-center justify-center rounded-xl transition-transform duration-300 group-hover:scale-110"
                style={{ background: `linear-gradient(135deg, ${from} 50%, ${to} 100%)`, boxShadow: `0 4px 12px ${shadow}` }}
              >
                <Icon className="h-5 w-5 text-black/55" />
              </div>
              <div className="mb-0.5 truncate text-sm font-bold text-foreground/90 transition-colors duration-300 group-hover:text-foreground">{title}</div>
              <div className="line-clamp-2 text-xs leading-tight text-muted-foreground/70 transition-colors duration-300 group-hover:text-muted-foreground/85">{desc}</div>
            </div>
          </div>
        );
      })}
    </div>
  );
}
