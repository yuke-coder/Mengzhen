'use client';

import React from 'react';
import { MindMapTemplate, TEMPLATE_CONFIG, TEMPLATE_CATEGORIES } from '@/lib/mindmap-types';
import { cn } from '@/lib/utils';
import {
  CircleDot,
  Circle,
  CircleDotDashed,
  GitBranch,
  ArrowRightLeft,
  Bone,
  Braces,
  Clock,
  Workflow,
  BookMarked,
  GitFork,
  Layers,
  Network,
  Scale
} from 'lucide-react';

interface TemplateSelectorProps {
  selected: MindMapTemplate[];
  onChange: (templates: MindMapTemplate[]) => void;
  maxSelect?: number;
  recommended?: MindMapTemplate[];
  onSubmit?: () => void; // 添加回车提交回调
}

// 类型化分组
type CategoryKey = keyof typeof TEMPLATE_CATEGORIES;
interface TemplateGroup {
  key: CategoryKey;
  label: string;
  templates: MindMapTemplate[];
}

// 每个模板独特的渐变色配置（模板身份色，保持不变）
const TEMPLATE_GRADIENTS: Record<MindMapTemplate, { from: string; via: string; to: string; shadow: string }> = {
  radial:       { from: '#9ED2BE', via: '#7BC4A8', to: '#5BB892', shadow: 'rgba(158,210,190,0.3)' },
  circle:      { from: '#A5C4E4', via: '#7BA8D4', to: '#528CC4', shadow: 'rgba(165,196,228,0.3)' },
  bubble:      { from: '#F5D0A9', via: '#F0B878', to: '#E8A04E', shadow: 'rgba(245,208,169,0.3)' },
  'double-bubble': { from: '#E8B4D4', via: '#D48CB8', to: '#C0649C', shadow: 'rgba(232,180,212,0.3)' },
  tree:        { from: '#B8E0B8', via: '#90C890', to: '#68B068', shadow: 'rgba(184,224,184,0.3)' },
  bracket:     { from: '#D4C4F0', via: '#B8A4E0', to: '#9C84D0', shadow: 'rgba(212,196,240,0.3)' },
  flowchart:   { from: '#FFB88C', via: '#FF9860', to: '#FF7838', shadow: 'rgba(255,184,140,0.3)' },
  'multi-flow': { from: '#FFC4C4', via: '#FF9898', to: '#FF6C6C', shadow: 'rgba(255,196,196,0.3)' },
  bridge:      { from: '#C4E0F0', via: '#98C4E0', to: '#6CA8D0', shadow: 'rgba(196,224,240,0.3)' },
  venn:        { from: '#E0C4F0', via: '#C498E0', to: '#A86CD0', shadow: 'rgba(224,196,240,0.3)' },
  fishbone:    { from: '#F0D4B8', via: '#E0B890', to: '#D09C68', shadow: 'rgba(240,212,184,0.3)' },
  timeline:    { from: '#B8D4F0', via: '#90B8E0', to: '#689CD0', shadow: 'rgba(184,212,240,0.3)' },
  'org-chart': { from: '#D4E0C4', via: '#B8D098', to: '#9CC06C', shadow: 'rgba(212,224,196,0.3)' },
  concept:     { from: '#F0C4D4', via: '#E098B8', to: '#D06C9C', shadow: 'rgba(240,196,212,0.3)' },
};

const TEMPLATE_ICONS_MAP: Record<MindMapTemplate, React.ReactNode> = {
  radial: <CircleDot className="w-5 h-5" />,
  circle: <Circle className="w-5 h-5" />,
  bubble: <CircleDotDashed className="w-5 h-5" />,
  'double-bubble': <Layers className="w-5 h-5" />,
  tree: <GitBranch className="w-5 h-5" />,
  bracket: <Braces className="w-5 h-5" />,
  flowchart: <ArrowRightLeft className="w-5 h-5" />,
  'multi-flow': <Workflow className="w-5 h-5" />,
  bridge: <Scale className="w-5 h-5" />,
  venn: <GitFork className="w-5 h-5" />,
  fishbone: <Bone className="w-5 h-5" />,
  timeline: <Clock className="w-5 h-5" />,
  'org-chart': <Network className="w-5 h-5" />,
  concept: <BookMarked className="w-5 h-5" />,
};

