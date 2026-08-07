package com.mengzhen.app.bilibili

import android.content.Context
import android.media.MediaExtractor
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.ArrayDeque
import java.util.Locale

class BiliCacheScanner(private val context: Context) {
    suspend fun scanTree(
        treeUri: Uri,
        onProgress: (Int) -> Unit = {},
    ): List<BiliCacheItem> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw IllegalArgumentException("无法读取所选目录")
        val nodes = collectDocuments(root, onProgress)
        buildDocumentItems(nodes)
    }

    private fun collectDocuments(
        root: DocumentFile,
        onProgress: (Int) -> Unit,
    ): List<DocumentNode> {
        val result = mutableListOf<DocumentNode>()
        val queue = ArrayDeque<DirectoryNode>()
        queue.add(DirectoryNode(root, "", 0))
        var visited = 0

        while (queue.isNotEmpty() && visited < MAX_FILES) {
            val directory = queue.removeFirst()
            val children = runCatching { directory.file.listFiles() }.getOrDefault(emptyArray())
            for (child in children) {
                if (++visited > MAX_FILES) break
                val name = child.name.orEmpty()
                if (name.isBlank()) continue
                val relativePath = joinPath(directory.relativePath, name)
                if (child.isDirectory && directory.depth < MAX_DEPTH) {
                    queue.add(DirectoryNode(child, relativePath, directory.depth + 1))
                } else if (child.isFile) {
                    result += DocumentNode(
                        relativePath = relativePath,
                        parentPath = directory.relativePath,
                        name = name,
                        file = child,
                    )
                }
                if (visited % 200 == 0) onProgress(visited)
            }
        }
        onProgress(visited)
        return result
    }

    private fun buildDocumentItems(nodes: List<DocumentNode>): List<BiliCacheItem> {
        val metadataParents = nodes
            .filter { it.lowerName in METADATA_NAMES }
            .groupBy(DocumentNode::parentPath)
        val m4sNodes = nodes.filter {
            it.lowerName.endsWith(".m4s") && !it.lowerName.endsWith(".bdl")
        }
        val usedAudioUris = mutableSetOf<String>()
        val items = mutableListOf<BiliCacheItem>()

        for ((parentPath, metadataNodes) in metadataParents) {
            val descendants = m4sNodes.filter {
                isDescendant(parentPath, it.relativePath, maxExtraDepth = 3)
            }
            if (descendants.isEmpty()) continue
            val playUrlNode = nodes.firstOrNull {
                it.lowerName == ".playurl" &&
                    isDescendant(parentPath, it.relativePath, maxExtraDepth = 2)
            }
            val playUrlRaw = playUrlNode?.let(::readText)
            val playUrlAudio = BiliCacheMetadataParser.parsePlayUrl(playUrlRaw)
            val audioNode = chooseDocumentAudio(descendants, playUrlAudio) ?: continue
            val entryRaw = metadataNodes
                .firstOrNull { it.lowerName == "entry.json" }
                ?.let(::readText)
            val videoInfoRaw = metadataNodes
                .firstOrNull { it.lowerName == "videoinfo.json" }
                ?.let(::readText)
                ?: metadataNodes.firstOrNull { it.lowerName == ".videoinfo" }?.let(::readText)
            val fallbackId = parentPath.ifBlank { audioNode.relativePath }
            val metadata = BiliCacheMetadataParser.parse(
                entryJson = entryRaw,
                videoInfoJson = videoInfoRaw,
                fallbackId = fallbackId,
                fallbackTitle = parentPath.substringAfterLast('/').ifBlank { "B站缓存" },
            )
            val hint = matchingHint(audioNode.name, playUrlAudio)
            val coverNode = chooseCover(nodes, parentPath)
            val uri = audioNode.file.uri.toString()
            usedAudioUris += uri
            items += BiliCacheItem(
                id = metadata.sourceId,
                title = metadata.title,
                subtitle = metadata.subtitle,
                owner = metadata.owner,
                durationSeconds = metadata.durationSeconds,
                audioSize = audioNode.file.length(),
                mimeType = hint?.mimeType ?: "audio/mp4",
                codec = hint?.codec.orEmpty(),
                audioLocation = uri,
                coverLocation = coverNode?.file?.uri?.toString(),
                accessMode = BiliCacheAccessMode.DOCUMENT,
                completed = metadata.completed && audioNode.file.length() > 0,
            )
        }

        m4sNodes
            .filter { it.file.uri.toString() !in usedAudioUris }
            .filter(::isDocumentAudioOnly)
            .forEach { audio ->
                items += BiliCacheItem(
                    id = audio.relativePath,
                    title = audio.parentPath.substringAfterLast('/').ifBlank {
                        audio.name.substringBeforeLast('.')
                    },
                    audioSize = audio.file.length(),
                    audioLocation = audio.file.uri.toString(),
                    coverLocation = chooseCover(nodes, audio.parentPath)?.file?.uri?.toString(),
                    accessMode = BiliCacheAccessMode.DOCUMENT,
                    completed = audio.file.length() > 0,
                )
            }

        return items
            .distinctBy(BiliCacheItem::id)
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.displayTitle() })
    }

    private fun chooseDocumentAudio(
        candidates: List<DocumentNode>,
        hints: List<BiliPlayUrlAudio>,
    ): DocumentNode? {
        candidates.firstOrNull { it.lowerName == "audio.m4s" }?.let { return it }
        for (hint in hints) {
            candidates.firstOrNull {
                it.name.equals(hint.fileName, ignoreCase = true) ||
                    (hint.id.isNotBlank() && it.lowerName.endsWith("-${hint.id}.m4s"))
            }?.let { return it }
        }
        return candidates
            .sortedBy(DocumentNode::size)
            .firstOrNull(::isDocumentAudioOnly)
    }

    private fun isDocumentAudioOnly(node: DocumentNode): Boolean {
        val header = runCatching {
            context.contentResolver.openInputStream(node.file.uri)?.use(::readHeader)
        }.getOrNull() ?: return false
        val skip = BiliM4sHeader.bytesToSkip(header)
        if (skip < 0) return false
        val length = node.file.length()
        if (length <= skip) return false

        return runCatching {
            context.contentResolver.openFileDescriptor(node.file.uri, "r")?.use { descriptor ->
                val extractor = MediaExtractor()
                try {
                    extractor.setDataSource(descriptor.fileDescriptor, skip.toLong(), length - skip)
                    var hasAudio = false
                    var hasVideo = false
                    for (index in 0 until extractor.trackCount) {
                        val mime = extractor.getTrackFormat(index)
                            .getString(android.media.MediaFormat.KEY_MIME)
                            .orEmpty()
                        hasAudio = hasAudio || mime.startsWith("audio/")
                        hasVideo = hasVideo || mime.startsWith("video/")
                    }
                    hasAudio && !hasVideo
                } finally {
                    extractor.release()
                }
            } ?: false
        }.getOrDefault(false)
    }

    private fun readText(node: DocumentNode): String? = runCatching {
        context.contentResolver.openInputStream(node.file.uri)?.use(::readLimitedText)
    }.getOrNull()

    private fun chooseCover(
        nodes: List<DocumentNode>,
        parentPath: String,
    ): DocumentNode? {
        val candidates = nodes.filter {
            it.lowerName in COVER_NAMES &&
                isDescendant(parentPath, it.relativePath, maxExtraDepth = 2)
        }
        return candidates.minByOrNull {
            COVER_NAMES.indexOf(it.lowerName).takeIf { index -> index >= 0 } ?: Int.MAX_VALUE
        }
    }

    private data class DirectoryNode(
        val file: DocumentFile,
        val relativePath: String,
        val depth: Int,
    )

    private data class DocumentNode(
        val relativePath: String,
        val parentPath: String,
        val name: String,
        val file: DocumentFile,
    ) {
        val lowerName: String = name.lowercase(Locale.ROOT)
        val size: Long get() = file.length()
    }

    companion object {
        private const val MAX_DEPTH = 9
        private const val MAX_FILES = 25_000
        private val METADATA_NAMES = setOf("entry.json", "videoinfo.json", ".videoinfo")
        private val COVER_NAMES = listOf("image.jpg", "cover.jpg", "image.png", "group.jpg")
    }
}

