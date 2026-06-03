'use client';

import React, { createContext, useContext, useState, useCallback } from 'react';
import { MindMapTemplate } from '@/lib/mindmap-types';

interface TemplateModalContextValue {
  openTemplateModal: () => void;
  isTemplateModalOpen: boolean;
  closeTemplateModal: () => void;
  selectedTemplates: MindMapTemplate[];
  setSelectedTemplates: (templates: MindMapTemplate[]) => void;
}

const TemplateModalContext = createContext<TemplateModalContextValue | null>(null);

export function useTemplateModal() {
  const ctx = useContext(TemplateModalContext);
  if (!ctx) throw new Error('useTemplateModal must be used within TemplateModalProvider');
  return ctx;
}

export function TemplateModalProvider({ children }: { children: React.ReactNode }) {
  const [modalOpen, setModalOpen] = useState(false);
  const [selectedTemplates, setSelectedTemplates] = useState<MindMapTemplate[]>([]);

  const openTemplateModal = useCallback(() => {
    setModalOpen(true);
  }, []);

  const closeTemplateModal = useCallback(() => {
    setModalOpen(false);
  }, []);

  return (
    <TemplateModalContext.Provider value={{ openTemplateModal, isTemplateModalOpen: modalOpen, closeTemplateModal, selectedTemplates, setSelectedTemplates }}>
      {children}
    </TemplateModalContext.Provider>
  );
}