export function TemplateSelector({
  selected,
  onChange,
  maxSelect = 5,
  recommended = [],
  onSubmit
}: TemplateSelectorProps) {
  const templates = Object.keys(TEMPLATE_CONFIG) as MindMapTemplate[];

  // 处理键盘事件
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && selected.length > 0 && onSubmit) {
      e.preventDefault();
      onSubmit();
    }
  };

  const toggleTemplate = (template: MindMapTemplate) => {
    if (selected.includes(template)) {
      onChange(selected.filter(t => t !== template));
    } else if (selected.length < maxSelect) {
      onChange([...selected, template]);
    }
  };

  // 按分类分组
  const categories = Object.keys(TEMPLATE_CATEGORIES) as CategoryKey[];
  const groupedTemplates: TemplateGroup[] = categories
    .map((categoryKey) => ({
      key: categoryKey,
      label: categoryKey,
      templates: TEMPLATE_CATEGORIES[categoryKey] as MindMapTemplate[]
    }))
    .filter((g) => g.templates.length > 0);

  return (
    <div className="space-y-4" onKeyDown={handleKeyDown}>
      {/* AI推荐提示 */}
      {recommended.length > 0 && (
        <div className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-[var(--brand-start)]/0.08 border border-[var(--brand-start)]/0.25">
          <svg className="w-4 h-4 text-[var(--brand-start)] flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
          </svg>
          <span className="text-xs text-muted-foreground">
            AI推荐：
            {recommended.map((t, i) => (
              <span key={t} className="inline-flex items-center gap-1 ml-1">
                <span className="font-semibold text-[var(--brand-start)]">{TEMPLATE_CONFIG[t].name}</span>
                {i < recommended.length - 1 && <span className="text-muted-foreground/60">、</span>}
              </span>
            ))}
            <span className="ml-1 text-muted-foreground/70">（根据文章内容智能推荐）</span>
          </span>
        </div>
      )}

      {/* 选择计数 */}
      <div className="flex items-center justify-between">
        <span className="text-sm font-semibold text-foreground">选择脑图类型</span>
        <span className={cn(
          "text-xs px-2.5 py-1 rounded-full font-medium",
          selected.length >= maxSelect
            ? "bg-destructive/15 text-destructive"
            : "bg-muted text-muted-foreground"
        )}>
          已选 {selected.length}/{maxSelect}
        </span>
      </div>

      {/* 分类模板列表 */}
      {groupedTemplates.map(group => (
        <div key={group.key} className="space-y-2.5">
          <div className="text-xs font-semibold text-muted-foreground uppercase tracking-widest">{group.label}</div>
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-7 gap-2.5">
            {group.templates.map(template => {
              const config = TEMPLATE_CONFIG[template];
              const gradient = TEMPLATE_GRADIENTS[template];
              const isSelected = selected.includes(template);
              const isDisabled = !isSelected && selected.length >= maxSelect;
              const isRecommended = recommended.includes(template);

              return (
                <button
                  key={template}
                  onClick={() => !isDisabled && toggleTemplate(template)}
                  disabled={isDisabled}
                  className={cn(
                    'relative p-3 rounded-xl border transition-all text-left group overflow-hidden',
                    'bg-card border-border hover:border-primary/30 hover:bg-accent/50',
                    isDisabled && 'opacity-40 cursor-not-allowed',
                    isRecommended && 'ring-1 ring-[var(--brand-start)]/40'
                  )}
                >
                  {/* Hover时的渐变背景 */}
                  {!isDisabled && (
                    <div
                      className="absolute inset-0 opacity-0 group-hover:opacity-100 transition-opacity duration-200"
                      style={{
                        background: `linear-gradient(135deg, ${gradient.from} 15%, ${gradient.via} 50%, ${gradient.to} 100%)`
                      }}
                    />
                  )}

                  {/* 推荐标记 */}
                  {!isDisabled && isRecommended && (
                    <div className="absolute -top-1.5 -right-1.5 w-5 h-5 rounded-full bg-[var(--brand-start)] flex items-center justify-center shadow-md z-10">
                      <svg className="w-3 h-3 text-background" fill="currentColor" viewBox="0 0 20 20">
                        <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                      </svg>
                    </div>
                  )}

                  {/* Check Mark - Hover时显示 */}
                  {!isDisabled && (
                    <div className="absolute top-2 right-2 w-5 h-5 rounded-full bg-white/25 backdrop-blur-sm flex items-center justify-center z-10 opacity-0 group-hover:opacity-100 transition-opacity duration-200">
                      <svg className="w-3 h-3 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={3}>
                        <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
                      </svg>
                    </div>
                  )}

                  {/* 内容区 - 相对定位确保在渐变背景之上 */}
                  <div className="relative z-10">
                    {/* 渐变卡片图标 - 未选中：深色图标；Hover：白色图标 */}
                    <div
                      className={cn(
                        'w-11 h-11 rounded-xl flex items-center justify-center mb-2.5 transition-all duration-200',
                        'group-hover:bg-white/20 group-hover:shadow-md group-hover:scale-105'
                      )}
                      style={{
                        background: `linear-gradient(135deg, ${gradient.from} 50%, ${gradient.to} 100%)`,
                        boxShadow: `0 4px 12px ${gradient.shadow}`
                      }}
                    >
                      {/* 图标颜色：未选中用深色（在浅色渐变上可见），Hover用白色 */}
                      <span style={{ color: 'rgba(0,0,0,0.55)' }} className="group-hover:text-white transition-colors duration-200">
                        {TEMPLATE_ICONS_MAP[template]}
                      </span>
                    </div>

                    {/* Name */}
                    <div className={cn(
                      "text-sm font-bold mb-0.5 truncate transition-colors duration-200",
                      "group-hover:text-white"
                    )}>
                      {config.name}
                    </div>

                    {/* Description */}
                    <div className={cn(
                      "text-xs leading-tight line-clamp-2 transition-colors duration-200",
                      "group-hover:text-white/75"
                    )}>
                      {config.description}
                    </div>
                  </div>
                </button>
              );
            })}
          </div>
        </div>
      ))}
    </div>
  );
}
