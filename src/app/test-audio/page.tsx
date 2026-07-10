"use client";
import { useState, useCallback } from "react";
import EnhancedTaskScheduler from "@/lib/background-scheduler";
import AudioDebug from "@/lib/audio-debug";

export default function TestAudioPage() {
  const [status, setStatus] = useState("就绪");
  const [audioState, setAudioState] = useState("未初始化");

  const testAudio = useCallback(async () => {
    try {
      setStatus("初始化音频系统...");
      const scheduler = EnhancedTaskScheduler.getInstance();
      await scheduler.initialize();

      setAudioState(scheduler.getAudioState() || "unknown");
      setStatus("音频系统初始化成功");

      // 测试播放一个音频
      setStatus("正在播放测试音频...");

      // 创建一个测试任务
      const testTask = {
        id: "test-task-1",
        name: "测试音频",
        startTime: {
          year: 2026,
          month: 7,
          day: 9,
          hour: 0,
          minute: 0,
          second: 0
        },
        audios: [
          {
            url: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            name: "测试音乐"
          }
        ],
        volume: 50,
        enableFade: true,
        fadeInDuration: 1,
        fadeOutDuration: 1,
        playDurationMinutes: 1,
        status: "pending" as const,
        playMode: "default",
        isLoop: false
      };

      scheduler.addTask(testTask);
      setStatus("测试任务已添加，正在播放...");

    } catch (error) {
      console.error("测试失败:", error);
      setStatus(`错误: ${(error as Error).message}`);
      AudioDebug.error("测试失败:", error);
    }
  }, []);

  const unlockAudio = useCallback(async () => {
    try {
      setStatus("正在解锁音频...");
      const scheduler = EnhancedTaskScheduler.getInstance();
      await scheduler.initializeAudioContext();
      setStatus("音频已解锁");
      setAudioState(scheduler.getAudioState() || "unknown");
    } catch (error) {
      console.error("解锁失败:", error);
      setStatus(`解锁失败: ${(error as Error).message}`);
    }
  }, []);

  const stopAll = useCallback(() => {
    const scheduler = EnhancedTaskScheduler.getInstance();
    scheduler.stopAllAudio();
    setStatus("所有音频已停止");
  }, []);

  return (
    <div className="min-h-screen p-8">
      <h1 className="text-2xl font-bold mb-6">音频播放测试</h1>

      <div className="space-y-4 mb-8">
        <button
          onClick={unlockAudio}
          className="px-4 py-2 bg-blue-500 text-white rounded"
        >
          手动解锁音频
        </button>

        <button
          onClick={testAudio}
          className="px-4 py-2 bg-green-500 text-white rounded ml-2"
        >
          测试定时音频播放
        </button>

        <button
          onClick={stopAll}
          className="px-4 py-2 bg-red-500 text-white rounded ml-2"
        >
          停止所有音频
        </button>
      </div>

      <div className="p-4 bg-gray-100 rounded">
        <p>当前状态: {status}</p>
        <p>音频状态: {audioState}</p>
      </div>

      <div className="mt-8">
        <h2 className="text-xl font-semibold mb-2">调试信息</h2>
        <button
          onClick={() => {
            AudioDebug.checkPermissions();
            AudioDebug.checkAudioContextState().then(state => {
              console.log("当前音频上下文状态:", state);
            });
          }}
          className="px-3 py-1 bg-gray-200 rounded text-sm"
        >
          运行调试检查
        </button>
      </div>
    </div>
  );
}