"use client";

import {
  CalendarClock,
  Clock,
  Cloud,
  Database,
  HardDrive,
  Headphones,
  ListMusic,
  Repeat,
  ShieldCheck,
  Smartphone,
  Upload,
  Volume2,
  type LucideIcon,
} from "lucide-react";

const colors = [
  ["#9ED2BE", "#7BC4A8", "#5BB892", "rgba(158,210,190,0.3)"],
  ["#A5C4E4", "#7BA8D4", "#528CC4", "rgba(165,196,228,0.3)"],
  ["#F5D0A9", "#F0B878", "#E8A04E", "rgba(245,208,169,0.3)"],
  ["#E8B4D4", "#D48CB8", "#C0649C", "rgba(232,180,212,0.3)"],
  ["#B8E0B8", "#90C890", "#68B068", "rgba(184,224,184,0.3)"],
  ["#D4C4F0", "#B8A4E0", "#9C84D0", "rgba(212,196,240,0.3)"],
  ["#FFB88C", "#FF9860", "#FF7838", "rgba(255,184,140,0.3)"],
  ["#FFC4C4", "#FF9898", "#FF6C6C", "rgba(255,196,196,0.3)"],
  ["#C4E0F0", "#98C4E0", "#6CA8D0", "rgba(196,224,240,0.3)"],
  ["#E0C4F0", "#C498E0", "#A86CD0", "rgba(224,196,240,0.3)"],
  ["#F0D4B8", "#E0B890", "#D09C68", "rgba(240,212,184,0.3)"],
  ["#B8D4F0", "#90B8E0", "#689CD0", "rgba(184,212,240,0.3)"],
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
];

export function TemplateSelector() {
  return (
    <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-2.5 sm:gap-3">
      {cards.map(([title, desc, Icon], i) => {
        const [from, via, to, shadow] = colors[i];
        return (
          <div
            key={title}
            className="sleep-capability-card group relative overflow-hidden rounded-xl p-3 text-left"
            style={{
              ["--card-shadow" as string]: shadow,
              ["--card-gradient" as string]: `radial-gradient(ellipse at 18% 18%, ${from}26 0%, transparent 58%), radial-gradient(ellipse at 84% 24%, ${via}22 0%, transparent 55%), radial-gradient(ellipse at 50% 92%, ${to}1f 0%, transparent 62%)`,
              ["--card-gradient-hover" as string]: `radial-gradient(ellipse at 18% 18%, ${from}38 0%, transparent 58%), radial-gradient(ellipse at 84% 24%, ${via}33 0%, transparent 55%), radial-gradient(ellipse at 50% 92%, ${to}2b 0%, transparent 62%)`,
            }}
          >
            <div className="sleep-capability-card__diffuse absolute inset-0 pointer-events-none" />
            <div className="relative z-10">
              <div className="mb-2.5 flex h-11 w-11 items-center justify-center rounded-xl transition-transform duration-300 group-hover:scale-110" style={{ background: `linear-gradient(135deg, ${from} 50%, ${to} 100%)`, boxShadow: `0 4px 12px ${shadow}` }}>
                <Icon className="h-5 w-5 text-black/55" />
              </div>
              <div className="mb-0.5 truncate text-sm font-bold text-foreground/90">{title}</div>
              <div className="line-clamp-2 text-xs leading-tight text-muted-foreground/70">{desc}</div>
            </div>
          </div>
        );
      })}
    </div>
  );
}
