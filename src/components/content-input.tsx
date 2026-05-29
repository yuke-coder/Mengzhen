"use client";

import { useState, useCallback, useRef, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Upload, FileText, X, Check, Globe, Loader2, Sparkles, File, Music2 } from "lucide-react";
import { cn } from "@/lib/utils";
import { useContentPreprocessor } from "@/hooks/use-content-preprocessor";

const MAX_CHARS = 10000;

// 清理显示文本
function cleanDisplayText(text: string): string {
    return text
        .replace(/\r\n/g, '\n')
        .replace(/\r/g, '\n')
        .replace(/\t/g, '  ')
        .replace(/ +\n/g, '\n')
        .replace(/\n{3,}/g, '\n\n');
}

interface ContentInputProps {
    onSubmit: (content: string) => void;
    loading?: boolean;
    error?: string | null;
    initialContent?: string;
    onContentChange?: (content: string) => void;
    hasAudio?: boolean;
}

interface Ripple {
    x: number;
    y: number;
    id: number;
    ring: number;
    delay: number;
}

export function ContentInput({
    onSubmit,
    loading = false,
    error = null,
    initialContent = "",
    onContentChange,
    hasAudio = false
}: ContentInputProps) {
    const [mode, setMode] = useState<"text" | "url" | "file">("text");
    const [content, setContent] = useState(initialContent);
    const [urlInput, setUrlInput] = useState("");
    const [fetchingUrl, setFetchingUrl] = useState(false);
    const [fetchedTitle, setFetchedTitle] = useState<string | null>(null);
    const [dragOver, setDragOver] = useState(false);
    const [fileName, setFileName] = useState<string | null>(null);
    const [fileContent, setFileContent] = useState<string>("");
    const [showProcessed, setShowProcessed] = useState(false);
    const [ripples, setRipples] = useState<Ripple[]>([]);
    const rippleIdRef = useRef(0);
    
    // 预处理hook
    const {
        isProcessing,
        paragraphs,
        currentPhase,
        stats,
        isComplete,
        processContent,
        reset: resetPreprocess
    } = useContentPreprocessor();
    
    const fileInputRef = useRef<HTMLInputElement>(null);
    
    // 内容变化时通知父组件并触发预处理
    useEffect(() => {
        if (onContentChange) {
            const finalContent = fileContent || content;
            onContentChange(finalContent);
        }
    }, [content, fileContent, onContentChange]);
    
    // 内容变化时自动触发预处理
    useEffect(() => {
        const timer = setTimeout(() => {
            if (content.trim().length >= 50 && !isProcessing && !isComplete) {
                processContent(content);
                setShowProcessed(true);
            }
        }, 500);
        
        return () => clearTimeout(timer);
    }, [content, isProcessing, isComplete, processContent]);
    
    const clearContent = useCallback(() => {
        setContent("");
        setFetchedTitle(null);
        setFileName(null);
        setFileContent("");
        resetPreprocess();
        setShowProcessed(false);
    }, [resetPreprocess]);

    const handleSubmit = useCallback(() => {
        const submitContent = fileContent || content;
        if (submitContent.trim()) {
            onSubmit(submitContent.trim());
        }
    }, [content, fileContent, onSubmit]);

    // 涟漪效果处理函数
    const handleRipple = useCallback((e: React.MouseEvent<HTMLButtonElement>) => {
        const button = e.currentTarget;
        const rect = button.getBoundingClientRect();
        const x = e.clientX - rect.left;
        const y = e.clientY - rect.top;
        const id = rippleIdRef.current++;
        
        // 生成多层涟漪效果
        const newRipples = [
            { x, y, id: id, ring: 1, delay: 0 },
            { x, y, id: id + 0.1, ring: 2, delay: 50 },
            { x, y, id: id + 0.2, ring: 3, delay: 100 },
        ];
        
        setRipples(prev => [...prev, ...newRipples]);
        
        // 动画结束后移除涟漪
        setTimeout(() => {
            setRipples(prev => prev.filter(r => !newRipples.some(nr => nr.id === r.id)));
        }, 1200);
    }, []);

    const handleFetchUrl = useCallback(async () => {
        if (!urlInput.trim()) return;

        setFetchingUrl(true);
        setFetchedTitle(null);

        try {
            const response = await fetch(`/api/fetch-url?url=${encodeURIComponent(urlInput)}`);
            const data = await response.json();

            if (data.success && data.content) {
                setContent(data.content);
                if (data.title) {
                    setFetchedTitle(data.title);
                }
            } else {
                alert(data.error || "无法获取网页内容");
            }
        } catch (err) {
            console.error("Fetch URL error:", err);
            alert("获取网页内容失败");
        } finally {
            setFetchingUrl(false);
        }
    }, [urlInput]);

    const handleFileSelect = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (!file) return;

        if (file.size > 5 * 1024 * 1024) {
            alert("文件大小超过 5MB 限制");
            return;
        }

        const reader = new FileReader();
        reader.onload = (event) => {
            const text = event.target?.result as string;
            setFileContent(text);
            setFileName(file.name);
        };
        reader.onerror = () => {
            alert("读取文件失败");
        };
        reader.readAsText(file);
    }, []);

    const handleDrop = useCallback((e: React.DragEvent) => {
        e.preventDefault();
        setDragOver(false);

        const file = e.dataTransfer.files[0];
        if (!file) return;

        if (file.size > 5 * 1024 * 1024) {
            alert("文件大小超过 5MB 限制");
            return;
        }

        const validTypes = [".txt", ".md", ".doc", ".docx"];
        const ext = "." + file.name.split(".").pop()?.toLowerCase();
        if (!validTypes.includes(ext)) {
            alert("不支持的文件格式，请上传 .txt, .md, .doc 或 .docx 文件");
            return;
        }

        const reader = new FileReader();
        reader.onload = (event) => {
            const text = event.target?.result as string;
            setFileContent(text);
            setFileName(file.name);
        };
        reader.onerror = () => {
            alert("读取文件失败");
        };
        reader.readAsText(file);
    }, []);

    const displayContent = fileContent || content;

    return (
        <div className="space-y-6">
            {/* 模式切换 */}
            <div className="flex gap-2">
                <Button
                    variant={mode === "text" ? "default" : "outline"}
                    onClick={() => setMode("text")}
                    className={cn(
                        "flex-1",
                        mode === "text" ? "bg-primary text-primary-foreground hover:bg-primary/90" : "border-border"
                    )}>
                    <FileText className="w-4 h-4 mr-2" />文本输入
                </Button>
                <Button
                    variant={mode === "url" ? "default" : "outline"}
                    onClick={() => setMode("url")}
                    className={cn(
                        "flex-1",
                        mode === "url" ? "bg-primary text-primary-foreground hover:bg-primary/90" : "border-border"
                    )}>
                    <Globe className="w-4 h-4 mr-2" />网页链接
                </Button>
                <Button
                    variant={mode === "file" ? "default" : "outline"}
                    onClick={() => setMode("file")}
                    className={cn(
                        "flex-1",
                        mode === "file" ? "bg-primary text-primary-foreground hover:bg-primary/90" : "border-border"
                    )}>
                    <Upload className="w-4 h-4 mr-2" />上传文件
                </Button>
            </div>

            {/* 文本输入模式 */}
            {mode === "text" && (
                <div className="space-y-4">
                    <div className="flex justify-between items-center">
                        <Label htmlFor="content" className="text-foreground font-medium">
                            文章内容
                        </Label>
                        <span
                            className={cn(
                                "text-xs font-medium",
                                content.length > MAX_CHARS ? "text-destructive" : content.length > MAX_CHARS * 0.9 ? "text-yellow-500" : "text-muted-foreground"
                            )}>
                            {content.length.toLocaleString()}/{MAX_CHARS.toLocaleString()}字
                        </span>
                    </div>
                    <Textarea
                        id="content"
                        value={content}
                        onChange={e => {
                            const raw = e.target.value;
                            const cleaned = cleanDisplayText(raw);
                            setContent(cleaned);
                        }}
                        onKeyDown={e => {
                            if (e.key === "Enter" && !e.shiftKey) {
                                e.preventDefault();
                                if (displayContent.trim()) {
                                    handleSubmit();
                                }
                            }
                        }}
                        placeholder="在此粘贴或输入文章内容...\n\n支持：\n• 新闻报道\n• 学术论文\n• 产品介绍\n• 教程文档\n• 任何结构化文章内容\n\n提示：按 Enter 键开始生成"
                        className="min-h-[180px] bg-card border-border text-foreground resize-none focus:ring-2 focus:ring-primary/20"
                    />
                    {content && (
                        <Button
                            variant="ghost"
                            size="sm"
                            onClick={clearContent}
                            className="text-muted-foreground hover:text-destructive">
                            <X className="w-4 h-4 mr-1" />清除内容
                        </Button>
                    )}
                </div>
            )}

            {/* URL输入模式 */}
            {mode === "url" && (
                <div className="space-y-3">
                    <Label className="text-foreground font-medium">网页链接</Label>
                    <div className="flex gap-2">
                        <Input
                            value={urlInput}
                            onChange={e => setUrlInput(e.target.value)}
                            onKeyDown={e => {
                                if (e.key === "Enter" && !fetchingUrl) {
                                    e.preventDefault();
                                    handleFetchUrl();
                                }
                            }}
                            placeholder="输入网页 URL，如 https://example.com/article"
                            className="flex-1 bg-card border-border text-foreground"
                            disabled={fetchingUrl} />
                        <Button
                            onClick={handleFetchUrl}
                            disabled={fetchingUrl || !urlInput.trim()}
                            variant="default"
                            className="bg-primary hover:bg-primary/90 text-primary-foreground px-5 min-w-[100px]">
                            {fetchingUrl ? (
                                <><Loader2 className="w-4 h-4 mr-1.5 animate-spin" />提取中</>
                            ) : (
                                <><Globe className="w-4 h-4 mr-1.5" />提取内容</>
                            )}
                        </Button>
                    </div>
                    <p className="text-xs text-muted-foreground">
                        支持新闻、博客、文档等公开网页，自动识别并提取正文内容
                    </p>

                    {content && mode === "url" && (
                        <div className="space-y-2">
                            <div className="flex justify-between items-center">
                                <Label className="text-muted-foreground">提取结果</Label>
                                <div className="flex items-center gap-2">
                                    {fetchedTitle && (
                                        <span className="text-[10px] text-primary font-mono px-2 py-0.5 rounded-full bg-primary/10 truncate max-w-[180px]">
                                            {fetchedTitle}
                                        </span>
                                    )}
                                    <span className={cn(
                                        "text-xs font-medium",
                                        content.length > MAX_CHARS ? "text-destructive" : "text-muted-foreground"
                                    )}>
                                        {content.length.toLocaleString()}/{MAX_CHARS.toLocaleString()}字
                                    </span>
                                </div>
                            </div>
                            <div className="p-4 rounded-lg bg-card border border-border max-h-[220px] overflow-y-auto">
                                <p className="text-sm text-muted-foreground whitespace-pre-wrap leading-relaxed">
                                    {cleanDisplayText(content).substring(0, 800)}
                                    {content.length > 800 && "\n...(已截断预览)"}
                                </p>
                            </div>
                            <Button
                                variant="ghost"
                                size="sm"
                                onClick={clearContent}
                                className="text-muted-foreground hover:text-destructive">
                                <X className="w-4 h-4 mr-1" />清除内容
                            </Button>
                        </div>
                    )}
                </div>
            )}

            {/* 文件上传模式 */}
            {mode === "file" && (
                <div className="space-y-3">
                    <Label className="text-foreground font-medium">上传文件</Label>
                    <div
                        onDragOver={e => {
                            e.preventDefault();
                            setDragOver(true);
                        }}
                        onDragLeave={() => setDragOver(false)}
                        onDrop={handleDrop}
                        className={cn(
                            "relative border-2 border-dashed rounded-lg p-8 text-center transition-all cursor-pointer",
                            dragOver ? "border-primary bg-primary/5" : "border-border hover:border-primary/50",
                            fileName && "border-primary bg-primary/3"
                        )}>
                        <input
                            ref={fileInputRef}
                            type="file"
                            accept=".txt,.md,.doc,.docx"
                            onChange={handleFileSelect}
                            className="absolute inset-0 w-full h-full opacity-0 cursor-pointer" />
                        {fileName ? (
                            <div className="space-y-2">
                                <div className="flex items-center justify-center gap-2 text-primary">
                                    <Check className="w-5 h-5" />
                                    <span className="font-medium">{fileName}</span>
                                </div>
                                <p className={cn(
                                    "text-xs font-medium",
                                    fileContent.length > MAX_CHARS ? "text-destructive" : fileContent.length > MAX_CHARS * 0.9 ? "text-yellow-500" : "text-muted-foreground"
                                )}>
                                    {fileContent.length.toLocaleString()}/{MAX_CHARS.toLocaleString()}字
                                </p>
                            </div>
                        ) : (
                            <div className="space-y-2">
                                <Upload className="w-10 h-10 mx-auto text-muted-foreground" />
                                <div>
                                    <p className="text-foreground">拖拽文件到此处或点击上传</p>
                                    <p className="text-xs text-muted-foreground mt-1">支持 .txt, .md, .doc, .docx 格式，最大 5MB</p>
                                </div>
                            </div>
                        )}
                    </div>

                    {fileContent && (
                        <div className="space-y-2">
                            <div className="flex justify-between items-center">
                                <Label className="text-muted-foreground">内容预览</Label>
                                <Button
                                    variant="ghost"
                                    size="sm"
                                    onClick={clearContent}
                                    className="text-muted-foreground hover:text-destructive">
                                    <X className="w-4 h-4 mr-1" />移除文件
                                </Button>
                            </div>
                            <div className="p-4 rounded-lg bg-card border border-border max-h-[200px] overflow-y-auto">
                                <p className="text-sm text-muted-foreground whitespace-pre-wrap">
                                    {fileContent.substring(0, 500)}
                                    {fileContent.length > 500 && "..."}
                                </p>
                            </div>
                        </div>
                    )}
                </div>
            )}

            {/* 预处理结果显示区域 */}
            {showProcessed && displayContent.trim().length >= 50 && (
                <div className="space-y-3 p-4 rounded-xl bg-gradient-to-br from-[var(--card)] to-[var(--background)] border border-border/50">
                    {/* 预处理进度 */}
                    <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                            <div className={cn(
                                "w-2 h-2 rounded-full",
                                isProcessing ? "bg-yellow-500 animate-pulse" : isComplete ? "bg-green-500" : "bg-gray-400"
                            )} />
                            <span className="text-sm font-medium text-foreground">
                                {isProcessing ? "正在预处理..." : isComplete ? "预处理完成" : "等待处理"}
                            </span>
                        </div>
                        
                        {/* 阶段指示器 */}
                        {currentPhase && (
                            <div className="flex items-center gap-1 text-xs text-muted-foreground">
                                {currentPhase.phase === "normalize" && (
                                    <span className={cn(currentPhase.done ? "text-green-500" : "")}>
                                        ✓ 规范化
                                    </span>
                                )}
                                {currentPhase.phase === "paragraphs" && (
                                    <>
                                        <span className="text-green-500">✓</span>
                                        <span className={cn(currentPhase.done ? "text-green-500" : "")}>
                                            提取段落
                                        </span>
                                    </>
                                )}
                                {currentPhase.phase === "structure" && (
                                    <>
                                        <span className="text-green-500">✓✓</span>
                                        <span className={cn(currentPhase.done ? "text-green-500" : "")}>
                                            分析结构
                                        </span>
                                    </>
                                )}
                                {currentPhase.phase === "streaming" && (
                                    <>
                                        <span className="text-green-500">✓✓✓</span>
                                        <span className={cn(currentPhase.done ? "text-green-500" : "")}>
                                            显示结果
                                        </span>
                                    </>
                                )}
                            </div>
                        )}
                    </div>

                    {/* 段落流式显示 */}
                    {paragraphs.length > 0 && (
                        <div className="space-y-2 max-h-[200px] overflow-y-auto">
                            {paragraphs.map((p, idx) => (
                                <div key={idx} className="flex gap-2">
                                    <span className="text-[var(--brand-start)] text-xs font-mono mt-0.5">
                                        {String(idx + 1).padStart(2, "0")}
                                    </span>
                                    <p className="text-sm text-foreground/80 whitespace-pre-wrap">
                                        {p}
                                    </p>
                                </div>
                            ))}
                            {isProcessing && !isComplete && (
                                <div className="flex gap-2">
                                    <span className="text-[var(--brand-start)] text-xs font-mono mt-0.5">
                                        {String(paragraphs.length + 1).padStart(2, "0")}
                                    </span>
                                    <span className="text-sm text-foreground/40 animate-pulse">
                                        输入中...
                                    </span>
                                </div>
                            )}
                        </div>
                    )}

                    {/* 统计信息 */}
                    {stats && (
                        <div className="flex items-center gap-4 pt-2 border-t border-border/50">
                            <div className="flex items-center gap-1.5">
                                <span className="text-xs text-muted-foreground">字符</span>
                                <span className="text-sm font-semibold text-[var(--brand-start)]">
                                    {stats.charCount.toLocaleString()}
                                </span>
                            </div>
                            <div className="flex items-center gap-1.5">
                                <span className="text-xs text-muted-foreground">段落</span>
                                <span className="text-sm font-semibold text-[var(--brand-start)]">
                                    {stats.paragraphCount}
                                </span>
                            </div>
                            <div className="flex items-center gap-1.5">
                                <span className="text-xs text-muted-foreground">行数</span>
                                <span className="text-sm font-semibold text-[var(--brand-start)]">
                                    {stats.lineCount}
                                </span>
                            </div>
                        </div>
                    )}
                </div>
            )}

            {/* 生成按钮 */}
            <div className="space-y-3">
                <Button
                    onClick={(e) => {
                        if (!loading && displayContent.trim()) {
                            handleRipple(e);
                            handleSubmit();
                        }
                    }}
                    disabled={loading || !displayContent.trim()}
                    className={cn(
                        "ripple-btn relative w-full h-14 rounded-xl font-semibold text-base overflow-hidden group",
                        "transition-all duration-300 ease-out",
                        "bg-gradient-to-r from-[var(--brand-start)] to-[var(--brand-end)]",
                        "hover:shadow-lg hover:shadow-[var(--brand-start)]/30",
                        "active:scale-[0.98] active:shadow-md",
                        "disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:scale-100 disabled:hover:shadow-none",
                        "text-white"
                    )}
                >
                    {/* 从左向右填充的hover效果 */}
                    <span className="sweep-fill absolute inset-0 bg-[#CC28FB]" />
                    {/* 涟漪效果容器 */}
                    <span className="ripple-container">
                        {ripples.map(ripple => (
                            <span
                                key={ripple.id}
                                className={`ripple-wave ring-${ripple.ring}`}
                                style={{
                                    left: ripple.x,
                                    top: ripple.y,
                                    animationDelay: `${ripple.delay}ms`,
                                }}
                            />
                        ))}
                    </span>
                    {/* 高光扫过效果 */}
                    <span className="absolute -inset-full top-0 block h-full w-1/2 -skew-x-12 bg-gradient-to-r from-transparent via-white/30 to-transparent translate-x-[-200%] group-hover:translate-x-[400%] transition-transform duration-700 ease-out" />
                    
                    <span className="relative flex items-center justify-center gap-3">
                        {loading ? (
                            <>
                                <svg className="w-5 h-5 animate-spin" viewBox="0 0 24 24" fill="none">
                                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                                </svg>
                                <span>AI 分析中...</span>
                            </>
                        ) : (
                            <>
                                <Sparkles className="w-5 h-5" />
                                <span>一键灵图</span>
                                <kbd className="ml-2 px-2 py-0.5 text-xs font-normal bg-white/20 rounded-md opacity-70 group-hover:opacity-100 transition-opacity">
                                    Enter
                                </kbd>
                            </>
                        )}
                    </span>
                </Button>
                
                {!displayContent.trim() && (
                    <p className="text-center text-xs text-muted-foreground">
                        请先输入或上传文章内容
                    </p>
                )}
                
                {displayContent.trim() && !loading && (
                    <p className="text-center text-xs text-muted-foreground/60 animate-pulse">
                        按 Enter 键开始生成
                    </p>
                )}
            </div>

            {/* 错误提示 */}
            {error && (
                <div className="mt-3 flex items-center justify-center gap-2 text-sm text-[var(--brand-end)] animate-pulse">
                    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                    <span style={{ fontFamily: "DOUYINSANSBOLD-GB", fontSize: "14px" }}>{error}</span>
                </div>
            )}
        </div>
    );
}