internal object BiliShellCacheScanner {
    private val cacheRoots = listOf(
        "/storage/emulated/0/Android/data/tv.danmaku.bili/download",
        "/sdcard/Android/data/tv.danmaku.bili/download",
    )

    fun scanDefaultCaches(): List<BiliCacheItem> {
        val existingRoots = existingCacheRoots()
        return existingRoots
            .flatMap(::scanRoot)
            .distinctBy(BiliCacheItem::id)
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.displayTitle() })
    }

    fun existingCacheRoots(): List<File> =
        cacheRoots
            .map(::File)
            .filter { it.isDirectory }
            .distinctBy { file ->
                runCatching { file.canonicalPath }.getOrDefault(file.absolutePath)
            }

    fun watchDirectories(maxDepth: Int): List<File> {
        val roots = cacheRoots.map(::File)
        val packageDirectories = roots.mapNotNull { it.parentFile?.takeIf(File::isDirectory) }
        val cacheDirectories = roots
            .filter(File::isDirectory)
            .flatMap { root ->
                root.walkTopDown()
                    .maxDepth(maxDepth)
                    .filter(File::isDirectory)
                    .toList()
            }
        return (packageDirectories + cacheDirectories).distinctBy { file ->
            runCatching { file.canonicalPath }.getOrDefault(file.absolutePath)
        }
    }

    private fun scanRoot(root: File): List<BiliCacheItem> {
        val nodes = collectFiles(root)
        val metadataParents = nodes
            .filter { it.lowerName in setOf("entry.json", "videoinfo.json", ".videoinfo") }
            .groupBy(FileNode::parentPath)
        val m4sNodes = nodes.filter {
            it.lowerName.endsWith(".m4s") && !it.lowerName.endsWith(".bdl")
        }
        val usedPaths = mutableSetOf<String>()
        val items = mutableListOf<BiliCacheItem>()

        for ((parentPath, metadataNodes) in metadataParents) {
            val descendants = m4sNodes.filter {
                isDescendant(parentPath, it.relativePath, maxExtraDepth = 3)
            }
            if (descendants.isEmpty()) continue
            val playUrl = nodes.firstOrNull {
                it.lowerName == ".playurl" &&
                    isDescendant(parentPath, it.relativePath, maxExtraDepth = 2)
            }?.let(::readFileText)
            val hints = BiliCacheMetadataParser.parsePlayUrl(playUrl)
            val audio = chooseFileAudio(descendants, hints) ?: continue
            val entry = metadataNodes.firstOrNull { it.lowerName == "entry.json" }
                ?.let(::readFileText)
            val videoInfo = metadataNodes.firstOrNull { it.lowerName == "videoinfo.json" }
                ?.let(::readFileText)
                ?: metadataNodes.firstOrNull { it.lowerName == ".videoinfo" }?.let(::readFileText)
            val metadata = BiliCacheMetadataParser.parse(
                entryJson = entry,
                videoInfoJson = videoInfo,
                fallbackId = parentPath.ifBlank { audio.relativePath },
                fallbackTitle = parentPath.substringAfterLast('/').ifBlank { "B站缓存" },
            )
            val hint = matchingHint(audio.name, hints)
            usedPaths += audio.file.absolutePath
            items += BiliCacheItem(
                id = metadata.sourceId,
                title = metadata.title,
                subtitle = metadata.subtitle,
                owner = metadata.owner,
                durationSeconds = metadata.durationSeconds,
                audioSize = audio.file.length(),
                mimeType = hint?.mimeType ?: "audio/mp4",
                codec = hint?.codec.orEmpty(),
                audioLocation = audio.file.absolutePath,
                coverLocation = chooseFileCover(nodes, parentPath)?.file?.absolutePath,
                accessMode = BiliCacheAccessMode.SHIZUKU,
                completed = metadata.completed && audio.file.length() > 0,
            )
        }

        m4sNodes
            .filter { it.file.absolutePath !in usedPaths }
            .filter(::isFileAudioOnly)
            .forEach { audio ->
                items += BiliCacheItem(
                    id = audio.relativePath,
                    title = audio.parentPath.substringAfterLast('/').ifBlank {
                        audio.name.substringBeforeLast('.')
                    },
                    audioSize = audio.file.length(),
                    audioLocation = audio.file.absolutePath,
                    coverLocation = chooseFileCover(nodes, audio.parentPath)?.file?.absolutePath,
                    accessMode = BiliCacheAccessMode.SHIZUKU,
                    completed = audio.file.length() > 0,
                )
            }
        return items
    }

    private fun collectFiles(root: File): List<FileNode> {
        val result = mutableListOf<FileNode>()
        val queue = ArrayDeque<FileDirectoryNode>()
        queue.add(FileDirectoryNode(root, "", 0))
        var visited = 0
        while (queue.isNotEmpty() && visited < 25_000) {
            val directory = queue.removeFirst()
            val children = runCatching { directory.file.listFiles() }.getOrNull() ?: continue
            for (child in children) {
                if (++visited > 25_000) break
                val relativePath = joinPath(directory.relativePath, child.name)
                if (child.isDirectory && directory.depth < 9) {
                    queue.add(FileDirectoryNode(child, relativePath, directory.depth + 1))
                } else if (child.isFile) {
                    result += FileNode(relativePath, directory.relativePath, child.name, child)
                }
            }
        }
        return result
    }

    private fun chooseFileAudio(
        candidates: List<FileNode>,
        hints: List<BiliPlayUrlAudio>,
    ): FileNode? {
        candidates.firstOrNull { it.lowerName == "audio.m4s" }?.let { return it }
        for (hint in hints) {
            candidates.firstOrNull {
                it.name.equals(hint.fileName, ignoreCase = true) ||
                    (hint.id.isNotBlank() && it.lowerName.endsWith("-${hint.id}.m4s"))
            }?.let { return it }
        }
        return candidates.sortedBy { it.file.length() }.firstOrNull(::isFileAudioOnly)
    }

    private fun isFileAudioOnly(node: FileNode): Boolean = runCatching {
        val header = FileInputStream(node.file).use(::readHeader)
        val skip = BiliM4sHeader.bytesToSkip(header)
        if (skip < 0 || node.file.length() <= skip) return false
        FileInputStream(node.file).use { input ->
            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(
                    input.fd,
                    skip.toLong(),
                    node.file.length() - skip,
                )
                var hasAudio = false
                var hasVideo = false
                for (index in 0 until extractor.trackCount) {
                    val mime = extractor.getTrackFormat(index)
                        .getString(android.media.MediaFormat.KEY_MIME)
                        .orEmpty()
                    hasAudio = hasAudio || mime.startsWith("audio/")
                    hasVideo = hasVideo || mime.startsWith("video/")
                }
                hasAudio && !hasVideo
            } finally {
                extractor.release()
            }
        }
    }.getOrDefault(false)

    private fun readFileText(node: FileNode): String? = runCatching {
        FileInputStream(node.file).use(::readLimitedText)
    }.getOrNull()

    private fun chooseFileCover(
        nodes: List<FileNode>,
        parentPath: String,
    ): FileNode? {
        val coverNames = listOf("image.jpg", "cover.jpg", "image.png", "group.jpg")
        return nodes
            .filter {
                it.lowerName in coverNames &&
                    isDescendant(parentPath, it.relativePath, maxExtraDepth = 2)
            }
            .minByOrNull {
                coverNames.indexOf(it.lowerName).takeIf { index -> index >= 0 } ?: Int.MAX_VALUE
            }
    }

    private data class FileDirectoryNode(
        val file: File,
        val relativePath: String,
        val depth: Int,
    )

    private data class FileNode(
        val relativePath: String,
        val parentPath: String,
        val name: String,
        val file: File,
    ) {
        val lowerName = name.lowercase(Locale.ROOT)
    }
}

