'use client';

import React, { useState, useMemo, useCallback, useEffect } from 'react';
import { MindMapTemplate, TEMPLATE_CONFIG, TEMPLATE_CATEGORIES } from '@/lib/mindmap-types';
import { cn } from '@/lib/utils';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import {
  Search,
  Check,
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
  Scale,
  Sparkles,
  X,
} from 'lucide-react';

type CategoryKey = keyof typeof TEMPLATE_CATEGORIES;

const TEMPLATE_GRADIENTS: Record<MindMapTemplate, { from: string; via: string; to: string; shadow: string; lightFrom?: string; lightVia?: string; lightTo?: string }> = {
  radial:       { from: '#9ED2BE', via: '#7BC4A8', to: '#5BB892', shadow: 'rgba(158,210,190,0.3)', lightFrom: '#6BBF9E', lightVia: '#4DAF88', lightTo: '#359A72' },
  circle:      { from: '#A5C4E4', via: '#7BA8D4', to: '#528CC4', shadow: 'rgba(165,196,228,0.3)', lightFrom: '#7BA8D4', lightVia: '#5C92C4', lightTo: '#3E7CB4' },
  bubble:      { from: '#F5D0A9', via: '#F0B878', to: '#E8A04E', shadow: 'rgba(245,208,169,0.3)', lightFrom: '#E8B878', lightVia: '#DCA050', lightTo: '#D08A38' },
  'double-bubble': { from: '#E8B4D4', via: '#D48CB8', to: '#C0649C', shadow: 'rgba(232,180,212,0.3)', lightFrom: '#D48CB8', lightVia: '#C070A0', lightTo: '#A85488' },
  tree:        { from: '#B8E0B8', via: '#90C890', to: '#68B068', shadow: 'rgba(184,224,184,0.3)', lightFrom: '#88C888', lightVia: '#68B068', lightTo: '#4E9A4E' },
  bracket:     { from: '#D4C4F0', via: '#B8A4E0', to: '#9C84D0', shadow: 'rgba(212,196,240,0.3)', lightFrom: '#B8A4E0', lightVia: '#9C88C8', lightTo: '#806CB0' },
  flowchart:   { from: '#FFB88C', via: '#FF9860', to: '#FF7838', shadow: 'rgba(255,184,140,0.3)', lightFrom: '#F09860', lightVia: '#E07838', lightTo: '#D06020' },
  'multi-flow': { from: '#FFC4C4', via: '#FF9898', to: '#FF6C6C', shadow: 'rgba(255,196,196,0.3)', lightFrom: '#F09898', lightVia: '#E07070', lightTo: '#D04848' },
  bridge:      { from: '#C4E0F0', via: '#98C4E0', to: '#6CA8D0', shadow: 'rgba(196,224,240,0.3)', lightFrom: '#90B8D8', lightVia: '#6CA0C4', lightTo: '#4E88B0' },
  venn:        { from: '#E0C4F0', via: '#C498E0', to: '#A86CD0', shadow: 'rgba(224,196,240,0.3)', lightFrom: '#C498E0', lightVia: '#A878C8', lightTo: '#8C58B0' },
  fishbone:    { from: '#F0D4B8', via: '#E0B890', to: '#D09C68', shadow: 'rgba(240,212,184,0.3)', lightFrom: '#D8B090', lightVia: '#C89868', lightTo: '#B88048' },
  timeline:    { from: '#B8D4F0', via: '#90B8E0', to: '#689CD0', shadow: 'rgba(184,212,240,0.3)', lightFrom: '#88A8D0', lightVia: '#6890BC', lightTo: '#4E78A8' },
  'org-chart': { from: '#D4E0C4', via: '#B8D098', to: '#9CC06C', shadow: 'rgba(212,224,196,0.3)', lightFrom: '#A8C888', lightVia: '#8CB468', lightTo: '#709E48' },
  concept:     { from: '#F0C4D4', via: '#E098B8', to: '#D06C9C', shadow: 'rgba(240,196,212,0.3)', lightFrom: '#D898B0', lightVia: '#C47098', lightTo: '#B04C80' },
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

const ALL_CATEGORY = '全部';

interface TemplateGridProps {
  selected: MindMapTemplate[];
  onChange: (templates: MindMapTemplate[]) => void;
  maxSelect?: number;
  recommended?: MindMapTemplate[];
  showSearch?: boolean;
  compact?: boolean;
  inlineMode?: boolean;
}

export function TemplateGrid({
  selected,
  onChange,
  maxSelect = 5,
  recommended = [],
  showSearch = true,
  compact = false,
  inlineMode = false,
}: TemplateGridProps) {
  const [searchQuery, setSearchQuery] = useState('');
  const [activeCategory, setActiveCategory] = useState<string>(ALL_CATEGORY);
  const [isDark, setIsDark] = useState(false);

  useEffect(() => {
    const el = document.documentElement;
    setIsDark(el.classList.contains('dark'));
    const observer = new MutationObserver(() => {
      setIsDark(el.classList.contains('dark'));
    });
    observer.observe(el, { attributes: true, attributeFilter: ['class'] });
    return () => observer.disconnect();
  }, []);

  const templates = Object.keys(TEMPLATE_CONFIG) as MindMapTemplate[];
  const categories = [ALL_CATEGORY, ...Object.keys(TEMPLATE_CATEGORIES)];

  const filteredTemplates = useMemo(() => {
    let result = templates;

    if (activeCategory !== ALL_CATEGORY) {
      const categoryTemplates = TEMPLATE_CATEGORIES[activeCategory as CategoryKey] as MindMapTemplate[];
      result = result.filter(t => categoryTemplates.includes(t));
    }

    if (searchQuery.trim()) {
      const query = searchQuery.trim().toLowerCase();
      result = result.filter(t => {
        const config = TEMPLATE_CONFIG[t];
        return (
          config.name.toLowerCase().includes(query) ||
          config.description.toLowerCase().includes(query) ||
          t.toLowerCase().includes(query)
        );
      });
    }

    return result;
  }, [templates, activeCategory, searchQuery]);

  const toggleTemplate = useCallback((template: MindMapTemplate) => {
    onChange(
      selected.includes(template)
        ? selected.filter(t => t !== template)
        : selected.length >= maxSelect
          ? selected
          : [...selected, template]
    );
  }, [selected, onChange, maxSelect]);

  const groupedTemplates = useMemo(() => {
    if (activeCategory !== ALL_CATEGORY) {
      return [{ key: activeCategory, label: activeCategory, templates: filteredTemplates }];
    }
    return Object.keys(TEMPLATE_CATEGORIES)
      .map(key => ({
        key,
        label: key,
        templates: (TEMPLATE_CATEGORIES[key as CategoryKey] as MindMapTemplate[]).filter(t =>
          filteredTemplates.includes(t)
        ),
      }))
      .filter(g => g.templates.length > 0);
  }, [activeCategory, filteredTemplates]);

  return (
    <div className="flex flex-col min-h-0 flex-1">
      {showSearch && (
        <div className="shrink-0 space-y-3 pb-4">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
            <Input
              value={searchQuery}
              onChange={e => setSearchQuery(e.target.value)}
              placeholder="搜索模板名称或描述..."
              className="pl-9 bg-black/[0.03] dark:bg-muted/30 border-black/[0.06] dark:border-border/50 focus:border-[var(--brand-start)]/40 dark:focus:border-border"
            />
            {searchQuery && (
              <button
                onClick={() => setSearchQuery('')}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
              >
                <X className="w-4 h-4" />
              </button>
            )}
          </div>

          <div className="flex items-center gap-1.5 flex-wrap">
            {categories.map(cat => (
              <button
                key={cat}
                onClick={() => setActiveCategory(cat)}
                className={cn(
                  'px-3 py-1.5 rounded-full text-xs font-medium transition-all',
                  activeCategory === cat
                    ? 'bg-[var(--brand-start)] text-white shadow-sm'
                    : 'bg-black/[0.04] dark:bg-muted/50 text-foreground/60 dark:text-muted-foreground hover:bg-black/[0.07] dark:hover:bg-muted hover:text-foreground'
                )}
              >
                {cat}
              </button>
            ))}
            {inlineMode && (
              <div className="flex items-center gap-1.5 ml-auto">
                {selected.length > 0 && (
                  <>
                    {selected.map(t => (
                      <Badge
                        key={t}
                        variant="secondary"
                        className="text-xs gap-1 pr-1.5"
                      >
                        {TEMPLATE_CONFIG[t].name}
                        <button
                          onClick={() => toggleTemplate(t)}
                          className="ml-0.5 hover:text-destructive transition-colors"
                        >
                          <X className="w-3 h-3" />
                        </button>
                      </Badge>
                    ))}
                  </>
                )}
                <span className={cn(
                  "text-xs px-2.5 py-1 rounded-full font-medium",
                  selected.length >= maxSelect
                    ? "bg-destructive/15 text-destructive"
                    : "bg-black/[0.06] dark:bg-muted text-foreground/60 dark:text-muted-foreground"
                )}>
                  {selected.length}/{maxSelect}
                </span>
              </div>
            )}
          </div>

          {recommended.length > 0 && (
            <div className="flex items-center gap-2 px-3 py-2 rounded-lg bg-[var(--brand-start)]/[0.08] dark:bg-[var(--brand-start)]/0.08 border border-[var(--brand-start)]/[0.15] dark:border-[var(--brand-start)]/0.2">
              <Sparkles className="w-3.5 h-3.5 text-[var(--brand-start)] flex-shrink-0" />
              <span className="text-xs text-muted-foreground">
                AI推荐：
                {recommended.map((t, i) => (
                  <span key={t} className="inline-flex items-center gap-1 ml-1">
                    <span className="font-semibold text-[var(--brand-start)]">{TEMPLATE_CONFIG[t].name}</span>
                    {i < recommended.length - 1 && <span className="text-muted-foreground/60">、</span>}
                  </span>
                ))}
              </span>
            </div>
          )}
        </div>
      )}

      <div className="custom-scrollbar flex-1 min-h-0 overflow-y-auto scroll-smooth [&::-webkit-scrollbar]:w-1.5 [&::-webkit-scrollbar-track]:bg-transparent [&::-webkit-scrollbar-thumb]:rounded-full [&::-webkit-scrollbar-thumb]:bg-black/10 dark:[&::-webkit-scrollbar-thumb]:bg-[var(--brand-glow)]/25 [&::-webkit-scrollbar-thumb:hover]:bg-black/20 dark:[&::-webkit-scrollbar-thumb:hover]:bg-[var(--brand-glow)]/45 [&::-webkit-scrollbar-corner]:bg-transparent">
        <div className="space-y-6">
          {groupedTemplates.map(group => (
            <div key={group.key} className="space-y-3">
              <div className="text-xs font-semibold text-muted-foreground uppercase tracking-widest">
                {group.label}
              </div>
              <div className={cn(
                "gap-3",
                compact ? "grid grid-cols-2 sm:grid-cols-3" : "grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4"
              )}>
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
                        'relative p-3.5 rounded-xl border transition-all text-left group overflow-hidden',
                        isSelected
                          ? 'border-transparent shadow-lg scale-[1.02]'
                          : 'bg-white dark:bg-card border-black/[0.06] dark:border-border hover:border-[var(--brand-start)]/30 dark:hover:border-primary/30 hover:shadow-sm dark:hover:bg-accent/50',
                        isDisabled && 'opacity-40 cursor-not-allowed',
                        !isSelected && !isDisabled && isRecommended && 'ring-1 ring-[var(--brand-start)]/40'
                      )}
                    >
                      {isSelected && (
                        <div
                          className="absolute inset-0 opacity-100"
                          style={{
                            background: `linear-gradient(135deg, ${isDark ? gradient.from : (gradient.lightFrom || gradient.from)} 15%, ${isDark ? gradient.via : (gradient.lightVia || gradient.via)} 50%, ${isDark ? gradient.to : (gradient.lightTo || gradient.to)} 100%)`
                          }}
                        />
                      )}

                      {!isSelected && !isDisabled && isRecommended && (
                        <div className="absolute -top-1.5 -right-1.5 w-5 h-5 rounded-full bg-[var(--brand-start)] flex items-center justify-center shadow-md z-10">
                          <Sparkles className="w-3 h-3 text-background" />
                        </div>
                      )}

                      {isSelected && (
                        <div className="absolute top-2 right-2 w-5 h-5 rounded-full bg-white/25 backdrop-blur-sm flex items-center justify-center z-10">
                          <Check className="w-3 h-3 text-white" />
                        </div>
                      )}

                      <div className="relative z-10">
                        <div
                          className={cn(
                            'w-10 h-10 rounded-lg flex items-center justify-center mb-2 transition-transform duration-200',
                            isSelected ? 'bg-white/20 shadow-md' : '',
                            !isSelected && 'group-hover:scale-105'
                          )}
                          style={!isSelected ? {
                            background: `linear-gradient(135deg, ${gradient.from} 50%, ${gradient.to} 100%)`,
                            boxShadow: `0 4px 12px ${gradient.shadow}`
                          } : undefined}
                        >
                          <span style={{ color: isSelected ? 'white' : 'rgba(0,0,0,0.55)' }}>
                            {TEMPLATE_ICONS_MAP[template]}
                          </span>
                        </div>

                        <div className={cn(
                          "text-sm font-bold mb-0.5 truncate",
                          isSelected ? 'text-white' : 'text-foreground'
                        )}>
                          {config.name}
                        </div>

                        <div className={cn(
                          "text-xs leading-tight line-clamp-2",
                          isSelected ? 'text-white/75' : 'text-muted-foreground'
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

          {filteredTemplates.length === 0 && (
            <div className="flex flex-col items-center justify-center py-12 text-muted-foreground">
              <Search className="w-10 h-10 mb-3 opacity-30" />
              <p className="text-sm">未找到匹配的模板</p>
              <p className="text-xs mt-1">请尝试其他搜索词或分类</p>
            </div>
          )}
        </div>
      </div>


    </div>
  );
}

export { TEMPLATE_GRADIENTS, TEMPLATE_ICONS_MAP, ALL_CATEGORY };
