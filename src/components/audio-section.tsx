"use client";
import React, { useState, useCallback, useRef, useEffect } from "react";
import { useAuth } from "@/lib/auth-context";
import { saveAudioBlob, deleteAudioBlob } from "@/lib/audio-db";
import { TaskAudio } from "@/lib/task-types";
import { cn } from "@/lib/utils";
import { AUDIO_ACCEPT, AUDIO_EXTENSIONS } from "@/lib/audio-formats";
import { MAX_FILES, formatFileSize, formatDuration, type AudioItemBase } from "@/lib/audio-utils";
import { Spinner } from "@/components/ui/spinner";
import { Upload, Music2, X, Play, Pause, Trash2, CheckCircle2, AlertCircle, Volume1, VolumeX } from "lucide-react";

interface AudioSectionItem extends AudioItemBase { file: File; }

interface AudioSectionProps {
    initialAudios?: TaskAudio[];
    onAudiosChange?: (audios: TaskAudio[]) => void;
    onVolumeChange?: (volume: number) => void;
    initialVolume?: number;
}

let audioSectionIdCounter = 0;

function useClientOnly() {
    const [mounted, setMounted] = useState(false);
    useEffect(() => setMounted(true), []);
    return mounted;
}

export function AudioSection({ initialAudios = [], onAudiosChange, onVolumeChange, initialVolume = 50 }: AudioSectionProps) {
    const { user } = useAuth();
    const audioRefs = useRef<Record<string, HTMLAudioElement>>({});
    const isClearingRef = useRef(false);
    const intervalRef = useRef<NodeJS.Timeout | null>(null);
    const onAudiosChangeRef = useRef(onAudiosChange);
    const initialAudiosRef = useRef<{ [key: string]: { size: number } }>({});
    const prevAudiosJsonRef = useRef("");

    onAudiosChangeRef.current = onAudiosChange;

    const [audios, setAudios] = useState<AudioSectionItem[]>(() =>
        initialAudios.map(a => {
            initialAudiosRef.current[a.id] = { size: a.size || 0 };
            let url: string | undefined;
            if (a.serverUrl) url = a.serverUrl;
            else if (a.fileKey) url = `/api/audio/proxy?key=${encodeURIComponent(a.fileKey)}`;
            return {
                id: a.id,
                name: a.name,
                file: new File([], a.name, { type: 'audio/mpeg' }),
                url,
                duration: a.duration,
                fileKey: a.fileKey,
                serverUrl: a.serverUrl,
                dbKey: a.dbKey,
            };
        })
    );

    const [playingId, setPlayingId] = useState<string | null>(null);
    const [currentTimes, setCurrentTimes] = useState<Record<string, number>>({});
    const [volume, setVolume] = useState(initialVolume);
    const [dragOver, setDragOver] = useState(false);
    const [uploadError, setUploadError] = useState<string | null>(null);
    const [showGuestTip, setShowGuestTip] = useState(false);
    const [showClearConfirm, setShowClearConfirm] = useState(false);
    const [portalReady, setPortalReady] = useState(false);

    useEffect(() => setPortalReady(true), []);

    useEffect(() => {
        const currentJson = JSON.stringify(initialAudios);
        if (currentJson !== prevAudiosJsonRef.current) {
            prevAudiosJsonRef.current = currentJson;
            const newAudios = initialAudios.map(a => {
                initialAudiosRef.current[a.id] = { size: a.size || 0 };
                let url: string | undefined;
                if (a.serverUrl) url = a.serverUrl;
                else if (a.fileKey) url = `/api/audio/proxy?key=${encodeURIComponent(a.fileKey)}`;
                return {
                    id: a.id,
                    name: a.name,
                    file: new File([], a.name, { type: 'audio/mpeg' }),
                    url,
                    duration: a.duration,
                    fileKey: a.fileKey,
                    serverUrl: a.serverUrl,
                    dbKey: a.dbKey,
                };
            });
            setAudios(newAudios);
        }
    }, [initialAudios]);

    const VolumeIcon = volume === 0 ? VolumeX : Volume1;

    useEffect(() => {
        if (isClearingRef.current) return;
        const taskAudios: TaskAudio[] = audios.map(a => {
            let size = a.file.size;
            if (size === 0 && initialAudiosRef.current[a.id]) size = initialAudiosRef.current[a.id].size;
            return { id: a.id, name: a.name, duration: a.duration, size, fileKey: a.fileKey, serverUrl: a.serverUrl, dbKey: a.dbKey };
        });
        onAudiosChangeRef.current?.(taskAudios);
    }, [audios]);

    const validateFile = useCallback((file: File): string | null => {
        const ext = "." + file.name.split(".").pop()?.toLowerCase();
        const typeOk = file.type.startsWith('audio/') || file.type === '';
        const extOk = AUDIO_EXTENSIONS.includes(ext);
        if (!typeOk && !extOk) return `不支持的音频格式，请上传 ${AUDIO_EXTENSIONS.join(", ")} 文件`;
        if (audios.some(a => a.file.name === file.name)) return `文件「${file.name}」已存在`;
        return null;
    }, [audios]);

    const autoUploadToServer = useCallback(async (id: string, file: File) => {
        if (!user) return;
        try {
            setAudios(prev => prev.map(a => a.id === id ? { ...a, uploading: true, uploadProgress: 0, uploadError: null } : a));
            const formData = new FormData();
            formData.append("audio", file);
            const res = await fetch("/api/audio/upload?save_to_files=true", { method: "POST", body: formData });
            const data = await res.json();
            if (!isClearingRef.current) {
                if (data.success) {
                    setAudios(prev => prev.map(a => a.id === id ? { ...a, url: data.audio_url, serverUrl: data.audio_url, fileKey: data.file_key, uploading: false, uploadProgress: 100 } : a));
                } else {
                    setAudios(prev => prev.map(a => a.id === id ? { ...a, uploading: false, uploadError: data.error || "上传失败" } : a));
                }
            }
        } catch {
            if (!isClearingRef.current) {
                setAudios(prev => prev.map(a => a.id === id ? { ...a, uploading: false, uploadError: "上传失败" } : a));
            }
        }
    }, [user]);

    const processFiles = useCallback(async (files: FileList | File[]) => {
        setUploadError(null);
        const fileArray = Array.from(files).slice(0, MAX_FILES - audios.length);
        const newAudios: AudioSectionItem[] = [];
        for (const file of fileArray) {
            const error = validateFile(file);
            if (error) { setUploadError(error); continue; }
            const url = URL.createObjectURL(file);
            const id = `audio_${++audioSectionIdCounter}_${Date.now()}`;
            let dbKey: string | undefined;
            try { await saveAudioBlob(id, file); dbKey = id; } catch { }
            newAudios.push({ id, file, name: file.name, url, duration: 0, dbKey });
            if (user) autoUploadToServer(id, file);
        }
        if (newAudios.length > 0) setAudios(prev => [...prev, ...newAudios]);
    }, [audios, validateFile, user, autoUploadToServer]);

    const handleRemove = useCallback((id: string) => {
        const audio = audios.find(a => a.id === id);
        if (audio) {
            const el = audioRefs.current[id];
            if (el) { el.pause(); el.removeAttribute('src'); delete audioRefs.current[id]; }
            if (audio.url?.startsWith("blob:")) URL.revokeObjectURL(audio.url);
            if (audio.dbKey) deleteAudioBlob(audio.dbKey).catch(() => { });
        }
        setAudios(prev => prev.filter(a => a.id !== id));
        setCurrentTimes(prev => { const next = { ...prev }; delete next[id]; return next; });
        if (playingId === id) setPlayingId(null);
    }, [audios, playingId]);

    const togglePlay = useCallback((id: string) => {
        const el = audioRefs.current[id];
        if (!el) return;
        Object.values(audioRefs.current).forEach(a => { if (a && !a.paused) a.pause(); });
        if (playingId === id) { el.pause(); setPlayingId(null); }
        else { el.currentTime = 0; el.volume = volume / 100; el.play().catch(() => { }); setPlayingId(id); }
    }, [playingId, volume]);

    useEffect(() => {
        Object.values(audioRefs.current).forEach(el => { if (el) el.volume = volume / 100; });
        onVolumeChange?.(volume);
    }, [volume, onVolumeChange]);

    useEffect(() => {
        const handleTimeUpdate = () => {
            if (!isClearingRef.current) {
                setCurrentTimes(prev => {
                    const next = { ...prev };
                    Object.entries(audioRefs.current).forEach(([id, el]) => { if (el) next[id] = el.currentTime; });
                    return next;
                });
            }
        };
        const handleEnded = () => { if (!isClearingRef.current) setPlayingId(null); };
        intervalRef.current = setInterval(handleTimeUpdate, 250);
        const cleanups: (() => void)[] = [];
        Object.entries(audioRefs.current).forEach(([id, el]) => {
            el.addEventListener("ended", handleEnded);
            cleanups.push(() => el.removeEventListener("ended", handleEnded));
        });
        return () => {
            if (intervalRef.current) clearInterval(intervalRef.current);
            cleanups.forEach(c => c());
        };
    }, []);

    useEffect(() => { isClearingRef.current = false; }, []);

    const handleUploadSingle = useCallback(async (id: string) => {
        const audio = audios.find(a => a.id === id);
        if (!audio || !user) return;
        await autoUploadToServer(id, audio.file);
    }, [audios, user, autoUploadToServer]);

    const allUploaded = audios.length > 0 && audios.every(a => a.serverUrl);
    const anyUploading = audios.some(a => a.uploading);

    const handleClearAll = useCallback(() => {
        isClearingRef.current = true;
        if (intervalRef.current) { clearInterval(intervalRef.current); intervalRef.current = null; }
        Object.entries(audioRefs.current).forEach(([id, el]) => { if (el) { try { el.pause(); el.src = ''; } catch { } delete audioRefs.current[id]; } });
        audios.forEach(a => { if (a.url?.startsWith("blob:")) URL.revokeObjectURL(a.url); if (a.dbKey) deleteAudioBlob(a.dbKey).catch(() => { }); });
        setAudios([]); setCurrentTimes({}); setPlayingId(null); setShowClearConfirm(false); isClearingRef.current = false;
    }, [audios]);

    const handleUploadAll = useCallback(async () => {
        const pending = audios.filter(a => !a.serverUrl && !a.uploading);
        for (const audio of pending) await handleUploadSingle(audio.id);
    }, [audios, handleUploadSingle]);

    return (
        <div className="space-y-4">
            <label
                htmlFor="audio-file-input-section"
                onDragOver={e => { e.preventDefault(); !dragOver && setDragOver(true); }}
                onDragLeave={() => setDragOver(false)}
                onDrop={e => { e.preventDefault(); setDragOver(false); processFiles(e.dataTransfer.files); }}
                className={cn("relative block p-6 transition-all duration-300 cursor-pointer rounded-2xl select-none touch-manipulation active:scale-[0.98]", dragOver && "scale-[1.02]")}
            >
                <input id="audio-file-input-section" type="file" multiple accept={AUDIO_ACCEPT} onChange={e => { e.stopPropagation(); if (e.target.files) processFiles(e.target.files); e.currentTarget.value = ""; }} className="sr-only" tabIndex={-1} />
                <div className="flex flex-col items-center gap-2.5 pointer-events-none">
                    <div className={cn("p-2.5 rounded-full bg-[var(--brand-glow)]/10 transition-transform duration-300", dragOver && "scale-110")}><Upload className="w-5 h-5 text-[var(--brand-glow)]" /></div>
                    <div className="text-center">
                        <p className="text-sm font-medium text-foreground">{dragOver ? "松开以上传" : "点击或拖拽音频文件到此处"}</p>
                        {audios.length > 0 && <p className="text-xs text-[var(--brand-start)] font-medium">已添加 {audios.length}/{MAX_FILES} 个音频</p>}
                    </div>
                </div>
            </label>

            {showGuestTip && (
                <div className="flex items-center justify-between gap-3 p-3 rounded-xl bg-amber-950/20 border border-amber-900/30 animate-in fade-in slide-in-from-top-2 duration-200">
                    <p className="text-sm text-amber-400">请先登录后再上传音频文件</p>
                    <button onClick={() => setShowGuestTip(false)} className="text-amber-400/60 hover:text-amber-400 transition-colors"><X className="w-4 h-4" /></button>
                </div>
            )}

            {uploadError && (
                <div className="flex items-center gap-2 p-3 rounded-xl bg-red-950/20 border border-red-900/30">
                    <AlertCircle className="w-4 h-4 text-red-400 flex-shrink-0" />
                    <span className="text-sm text-red-400">{uploadError}</span>
                    <button onClick={() => setUploadError(null)} className="ml-auto text-red-400/60 hover:text-red-400"><X className="w-4 h-4" /></button>
                </div>
            )}

            {audios.length > 0 && (
                <div className="space-y-2">
                    <div className="flex items-center justify-between px-1">
                        <h3 className="text-sm font-medium text-foreground flex items-center gap-2"><Music2 className="w-4 h-4 text-[var(--brand-start)]" />音频列表（{audios.length}）</h3>
                        {!allUploaded && user && (
                            <button onClick={handleUploadAll} disabled={anyUploading} className="rounded-lg text-xs h-7 px-2.5 border border-border/60 bg-muted/30 text-muted-foreground hover:text-foreground hover:bg-muted/60 transition-all disabled:opacity-50">
                                {anyUploading ? <><Spinner size="sm" className="mr-1 h-3 w-3 inline" />上传中...</> : <><Upload className="w-3 h-3 mr-1 inline" />全部上传</>}
                            </button>
                        )}
                    </div>

                    <div className="space-y-2">
                        {audios.map(audio => {
                            const isPlaying = playingId === audio.id;
                            const currentTime = currentTimes[audio.id] || 0;
                            return (
                                <div key={audio.id} onClick={() => togglePlay(audio.id)} className="group/audio relative cursor-pointer transition-all duration-200">
                                    <div className="p-3 space-y-2">
                                        <div className="flex items-center gap-2.5">
                                            <button onClick={e => { e.stopPropagation(); togglePlay(audio.id); }} className="flex-shrink-0 w-9 h-9 sm:w-8 sm:h-8 rounded-lg bg-muted/80 border border-border/40 flex items-center justify-center text-foreground hover:text-[var(--brand-start)] hover:border-[var(--brand-start)]/30 active:scale-95 transition-all">
                                                {isPlaying ? <Pause className="w-3.5 h-3.5 sm:w-3 sm:h-3" fill="currentColor" /> : <Play className="w-3.5 h-3.5 sm:w-3 sm:h-3 ml-0.5" fill="currentColor" />}
                                            </button>
                                            <div className="flex-1 min-w-0">
                                                <p className={cn("font-medium text-sm truncate transition-colors", isPlaying ? "text-[var(--brand-start)]" : "text-foreground group-hover/audio:text-[var(--brand-start)]")}>{audio.name}</p>
                                                <div className="flex items-center gap-2 mt-0.5 text-[11px] text-muted-foreground">
                                                    <span className="flex items-center gap-1"><VolumeIcon className="w-3 h-3" />{formatFileSize(audio.file.size)}</span>
                                                    {audio.duration > 0 && <span className="flex items-center gap-1">{formatDuration(audio.duration)}</span>}
                                                    {audio.serverUrl && <span className="flex items-center gap-1 text-green-500"><CheckCircle2 className="w-3 h-3" />已上传</span>}
                                                    {audio.uploading && <span className="flex items-center gap-1 text-[var(--brand-start)]"><Spinner size="sm" className="h-3 w-3" />{audio.uploadProgress || 0}%</span>}
                                                </div>
                                            </div>
                                            <div className="flex items-center gap-1 flex-shrink-0 opacity-0 group-hover/audio:opacity-100 focus-within:opacity-100 transition-opacity sm:flex">
                                                {!audio.serverUrl && !audio.uploading && user && <button onClick={e => { e.stopPropagation(); handleUploadSingle(audio.id); }} className="w-7 h-7 rounded-md flex items-center justify-center text-muted-foreground hover:text-[var(--brand-start)] hover:bg-muted/60 transition-all"><Upload className="w-3 h-3" /></button>}
                                                <button onClick={e => { e.stopPropagation(); handleRemove(audio.id); }} className="w-7 h-7 rounded-md flex items-center justify-center text-muted-foreground hover:text-red-500 hover:bg-muted/60 transition-all"><Trash2 className="w-3 h-3" /></button>
                                            </div>
                                            <button onClick={e => { e.stopPropagation(); handleRemove(audio.id); }} className="sm:hidden flex-shrink-0 w-8 h-8 rounded-md flex items-center justify-center text-muted-foreground hover:text-red-500 hover:bg-muted/60 active:bg-muted transition-all"><Trash2 className="w-3.5 h-3.5" /></button>
                                        </div>
                                        {audio.duration > 0 && (
                                            <div className="space-y-1">
                                                <input type="range" min={0} max={audio.duration} value={currentTime} step={0.1} onChange={e => { e.stopPropagation(); const el = audioRefs.current[audio.id]; if (el) el.currentTime = parseFloat(e.target.value); }} onClick={e => e.stopPropagation()} className="w-full h-1.5 rounded-full appearance-none bg-border/40 cursor-pointer [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:w-4 [&::-webkit-slider-thumb]:h-4 [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:bg-[var(--brand-start)] [&::-webkit-slider-thumb]:shadow-md [&::-webkit-slider-thumb]:cursor-pointer [&::-webkit-slider-thumb]:active:scale-95 sm:[&::-webkit-slider-thumb]:w-3 sm:[&::-webkit-slider-thumb]:h-3 sm:[&::-webkit-slider-thumb]:hover:scale-125" style={{ background: `linear-gradient(to right, var(--brand-start) ${(currentTime / audio.duration) * 100}%, rgba(128,128,128,0.25) ${(currentTime / audio.duration) * 100}%)` }} />
                                                <div className="flex justify-between text-[10px] text-muted-foreground font-mono"><span>{formatDuration(currentTime)}</span><span>{formatDuration(audio.duration)}</span></div>
                                            </div>
                                        )}
                                        {audio.uploading && (
                                            <div className="space-y-1"><div className="w-full h-1.5 rounded-full bg-border/40 overflow-hidden"><div className="h-full rounded-full bg-gradient-to-r from-[var(--brand-start)] to-[var(--brand-end)] transition-all duration-200" style={{ width: `${audio.uploadProgress || 0}%` }} /></div></div>
                                        )}
                                        {audio.uploadError && (
                                            <div className="flex items-center gap-2 p-2 rounded-lg bg-red-100/60 dark:bg-red-950/15 border border-red-900/30 dark:border-red-900/20"><AlertCircle className="w-3 h-3 text-red-400 flex-shrink-0" /><span className="text-xs text-red-400">{audio.uploadError}</span></div>
                                        )}
                                    </div>
                                    <audio
                                        ref={el => { if (el) audioRefs.current[audio.id] = el; }}
                                        {...(audio.url ? { src: audio.url } : {})}
                                        preload="metadata"
                                        onLoadedMetadata={() => {
                                            const el = audioRefs.current[audio.id];
                                            if (el && audio.duration === 0 && el.duration) {
                                                setAudios(prev => prev.map(a => a.id === audio.id ? { ...a, duration: el.duration } : a));
                                            }
                                        }}
                                        className="hidden"
                                    />
                                </div>
                            );
                        })}
                    </div>

                    <div className="flex items-center justify-between pt-1 px-1">
                        <p className="text-xs text-muted-foreground">共 {audios.length} 个音频{allUploaded && user && " · 全部已上传"}</p>
                        {audios.length > 1 && <button onClick={() => setShowClearConfirm(true)} className="text-xs text-muted-foreground hover:text-red-500 transition-colors">清空全部</button>}
                    </div>
                </div>
            )}

            <div>
                <div className="p-4 space-y-2.5">
                    <div className="flex items-center justify-between"><label className="text-xs font-medium text-muted-foreground flex items-center gap-1.5"><VolumeIcon className="w-3.5 h-3.5" />音量控制</label><span className="text-sm font-mono font-semibold tabular-nums text-foreground">{volume}%</span></div>
                    <div className="relative pt-1 pb-1"><input type="range" min={0} max={100} value={volume} step={1} onInput={e => setVolume(parseInt((e.target as HTMLInputElement).value, 10))} className="w-full h-2.5 rounded-full appearance-none bg-border/30 cursor-pointer [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:w-6 [&::-webkit-slider-thumb]:h-6 [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:bg-white [&::-webkit-slider-thumb]:shadow-md [&::-webkit-slider-thumb]:cursor-pointer [&::-webkit-slider-thumb]:border-2 [&::-webkit-slider-thumb]:border-[var(--brand-start)] [&::-webkit-slider-thumb]:active:scale-95 sm:[&::-webkit-slider-thumb]:w-5 sm:[&::-webkit-slider-thumb]:h-5" style={{ background: `linear-gradient(to right, var(--brand-start) ${volume}%, rgba(128,128,128,0.2) ${volume}%)` }} /></div>
                </div>
            </div>

            {showClearConfirm && portalReady && (
                <div className="fixed inset-0 z-[200] flex items-center justify-center bg-black/50 backdrop-blur-sm" style={{ position: "fixed", top: 0, left: 0, right: 0, bottom: 0 }} onClick={() => setShowClearConfirm(false)}>
                    <div role="dialog" aria-modal="true" aria-labelledby="clear-confirm-title" aria-describedby="clear-confirm-desc" className="bg-background border border-border/60 rounded-2xl shadow-2xl p-6 max-w-sm w-[calc(100%-2rem)] space-y-4" onClick={e => e.stopPropagation()}>
                        <div className="flex flex-col items-center gap-3 text-center">
                            <div className="w-12 h-12 rounded-full bg-red-500/10 flex items-center justify-center"><Trash2 className="w-6 h-6 text-red-500" /></div>
                            <div><h3 id="clear-confirm-title" className="text-base font-semibold text-foreground">确认清空全部音频？</h3><p id="clear-confirm-desc" className="text-sm text-muted-foreground mt-1.5">此操作将删除所有已上传的音频文件，且无法恢复。</p></div>
                        </div>
                        <div className="flex gap-3">
                            <button onClick={() => setShowClearConfirm(false)} className="flex-1 h-10 rounded-xl border border-border/60 text-sm font-medium text-muted-foreground hover:text-foreground hover:bg-muted/80 active:bg-muted transition-all cursor-pointer">取消</button>
                            <button onClick={handleClearAll} className="flex-1 h-10 rounded-xl text-sm font-bold bg-red-500 text-white hover:bg-red-500/90 active:bg-red-500/80 transition-all cursor-pointer">确认清空</button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
