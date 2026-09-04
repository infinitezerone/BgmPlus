package com.infinitezerone.minibgm.core.designsystem.component.bbcode

/**
 * Bangumi 块级语法树节点
 */
sealed interface BbCodeBlock {
    /**
     * 引用块：如 [quote][b]用户名[/b] 说: 引用的文本[/quote] 或 [quote]引用的文本[/quote]
     */
    data class Quote(
        val author: String?,
        val content: String,
    ) : BbCodeBlock

    /**
     * 独立图片块：如 [img]https://...[/img] 或 [IMG]...[/IMG]
     */
    data class Image(
        val url: String,
    ) : BbCodeBlock

    /**
     * 富文本段落块
     */
    data class Paragraph(
        val rawText: String,
        val elements: List<BbInlineElement>,
    ) : BbCodeBlock
}

/**
 * Bangumi 行内语法树节点
 */
sealed interface BbInlineElement {
    /** 普通纯文本 */
    data class Plain(
        val text: String,
    ) : BbInlineElement

    /**
     * 样式文本：支持粗体、斜体、下划线、删除线、超链接、字号缩放
     */
    data class Styled(
        val text: String,
        val isBold: Boolean = false,
        val isItalic: Boolean = false,
        val isUnderline: Boolean = false,
        val isStrikethrough: Boolean = false,
        val url: String? = null,
        val sizeScale: Float? = null,
    ) : BbInlineElement

    /**
     * 黑幕 / 剧透文本：如 [mask]剧透[/mask]，支持点击揭开
     */
    data class Mask(
        val id: String,
        val text: String,
    ) : BbInlineElement

    /**
     * Bangumi 经典娘表情贴图：如 (bgm38)
     */
    data class Sticker(
        val code: String,
        val stickerId: Int,
        val url: String,
    ) : BbInlineElement
}

/**
 * Bangumi BBCode 解析器
 *
 * 专门解析 Bangumi 社区吐槽、短评与讨论帖中的 BBCode 及表情语法。
 * 具备极高容错性，针对未闭合标签或畸形输入提供平滑降级，确保永不崩溃。
 */
object BgmBbCodeParser {
    private val QUOTE_REGEX = Regex("""\[quote\]([\s\S]*?)\[/quote\]""", RegexOption.IGNORE_CASE)
    private val IMG_REGEX = Regex("""\[img\]([\s\S]*?)\[/img\]""", RegexOption.IGNORE_CASE)
    private val QUOTE_AUTHOR_REGEX = Regex("""^(?:\[b\])?(.*?)(?:\[/b\])?\s*(?:说|:)\s*:\s*([\s\S]*)$""", RegexOption.DOT_MATCHES_ALL)
    private val STICKER_REGEX = Regex("""\(bgm(\d+)\)""")
    private val MASK_REGEX = Regex("""\[mask\]([\s\S]*?)\[/mask\]""", RegexOption.IGNORE_CASE)
    private val UNWANTED_TAGS_REGEX = Regex("""\[/?(?:photo=\d+|right|size=\d+|color=[^\]]+)\]""", RegexOption.IGNORE_CASE)

    /**
     * 将原始评论文本解析为块级语法树列表
     */
    fun parseBlocks(rawText: String): List<BbCodeBlock> {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) return emptyList()

        val blocks = mutableListOf<BbCodeBlock>()
        var currentIndex = 0

        // 统一匹配 [quote]...[/quote] 与 [img]...[/img] 两种块级元素
        val blockRegex = Regex("""(\[quote\][\s\S]*?\[/quote\]|\[img\][\s\S]*?\[/img\])""", RegexOption.IGNORE_CASE)
        val matches = blockRegex.findAll(trimmed)

        for (match in matches) {
            val range = match.range
            if (range.first > currentIndex) {
                val textSegment = trimmed.substring(currentIndex, range.first).trim()
                if (textSegment.isNotEmpty()) {
                    blocks.add(parseParagraph(textSegment))
                }
            }

            val matchedStr = match.value
            if (matchedStr.startsWith("[quote", ignoreCase = true)) {
                val quoteInner =
                    QUOTE_REGEX
                        .find(matchedStr)
                        ?.groupValues
                        ?.get(1)
                        ?.trim()
                        .orEmpty()
                val authorMatch = QUOTE_AUTHOR_REGEX.find(quoteInner)
                if (authorMatch != null) {
                    val author = authorMatch.groupValues[1].trim()
                    val content = authorMatch.groupValues[2].trim()
                    blocks.add(BbCodeBlock.Quote(author = author.ifBlank { null }, content = content))
                } else {
                    blocks.add(BbCodeBlock.Quote(author = null, content = quoteInner))
                }
            } else if (matchedStr.startsWith("[img", ignoreCase = true)) {
                val imgUrl =
                    IMG_REGEX
                        .find(matchedStr)
                        ?.groupValues
                        ?.get(1)
                        ?.trim()
                        .orEmpty()
                if (imgUrl.isNotBlank()) {
                    blocks.add(BbCodeBlock.Image(url = imgUrl))
                }
            }

            currentIndex = range.last + 1
        }

