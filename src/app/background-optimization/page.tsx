"use client";

import { useState, useEffect, useCallback } from "react";
import { useRouter } from "next/navigation";
import { toast } from "@/components/sonner";
// Capacitor 桥接已移除，后台优化功能由纯原生 App 处理
const isNativePlatform = () => false;
const checkBatteryOptimization = async () => false;
const requestBatteryOptimization = async () => false;
const openAutoStartSettings = async () => false;
const openNotificationSettings = async () => false;
const openExactAlarmSettings = async () => false;
const getDeviceInfo = async () => null;
const hasNotificationPermission = async () => true;
const canScheduleExactAlarms = async () => true;
import { ChevronLeft, Battery, Bell, AlarmClock, Smartphone, Shield, CheckCircle2, AlertCircle, ExternalLink } from "lucide-react";

interface CheckItem {
  key: string;
  title: string;
  description: string;
  icon: React.ReactNode;
  status: "checking" | "ok" | "warning";
  actionLabel: string;
  onAction: () => Promise<void>;
}

export default function BackgroundOptimizationPage() {
  const router = useRouter();
  const [native, setNative] = useState(false);
  const [deviceInfo, setDeviceInfo] = useState<{
    brand: string;
    manufacturer: string;
    model: string;
    androidVersion: string;
  } | null>(null);

  // 各项状态
  const [batteryOptimized, setBatteryOptimized] = useState<boolean | null>(null);
  const [notifGranted, setNotifGranted] = useState<boolean | null>(null);
  const [exactAlarm, setExactAlarm] = useState<boolean | null>(null);

  useEffect(() => {
    const isNative = isNativePlatform();
    setNative(isNative);

    if (isNative) {
      getDeviceInfo().then((info) => {
        if (info) {
          setDeviceInfo({
            brand: info.brand,
            manufacturer: info.manufacturer,
            model: info.model,
            androidVersion: info.androidVersion,
          });
        }
      });

      checkBatteryOptimization().then(setBatteryOptimized);
      hasNotificationPermission().then(setNotifGranted);
      canScheduleExactAlarms().then(setExactAlarm);
    }
  }, []);

  const handleBatteryOpt = useCallback(async () => {
    const ok = await requestBatteryOptimization();
    if (ok) {
      toast.success("已跳转到电池优化设置页", {
        description: "请将梦枕加入白名单以确保息屏播放不被中断",
      });
      // 延迟刷新状态（用户可能需要从设置页返回）
      setTimeout(() => {
        checkBatteryOptimization().then(setBatteryOptimized);
      }, 2000);
    } else {
      toast.error("无法打开电池优化设置");
    }
  }, []);

  const handleAutoStart = useCallback(async () => {
    const opened = await openAutoStartSettings();
    if (opened) {
      toast.success("已跳转到自启动管理", {
        description: "请允许梦枕自启动",
      });
    } else {
      toast.warning("无法识别设备品牌", {
        description: "请手动到设置 > 应用管理 > 梦枕 中开启自启动",
      });
    }
  }, []);

  const handleNotification = useCallback(async () => {
    const opened = await openNotificationSettings();
    if (opened) {
      toast.success("已跳转到通知设置", {
        description: "请确保梦枕的通知权限已开启",
      });
      setTimeout(() => {
        hasNotificationPermission().then(setNotifGranted);
      }, 2000);
    }
  }, []);

  const handleExactAlarm = useCallback(async () => {
    const opened = await openExactAlarmSettings();
    if (opened) {
      toast.success("已跳转到闹钟权限设置", {
        description: "请允许梦枕使用精确闹钟",
      });
      setTimeout(() => {
        canScheduleExactAlarms().then(setExactAlarm);
      }, 2000);
    }
  }, []);

  if (!native) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center p-6">
        <div className="max-w-md text-center space-y-4">
          <Smartphone className="w-16 h-16 mx-auto text-muted-foreground" />
          <h2 className="text-xl font-semibold">仅在 Android 原生环境可用</h2>
          <p className="text-muted-foreground text-sm">
            后台播放优化功能仅在使用梦枕 Android 客户端时可用。
            请先安装梦枕 App。
          </p>
          <button
            onClick={() => router.back()}
            className="text-primary hover:underline text-sm"
          >
            ← 返回
          </button>
        </div>
      </div>
    );
  }

  const brandLabel = deviceInfo?.brand
    ? deviceInfo.brand.charAt(0).toUpperCase() + deviceInfo.brand.slice(1)
    : "您的";

  return (
    <div className="min-h-screen bg-background">
      {/* 顶部导航 */}
      <div className="sticky top-0 z-10 bg-background/80 backdrop-blur border-b">
        <div className="max-w-2xl mx-auto px-4 py-3 flex items-center gap-3">
          <button
            onClick={() => router.back()}
            className="p-1.5 rounded-lg hover:bg-muted transition-colors"
          >
            <ChevronLeft className="w-5 h-5" />
          </button>
          <h1 className="text-lg font-semibold">后台播放优化</h1>
        </div>
      </div>

      <div className="max-w-2xl mx-auto px-4 py-6 space-y-6">
        {/* 说明卡片 */}
        <div className="rounded-xl bg-muted/50 p-4 space-y-2">
          <div className="flex items-center gap-2">
            <Shield className="w-5 h-5 text-primary" />
            <h2 className="font-medium text-sm">为什么需要优化？</h2>
          </div>
          <p className="text-xs text-muted-foreground leading-relaxed">
            Android 系统为节省电量，会限制后台应用运行。为确保梦枕在息屏状态下能准时播放音频，
            需要开启以下权限。各品牌手机设置方式略有不同。
          </p>
          {deviceInfo && (
            <div className="text-xs text-muted-foreground pt-1">
              当前设备：{brandLabel} {deviceInfo.model} · Android {deviceInfo.androidVersion}
            </div>
          )}
        </div>

        {/* 检查项列表 */}
        <div className="space-y-3">
          <CheckCard
            icon={<Battery className="w-5 h-5" />}
            title="电池优化白名单"
            description={`允许梦枕在后台持续运行，不被系统省电策略杀死`}
            status={
              batteryOptimized === null
                ? "checking"
                : batteryOptimized
                ? "ok"
                : "warning"
            }
            actionLabel="加入白名单"
            onAction={handleBatteryOpt}
          />

          <CheckCard
            icon={<Smartphone className="w-5 h-5" />}
            title="自启动管理"
            description={`允许梦枕开机后自动启动，确保闹钟任务在重启后仍可触发`}
            status="warning"
            actionLabel={`${brandLabel}手机自启动设置`}
            onAction={handleAutoStart}
          />

          <CheckCard
            icon={<Bell className="w-5 h-5" />}
            title="通知权限"
            description="前台播放通知需要通知权限，Android 13+ 必须授权"
            status={
              notifGranted === null
                ? "checking"
                : notifGranted
                ? "ok"
                : "warning"
            }
            actionLabel="通知设置"
            onAction={handleNotification}
          />

          <CheckCard
            icon={<AlarmClock className="w-5 h-5" />}
            title="精确闹钟权限"
            description="确保定时播放任务在息屏下精确触发（Android 12+ 需要）"
            status={
              exactAlarm === null
                ? "checking"
                : exactAlarm
                ? "ok"
                : "warning"
            }
            actionLabel="闹钟权限设置"
            onAction={handleExactAlarm}
          />
        </div>

        {/* 底部说明 */}
        <div className="text-xs text-muted-foreground space-y-1 pt-4 border-t">
          <p>· 以上设置仅需操作一次，系统会记住您的选择</p>
          <p>· 不同品牌手机的设置路径可能略有不同</p>
          <p>· 如设置后仍无法息屏播放，请尝试在手机管家/安全中心中添加信任</p>
        </div>
      </div>
    </div>
  );
}

function CheckCard({
  icon,
  title,
  description,
  status,
  actionLabel,
  onAction,
}: {
  icon: React.ReactNode;
  title: string;
  description: string;
  status: "checking" | "ok" | "warning";
  actionLabel: string;
  onAction: () => Promise<void>;
}) {
  return (
    <div className="rounded-xl border bg-card p-4 flex items-start gap-3">
      <div className="mt-0.5 text-muted-foreground">{icon}</div>
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2">
          <h3 className="font-medium text-sm">{title}</h3>
          {status === "ok" && (
            <CheckCircle2 className="w-4 h-4 text-green-500 flex-shrink-0" />
          )}
          {status === "warning" && (
            <AlertCircle className="w-4 h-4 text-orange-500 flex-shrink-0" />
          )}
        </div>
        <p className="text-xs text-muted-foreground mt-1 leading-relaxed">
          {description}
        </p>
        {status !== "ok" && (
          <button
            onClick={onAction}
            className="mt-3 inline-flex items-center gap-1 text-xs text-primary hover:underline"
          >
            <ExternalLink className="w-3 h-3" />
            {actionLabel}
          </button>
        )}
      </div>
    </div>
  );
}
