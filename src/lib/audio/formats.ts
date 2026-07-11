export const AUDIO_EXTENSIONS = [".mp3", ".wav", ".ogg", ".m4a", ".flac", ".aac"];
// 安卓优化配置：只使用文件扩展名，确保文件选择器正确显示，不显示录音选项
export const AUDIO_ACCEPT = AUDIO_EXTENSIONS.join(",");
