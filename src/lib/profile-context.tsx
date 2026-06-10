"use client";

import { createContext, useContext, useState, ReactNode, useCallback, useRef } from "react";

interface ProfileContextValue {
  saving: boolean;
  setSaving: (saving: boolean) => void;
  submitHandler: (() => void) | null;
  setSubmitHandler: (handler: (() => void) | null) => void;
  cancelHandler: (() => void) | null;
  setCancelHandler: (handler: (() => void) | null) => void;
  /** 保存编辑前的数据快照 */
  snapshot: unknown | null;
  setSnapshot: (data: unknown) => void;
  /** 撤销到快照状态 */
  undoHandler: (() => void) | null;
  setUndoHandler: (handler: (() => void) | null) => void;
}

const ProfileContext = createContext<ProfileContextValue | null>(null);

export function ProfileProvider({ children }: { children: ReactNode }) {
  const [saving, setSaving] = useState(false);
  const [submitHandler, setSubmitHandler] = useState<(() => void) | null>(null);
  const [cancelHandler, setCancelHandler] = useState<(() => void) | null>(null);
  const [snapshot, setSnapshot] = useState<unknown | null>(null);
  const [undoHandler, setUndoHandler] = useState<(() => void) | null>(null);

  return (
    <ProfileContext.Provider value={{
      saving, setSaving,
      submitHandler, setSubmitHandler,
      cancelHandler, setCancelHandler,
      snapshot, setSnapshot,
      undoHandler, setUndoHandler,
    }}>
      {children}
    </ProfileContext.Provider>
  );
}

export function useProfile() {
  const ctx = useContext(ProfileContext);
  if (!ctx) throw new Error("useProfile must be used within ProfileProvider");
  return ctx;
}
