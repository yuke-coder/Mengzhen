"use client";

import { useState, useEffect, useRef } from "react";
import { useAuth } from "@/lib/auth-context";
import { useRouter } from "next/navigation";
import { LocationSelector } from "@/components/location-selector";
import { formatFullAddress } from "@/lib/location-data";
import {
  Brain,
  Camera,
  Save,
  X,
  User,
  Mail,
  Calendar,
  MapPin,
  FileText,
  Heart,
  Loader2,
  Edit3,
  AlertCircle,
} from "lucide-react";

interface LocationValue {
  planet?: string;
  country?: string;
  province?: string;
  city?: string;
  district?: string;
}

interface ProfileFormData {
  username: string;
  nickname: string;
  gender: "male" | "female" | "secret" | "";
  birthday: string;
  location: LocationValue;
  signature: string;
  bio: string;
}

export default function ProfilePage() {
  const { user, loading, updateUser } = useAuth();
  const router = useRouter();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const usernameInputRef = useRef<HTMLInputElement>(null);
  const [saving, setSaving] = useState(false);
  const [uploadingAvatar, setUploadingAvatar] = useState(false);
  const [editingUsername, setEditingUsername] = useState(false);
  const [message, setMessage] = useState<{ type: "success" | "error"; text: string } | null>(null);
  const [formData, setFormData] = useState<ProfileFormData>({
    username: "",
    nickname: "",
    gender: "",
    birthday: "",
    location: { planet: "", country: "", province: "", city: "", district: "" },
    signature: "",
    bio: "",
  });

  useEffect(() => {
    if (!loading && !user) {
      router.push("/auth/login");
    }
  }, [loading, user, router]);

  // 加载用户名修改限制信息
  useEffect(() => {
    const loadProfile = async () => {
      try {
        const res = await fetch("/api/profile", {
          credentials: "include",
        });
        const data = await res.json();
        if (data.success) {
          // 解析 location 字符串为对象
          const locationObj: LocationValue = { planet: "", country: "", province: "", city: "", district: "" };
          if (data.profile.location) {
            const parts = data.profile.location.split('/');
            // 简化处理：假设 location 格式为 "省份/城市/区县"
            if (parts.length >= 1) locationObj.province = parts[0] || "";
            if (parts.length >= 2) locationObj.city = parts[1] || "";
            if (parts.length >= 3) locationObj.district = parts[2] || "";
            // 如果有星球或国家信息，从 parts[0] 中推断
            if (parts[0]?.includes('地球') || parts[0]?.includes('中国')) {
              locationObj.planet = 'earth';
              locationObj.country = 'CN';
            }
          }
          setFormData({
            username: data.profile.username || "",
            nickname: data.profile.nickname || data.profile.username || "",
            gender: data.profile.gender || "",
            birthday: data.profile.birthday || "",
            location: locationObj,
            signature: data.profile.signature || "",
            bio: data.profile.bio || "",
          });
        }
      } catch (error) {
        console.error("加载资料失败:", error);
      }
    };

    if (user) {
      loadProfile();
    }
  }, [user]);

  const handleAvatarUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file || !user) return;

    // 验证文件类型
    if (!file.type.startsWith("image/")) {
      setMessage({ type: "error", text: "请选择图片文件" });
      return;
    }

    // 验证文件大小（最大 5MB）
    if (file.size > 5 * 1024 * 1024) {
      setMessage({ type: "error", text: "图片大小不能超过 5MB" });
      return;
    }

    setUploadingAvatar(true);
    try {
      const avatarData = new FormData();
      avatarData.append("avatar", file);

      const res = await fetch("/api/avatar", {
        method: "POST",
        credentials: "include",
        body: avatarData,
      });

      const data = await res.json();
      if (data.success) {
        // 使用带时间戳的 URL 防止缓存
        const timestamp = Date.now();
        const newAvatarUrl = `${data.avatar_url}?t=${timestamp}`;
        updateUser({ avatar_url: newAvatarUrl });
        setMessage({ type: "success", text: "头像上传成功" });
      } else {
        setMessage({ type: "error", text: data.error || "上传失败" });
      }
    } catch {
      setMessage({ type: "error", text: "上传失败，请重试" });
    } finally {
      setUploadingAvatar(false);
    }
  };

  const defaultAvatars = {
    male: "https://aka.doubaocdn.com/s/1aJD1wOuCX",
    female: "https://aka.doubaocdn.com/s/qe1a1wOuCc",
    secret: "https://aka.doubaocdn.com/s/Q3oJ1wOuCh",
  };

  const handleResetAvatar = async () => {
    if (!user) return;
    setUploadingAvatar(true);
    try {
      const currentGender = formData.gender || "secret";
      const defaultAvatarUrl = defaultAvatars[currentGender as keyof typeof defaultAvatars];
      
      const res = await fetch(`/api/avatar?gender=${currentGender}`, {
        method: "DELETE",
        credentials: "include",
      });
      const data = await res.json();
      if (data.success) {
        updateUser({ avatar_url: defaultAvatarUrl });
        setMessage({ type: "success", text: "头像已重置" });
      }
    } catch {
      setMessage({ type: "error", text: "重置失败" });
    } finally {
      setUploadingAvatar(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return;

    setSaving(true);
    setMessage(null);
    try {
      // 将 location 对象转换为字符串格式
      const loc = formData.location;
      const submitData = {
        ...formData,
        location: loc.planet && loc.country
          ? `${loc.planet} > ${loc.country}${loc.province ? ` > ${loc.province}` : ""}${loc.city ? ` > ${loc.city}` : ""}${loc.district ? ` > ${loc.district}` : ""}`
          : undefined,
      };
      const res = await fetch("/api/profile", {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify(submitData),
      });

      const data = await res.json();
      if (data.success) {
        // 更新本地状态
        updateUser({
          username: data.profile.username,
          nickname: data.profile.nickname,
          avatar_url: data.profile.avatar_url,
          gender: data.profile.gender || null,
          birthday: data.profile.birthday || null,
          location: data.profile.location || null,
          signature: data.profile.signature || null,
          bio: data.profile.bio || null,
        });
        setFormData((prev) => ({
          ...prev,
          username: data.profile.username,
          nickname: data.profile.nickname,
        }));
        setEditingUsername(false);
        setMessage({ type: "success", text: data.message || "资料更新成功" });
        // 保存成功后返回上一页（预览环境使用 router.push）
        setTimeout(() => {
          if (window.location.hostname.includes('preview') || window.location.hostname.includes('dev.coze')) {
            router.push('/');
          } else {
            router.back();
          }
        }, 800);
      } else {
        setMessage({ type: "error", text: data.error || "更新失败" });
      }
    } catch {
      setMessage({ type: "error", text: "更新失败，请重试" });
    } finally {
      setSaving(false);
    }
  };

  const startEditUsername = () => {
    setEditingUsername(true);
    setTimeout(() => usernameInputRef.current?.focus(), 0);
  };

  if (loading || !user) {
    return (
      <div className="min-h-screen flex items-center justify-center" style={{ background: "var(--background)" }}>
        <Loader2 className="w-8 h-8 animate-spin" style={{ color: "var(--brand-start)" }} />
      </div>
    );
  }

  return (
    <div className="min-h-screen" style={{ background: "var(--background)" }}>
      {/* Header */}
      <header className="sticky top-0 z-50 backdrop-blur-xl border-b border-[var(--border)]" style={{ background: "rgba(5, 5, 16, 0.8)" }}>
        <div className="max-w-4xl mx-auto px-4 py-4">
          <div className="flex items-center justify-between">
            <button onClick={() => router.back()} className="p-2 rounded-lg hover:bg-[var(--muted)] transition-colors">
              <X className="w-5 h-5" style={{ color: "var(--foreground)" }} />
            </button>
            <h1 className="text-lg font-semibold" style={{ color: "var(--foreground)" }}>编辑资料</h1>
            <button
              onClick={handleSubmit}
              disabled={saving}
              className="flex items-center gap-2 px-4 py-2 rounded-lg font-medium transition-all hover:-translate-y-0.5 hover:shadow-lg active:scale-[0.98]"
              style={{
                background: "linear-gradient(135deg, var(--brand-start), var(--brand-end))",
                color: "white",
                opacity: saving ? 0.7 : 1
              }}
            >
              {saving ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
              保存
            </button>
          </div>
        </div>
      </header>

      {/* Content */}
      <main className="max-w-4xl mx-auto px-4 py-6">
        {/* Message */}
        {message && (
          <div
            className="mb-6 p-4 rounded-lg border flex items-center gap-3"
            style={{
              background: message.type === "success" ? "rgba(34, 197, 94, 0.1)" : "rgba(239, 68, 68, 0.1)",
              borderColor: message.type === "success" ? "rgba(34, 197, 94, 0.3)" : "rgba(239, 68, 68, 0.3)",
              color: message.type === "success" ? "#22c55e" : "#ef4444",
            }}
          >
            {message.type === "error" ? <AlertCircle className="w-5 h-5 flex-shrink-0" /> : null}
            <span>{message.text}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Avatar & Gender Section */}
          <div className="p-6 rounded-xl border" style={{ background: "var(--card)", borderColor: "var(--border)" }}>
            <div className="flex items-center gap-6">
              {/* Avatar Preview - Clickable */}
              <div 
                className="relative cursor-pointer group shrink-0"
                onClick={() => fileInputRef.current?.click()}
              >
                <div className="w-24 h-24 rounded-full overflow-hidden border-2 transition-all group-hover:border-[var(--brand-end)]" style={{ borderColor: "var(--brand-start)" }}>
                  {user.avatar_url ? (
                    <img src={`${user.avatar_url}${user.avatar_url.includes('?') ? '&' : '?'}t=${Date.now()}`} alt="头像" className="w-full h-full object-cover" />
                  ) : (
                    <div className="w-full h-full flex items-center justify-center" style={{ background: "var(--muted)" }}>
                      <User className="w-12 h-12" style={{ color: "var(--muted-foreground)" }} />
                    </div>
                  )}
                </div>
                {uploadingAvatar && (
                  <div className="absolute inset-0 flex items-center justify-center rounded-full" style={{ background: "rgba(0,0,0,0.5)" }}>
                    <Loader2 className="w-6 h-6 animate-spin text-white" />
                  </div>
                )}
                {/* Hover Overlay */}
                <div className="absolute inset-0 flex items-center justify-center rounded-full bg-black/50 opacity-0 group-hover:opacity-100 transition-opacity">
                  <Camera className="w-8 h-8 text-white" />
                </div>
              </div>

              {/* Gender Selection */}
              <div className="flex-1">
                <label className="flex items-center gap-2 text-sm font-medium mb-3" style={{ color: "var(--muted-foreground)" }}>
                  <Heart className="w-4 h-4" />
                  性别
                </label>
                <div className="flex gap-3">
                  {[
                    { value: "male", label: "男", emoji: "♂" },
                    { value: "female", label: "女", emoji: "♀" },
                    { value: "secret", label: "保密", emoji: "🔒" },
                  ].map((option) => (
                    <button
                      key={option.value}
                      type="button"
                      onClick={() => setFormData({ ...formData, gender: option.value as ProfileFormData["gender"] })}
                      className="flex-1 py-2.5 px-4 rounded-lg border font-medium transition-all"
                      style={{
                        background: formData.gender === option.value ? "var(--brand-start)" : "transparent",
                        borderColor: formData.gender === option.value ? "var(--brand-start)" : "var(--border)",
                        color: formData.gender === option.value ? "white" : "var(--foreground)",
                      }}
                    >
                      {option.emoji} {option.label}
                    </button>
                  ))}
                </div>
                {user.avatar_url && (
                  <button
                    type="button"
                    onClick={handleResetAvatar}
                    disabled={uploadingAvatar}
                    className="mt-3 px-4 py-2 rounded-lg font-medium border transition-colors hover:bg-[var(--muted)] text-sm"
                    style={{ borderColor: "var(--border)", color: "var(--muted-foreground)" }}
                  >
                    重置默认头像
                  </button>
                )}
              </div>
            </div>
            <input ref={fileInputRef} type="file" accept="image/*" onChange={handleAvatarUpload} className="hidden" />
          </div>

          {/* Username Section */}
          <div className="p-6 rounded-xl border" style={{ background: "var(--card)", borderColor: "var(--border)" }}>
            <label className="flex items-center gap-3 text-sm font-medium mb-3" style={{ color: "var(--muted-foreground)" }}>
              <Mail className="w-4 h-4" />
              用户名
            </label>

            {editingUsername ? (
              <div className="space-y-3">
                <div className="flex items-center gap-3">
                  <input
                    ref={usernameInputRef}
                    type="text"
                    value={formData.username}
                    onChange={(e) => setFormData({ ...formData, username: e.target.value })}
                    placeholder="输入用户名"
                    className="flex-1 px-4 py-3 rounded-lg border transition-colors focus:outline-none focus:ring-2"
                    style={{
                      background: "var(--background)",
                      borderColor: "var(--brand-start)",
                      color: "var(--foreground)",
                      "--tw-ring-color": "var(--brand-start)",
                    } as React.CSSProperties}
                  />
                  <button
                    type="button"
                    onClick={() => {
                      setEditingUsername(false);
                      setFormData((prev) => ({ ...prev, username: user.username || "" }));
                    }}
                    className="px-4 py-3 rounded-lg border font-medium transition-colors hover:bg-[var(--muted)]"
                    style={{ borderColor: "var(--border)", color: "var(--foreground)" }}
                  >
                    取消
                  </button>
                </div>
              </div>
            ) : (
              <div className="flex items-center justify-between">
                <div className="px-4 py-3 rounded-lg" style={{ background: "var(--muted)", color: "var(--foreground)" }}>
                  {user.username}
                </div>
                <button
                  type="button"
                  onClick={startEditUsername}
                  className="flex items-center gap-2 px-4 py-2 rounded-lg font-medium border transition-colors hover:bg-[var(--muted)]"
                  style={{
                    borderColor: "var(--brand-start)",
                    color: "var(--brand-start)",
                  }}
                >
                  <Edit3 className="w-4 h-4" />
                  修改
                </button>
              </div>
            )}

            <p className="text-xs mt-3" style={{ color: "var(--muted-foreground)" }}>
              用户名用于登录，可随时修改
            </p>
          </div>

          {/* Nickname (Unified with username, deprecated) */}
          <div className="p-6 rounded-xl border" style={{ background: "var(--card)", borderColor: "var(--border)" }}>
            <label className="flex items-center gap-3 text-sm font-medium mb-3" style={{ color: "var(--muted-foreground)" }}>
              <User className="w-4 h-4" />
              显示名称
              <span className="text-xs px-2 py-0.5 rounded" style={{ background: "var(--muted)", color: "var(--muted-foreground)" }}>
                选填
              </span>
            </label>
            <input
              type="text"
              value={formData.nickname}
              onChange={(e) => setFormData({ ...formData, nickname: e.target.value })}
              maxLength={30}
              placeholder="不填则显示用户名"
              className="w-full px-4 py-3 rounded-lg border transition-colors focus:outline-none focus:ring-2"
              style={{
                background: "var(--background)",
                borderColor: "var(--border)",
                color: "var(--foreground)",
                "--tw-ring-color": "var(--brand-start)",
              } as React.CSSProperties}
            />
            <p className="text-xs mt-2" style={{ color: "var(--muted-foreground)" }}>
              在个人页面和社区中显示的名称，不影响登录
            </p>
          </div>

          {/* Birthday */}
          <div className="p-6 rounded-xl border" style={{ background: "var(--card)", borderColor: "var(--border)" }}>
            <label className="flex items-center gap-3 text-sm font-medium mb-3" style={{ color: "var(--muted-foreground)" }}>
              <Calendar className="w-4 h-4" />
              生日
            </label>
            <input
              type="date"
              value={formData.birthday}
              onChange={(e) => setFormData({ ...formData, birthday: e.target.value })}
              className="w-full px-4 py-3 rounded-lg border transition-colors focus:outline-none focus:ring-2"
              style={{
                background: "var(--background)",
                borderColor: "var(--border)",
                color: "var(--foreground)",
                "--tw-ring-color": "var(--brand-start)",
              } as React.CSSProperties}
            />
          </div>

          {/* Location */}
          <div className="p-6 rounded-xl border" style={{ background: "var(--card)", borderColor: "var(--border)" }}>
            <label className="flex items-center gap-3 text-sm font-medium mb-3" style={{ color: "var(--muted-foreground)" }}>
              <MapPin className="w-4 h-4" />
              所在地
            </label>
            <LocationSelector
              value={formData.location}
              onChange={(newLocation) => setFormData({ ...formData, location: newLocation })}
            />
          </div>

          {/* Signature */}
          <div className="p-6 rounded-xl border" style={{ background: "var(--card)", borderColor: "var(--border)" }}>
            <label className="flex items-center gap-3 text-sm font-medium mb-3" style={{ color: "var(--muted-foreground)" }}>
              <FileText className="w-4 h-4" />
              个性签名
            </label>
            <input
              type="text"
              value={formData.signature}
              onChange={(e) => setFormData({ ...formData, signature: e.target.value })}
              maxLength={100}
              placeholder="一句话介绍自己"
              className="w-full px-4 py-3 rounded-lg border transition-colors focus:outline-none focus:ring-2"
              style={{
                background: "var(--background)",
                borderColor: "var(--border)",
                color: "var(--foreground)",
                "--tw-ring-color": "var(--brand-start)",
              } as React.CSSProperties}
            />
          </div>

          {/* Bio */}
          <div className="p-6 rounded-xl border" style={{ background: "var(--card)", borderColor: "var(--border)" }}>
            <label className="flex items-center gap-3 text-sm font-medium mb-3" style={{ color: "var(--muted-foreground)" }}>
              <FileText className="w-4 h-4" />
              个人简介
            </label>
            <textarea
              value={formData.bio}
              onChange={(e) => setFormData({ ...formData, bio: e.target.value })}
              maxLength={500}
              rows={4}
              placeholder="详细介绍一下自己..."
              className="w-full px-4 py-3 rounded-lg border transition-colors focus:outline-none focus:ring-2 resize-none"
              style={{
                background: "var(--background)",
                borderColor: "var(--border)",
                color: "var(--foreground)",
                "--tw-ring-color": "var(--brand-start)",
              } as React.CSSProperties}
            />
            <p className="text-xs mt-2 text-right" style={{ color: "var(--muted-foreground)" }}>
              {formData.bio.length}/500
            </p>
          </div>
        </form>
      </main>
    </div>
  );
}
