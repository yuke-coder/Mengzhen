"use client";

import { initializeAudioContext, tryUnlockAudio } from "./audio";

export async function unlockAudio(): Promise<boolean> {
  tryUnlockAudio();
  return initializeAudioContext();
}

export function setupAutoUnlock(): () => void {
  const handler = () => unlockAudio();
  document.addEventListener('click', handler, { once: true });
  document.addEventListener('touchstart', handler, { once: true });
  document.addEventListener('keydown', handler, { once: true });
  return () => {
    document.removeEventListener('click', handler);
    document.removeEventListener('touchstart', handler);
    document.removeEventListener('keydown', handler);
  };
}