        if (currentIndex < trimmed.length) {
            val remaining = trimmed.substring(currentIndex).trim()
            if (remaining.isNotEmpty()) {
                blocks.add(parseParagraph(remaining))
            }
        }

        return blocks
    }

    /**
     * 将一段非块级文本解析为包含丰富行内样式的 Paragraph
     */
    fun parseParagraph(rawParagraph: String): BbCodeBlock.Paragraph {
        // 先清理不影响排版的未知废弃标签（如 [right] 等）
        val cleanParagraph = rawParagraph.replace(UNWANTED_TAGS_REGEX, "")

        val elements = mutableListOf<BbInlineElement>()
        var maskCounter = 0

        // 正则识别 [mask]...[/mask] 与 (bgmXX) 贴图
        val inlineTokenRegex = Regex("""(\[mask\][\s\S]*?\[/mask\]|\(bgm\d+\))""", RegexOption.IGNORE_CASE)
        var currentIndex = 0

        val matches = inlineTokenRegex.findAll(cleanParagraph)
        for (match in matches) {
            val range = match.range
            if (range.first > currentIndex) {
                val textChunk = cleanParagraph.substring(currentIndex, range.first)
                if (textChunk.isNotEmpty()) {
                    elements.addAll(parseFormattedText(textChunk))
                }
            }

            val token = match.value
            if (token.startsWith("[mask", ignoreCase = true)) {
                val inner =
                    MASK_REGEX
                        .find(token)
                        ?.groupValues
                        ?.get(1)
                        .orEmpty()
                val maskId = "mask_${++maskCounter}_${inner.hashCode()}"
                elements.add(BbInlineElement.Mask(id = maskId, text = inner))
            } else if (token.startsWith("(bgm", ignoreCase = true)) {
                val stickerNumStr =
                    STICKER_REGEX
                        .find(token)
                        ?.groupValues
                        ?.get(1)
                        .orEmpty()
                val stickerId = stickerNumStr.toIntOrNull() ?: 0
                val paddedNum = stickerId.toString().padStart(2, '0')
                val stickerUrl = "https://lain.bgm.tv/img/smiles/tv/$paddedNum.gif"
                elements.add(
                    BbInlineElement.Sticker(
                        code = token,
                        stickerId = stickerId,
                        url = stickerUrl,
                    ),
                )
            }

            currentIndex = range.last + 1
        }

        if (currentIndex < cleanParagraph.length) {
            val remaining = cleanParagraph.substring(currentIndex)
            if (remaining.isNotEmpty()) {
                elements.addAll(parseFormattedText(remaining))
            }
        }

        return BbCodeBlock.Paragraph(rawText = cleanParagraph, elements = elements)
    }

    /**
     * 解析基础格式化标签：[b], [i], [s], [u], [url]
     */
    private fun parseFormattedText(text: String): List<BbInlineElement> {
        if (!text.contains('[') || !text.contains(']')) {
            return listOf(BbInlineElement.Plain(text))
        }

        val results = mutableListOf<BbInlineElement>()
        val tagRegex = Regex("""\[(b|i|s|u|url)(?:=([^\]]+))?\]([\s\S]*?)\[/\1\]""", RegexOption.IGNORE_CASE)
        var currentIndex = 0

        val matches = tagRegex.findAll(text)
        for (match in matches) {
            val range = match.range
            if (range.first > currentIndex) {
                val plainPart = text.substring(currentIndex, range.first)
                if (plainPart.isNotEmpty()) {
                    results.add(BbInlineElement.Plain(plainPart))
                }
            }

            val tagName =
                match.groups[1]
                    ?.value
                    .orEmpty()
                    .lowercase()
            val tagArg = match.groups[2]?.value?.trim('"', '\'')
            val innerContent = match.groups[3]?.value.orEmpty()

            when (tagName) {
                "b" -> {
                    results.add(BbInlineElement.Styled(text = innerContent, isBold = true))
                }
                "i" -> {
                    results.add(BbInlineElement.Styled(text = innerContent, isItalic = true))
                }
                "s" -> {
                    results.add(BbInlineElement.Styled(text = innerContent, isStrikethrough = true))
                }
                "u" -> {
                    results.add(BbInlineElement.Styled(text = innerContent, isUnderline = true))
                }
                "url" -> {
                    val url = tagArg?.ifBlank { null } ?: innerContent.trim()
                    results.add(BbInlineElement.Styled(text = innerContent, url = url, isUnderline = true))
                }
                else -> {
                    results.add(BbInlineElement.Plain(innerContent))
                }
            }

            currentIndex = range.last + 1
        }

        if (currentIndex < text.length) {
            val remaining = text.substring(currentIndex)
            if (remaining.isNotEmpty()) {
                results.add(BbInlineElement.Plain(remaining))
            }
        }

        return if (results.isEmpty()) listOf(BbInlineElement.Plain(text)) else results
    }
}
