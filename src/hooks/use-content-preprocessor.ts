"use client";

import { useState, useCallback } from 'react';

export interface PreprocessStats {
  wordCount: number;
  charCount: number;
  estimatedTemplates: number;
  paragraphCount: number;
  lineCount: number;
}

export interface PhaseInfo {
  phase: 'normalize' | 'paragraphs' | 'structure' | 'streaming';
  done: boolean;
}

export function useContentPreprocessor() {
  const [isProcessing, setIsProcessing] = useState(false);
  const [isComplete, setIsComplete] = useState(false);
  const [stats, setStats] = useState<PreprocessStats>({
    wordCount: 0,
    charCount: 0,
    estimatedTemplates: 0,
    paragraphCount: 0,
    lineCount: 0,
  });
  const [preprocessedContent, setPreprocessedContent] = useState<string>('');
  const [paragraphs, setParagraphs] = useState<string[]>([]);
  const [currentPhase, setCurrentPhase] = useState<PhaseInfo | null>(null);

  const processContent = useCallback(async (content: string) => {
    setIsProcessing(true);
    setIsComplete(false);
    setParagraphs([]);

    const phases: Array<{ phase: PhaseInfo['phase']; delay: number }> = [
      { phase: 'normalize', delay: 200 },
      { phase: 'paragraphs', delay: 300 },
      { phase: 'structure', delay: 300 },
      { phase: 'streaming', delay: 200 },
    ];

    try {
      for (const { phase, delay } of phases) {
        setCurrentPhase({ phase, done: false });
        await new Promise(resolve => setTimeout(resolve, delay));
        setCurrentPhase({ phase, done: true });

        // 在 paragraphs 阶段提取段落
        if (phase === 'paragraphs') {
          const extractedParagraphs = content
            .split(/\n\n+/)
            .map(p => p.trim())
            .filter(p => p.length > 0);
          setParagraphs(extractedParagraphs);
        }
      }

      // 计算统计信息
      const charCount = content.length;
      const wordCount = content.split(/\s+/).filter(w => w.length > 0).length;
      const estimatedTemplates = Math.min(Math.ceil(wordCount / 50), 10);
      const paragraphCount = paragraphs.length;
      const lineCount = content.split('\n').length;

      setStats({
        wordCount,
        charCount,
        estimatedTemplates,
        paragraphCount,
        lineCount,
      });
      setPreprocessedContent(content);
      setIsComplete(true);
    } catch {
      setIsComplete(false);
    } finally {
      setIsProcessing(false);
      setCurrentPhase(null);
    }
  }, []);

  const reset = useCallback(() => {
    setIsProcessing(false);
    setIsComplete(false);
    setStats({
      wordCount: 0,
      charCount: 0,
      estimatedTemplates: 0,
      paragraphCount: 0,
      lineCount: 0,
    });
    setPreprocessedContent('');
    setParagraphs([]);
    setCurrentPhase(null);
  }, []);

  return {
    isProcessing,
    isComplete,
    stats,
    preprocessedContent,
    processContent,
    reset,
    paragraphs,
    currentPhase,
  };
}
