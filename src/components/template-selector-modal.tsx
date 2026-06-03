'use client';

import React, { useState, useCallback, useEffect } from 'react';
import { MindMapTemplate, TEMPLATE_CONFIG } from '@/lib/mindmap-types';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { TemplateGrid } from '@/components/template-grid';
import {
  Check,
  Sparkles,
} from 'lucide-react';

interface TemplateModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  selected: MindMapTemplate[];
  onChange: (templates: MindMapTemplate[]) => void;
  maxSelect?: number;
  recommended?: MindMapTemplate[];
}

export function TemplateSelectorModal({
  open,
  onOpenChange,
  selected,
  onChange,
  maxSelect = 5,
  recommended = [],
}: TemplateModalProps) {
  const [localSelected, setLocalSelected] = useState<MindMapTemplate[]>(selected);

  useEffect(() => {
    if (open) {
      setLocalSelected(selected);
    }
  }, [open, selected]);

  const handleLocalChange = useCallback((templates: MindMapTemplate[]) => {
    setLocalSelected(templates);
  }, []);

  const handleConfirm = useCallback(() => {
    onChange(localSelected);
    onOpenChange(false);
  }, [localSelected, onChange, onOpenChange]);

  const handleCancel = useCallback(() => {
    onOpenChange(false);
  }, [onOpenChange]);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-3xl max-h-[85vh] flex flex-col p-0 gap-0 overflow-hidden shadow-2xl ring-1 ring-black/[0.04] dark:ring-0">
        <DialogHeader className="shrink-0 px-6 pt-6 pb-4 border-b border-black/5 dark:border-border/50">
          <DialogTitle className="flex items-center gap-2 text-lg">
            <Sparkles className="w-5 h-5 text-[var(--brand-glow)]" />
            选择脑图模板
          </DialogTitle>
          <DialogDescription className="text-sm text-muted-foreground">
            最多选择 {maxSelect} 种模板同时生成，AI 将根据文章内容智能构建脑图
          </DialogDescription>
        </DialogHeader>

        <div className="flex-1 min-h-0 px-6 py-4 flex flex-col overflow-hidden">
          <TemplateGrid
            selected={localSelected}
            onChange={handleLocalChange}
            maxSelect={maxSelect}
            recommended={recommended}
            showSearch={true}
          />
        </div>

        <DialogFooter className="shrink-0 px-6 py-4 border-t border-black/5 dark:border-border/50 bg-black/[0.02] dark:bg-muted/20">
          <div className="flex items-center justify-between w-full">
            <div className="flex items-center gap-2">
              {localSelected.length > 0 ? (
                <span className="text-sm text-muted-foreground">
                  已选 <span className="font-semibold text-[var(--brand-start)]">{localSelected.length}</span> 种模板
                </span>
              ) : (
                <span className="text-xs text-muted-foreground">请至少选择一种模板</span>
              )}
              <span className="text-xs px-2.5 py-1 rounded-full font-medium ml-2 bg-black/[0.06] dark:bg-muted text-foreground/60 dark:text-muted-foreground">
                {localSelected.length}/{maxSelect}
              </span>
            </div>

            <div className="flex items-center gap-2">
              <Button variant="outline" onClick={handleCancel} className="border-black/[0.08] dark:border-border hover:bg-black/[0.04] dark:hover:bg-accent/50">
                取消
              </Button>
              <Button
                onClick={handleConfirm}
                disabled={localSelected.length === 0}
                className="bg-gradient-to-r from-[var(--brand-start)] to-[var(--brand-end)] text-white hover:opacity-90"
              >
                <Check className="w-4 h-4 mr-1.5" />
                确认选择{localSelected.length > 0 ? ` (${localSelected.length})` : ''}
              </Button>
            </div>
          </div>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
