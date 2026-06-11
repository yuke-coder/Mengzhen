"use client";

import { useEffect } from "react";
import { toast } from "sonner";

export function ProfileToastListener() {
  useEffect(() => {
    const raw = sessionStorage.getItem("profile-toast");
    if (!raw) return;
    sessionStorage.removeItem("profile-toast");

    try {
      const { message } = JSON.parse(raw);
      toast.success(message, { duration: 2000 });
    } catch {}
  }, []);

  return null;
}