private fun matchingHint(
    fileName: String,
    hints: List<BiliPlayUrlAudio>,
): BiliPlayUrlAudio? = hints.firstOrNull {
    fileName.equals(it.fileName, ignoreCase = true) ||
        (it.id.isNotBlank() && fileName.lowercase(Locale.ROOT).endsWith("-${it.id}.m4s"))
}

private fun isDescendant(parentPath: String, childPath: String, maxExtraDepth: Int): Boolean {
    val normalizedParent = parentPath.trim('/')
    val normalizedChild = childPath.trim('/')
    if (normalizedParent.isNotEmpty() &&
        normalizedChild != normalizedParent &&
        !normalizedChild.startsWith("$normalizedParent/")
    ) {
        return false
    }
    val parentDepth = normalizedParent.count { it == '/' } + if (normalizedParent.isEmpty()) 0 else 1
    val childDepth = normalizedChild.count { it == '/' } + if (normalizedChild.isEmpty()) 0 else 1
    return childDepth - parentDepth <= maxExtraDepth
}

private fun joinPath(parent: String, name: String): String =
    if (parent.isBlank()) name else "$parent/$name"

private fun readHeader(input: InputStream): ByteArray {
    val buffer = ByteArray(32)
    var total = 0
    while (total < buffer.size) {
        val read = input.read(buffer, total, buffer.size - total)
        if (read <= 0) break
        total += read
    }
    return buffer.copyOf(total)
}

private fun readLimitedText(input: InputStream): String {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8 * 1_024)
    var total = 0
    while (total < 2 * 1_024 * 1_024) {
        val read = input.read(buffer, 0, minOf(buffer.size, 2 * 1_024 * 1_024 - total))
        if (read <= 0) break
        output.write(buffer, 0, read)
        total += read
    }
    return output.toString(Charsets.UTF_8.name())
}
