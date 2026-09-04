package com.infinitezerone.minibgm.core.designsystem.component.bbcode

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

/**
 * Bangumi 专用 BBCode 富文本渲染组件
 *
 * 原生支持：
 * - [quote] 引用块（带强调色左竖线、浅色卡片底色及作者注明）
 * - [mask] 黑幕刮刮乐（默认遮罩，点击原地刮开揭晓/再次点击遮挡）
 * - [b] 粗体、[s] 删除线、[i] 斜体、[u] 下划线、[url] 超链接
 * - (bgmXX) 行内娘表情贴图（无缝排版与官方 GIF 加载）
 * - [img] 独立安全限高图片渲染
 */
@Composable
fun BgmBbCodeContent(
    content: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodySmall,
    color: Color = MaterialTheme.colorScheme.onSurface,
    onUrlClick: ((String) -> Unit)? = null,
) {
    val blocks = remember(content) { BgmBbCodeParser.parseBlocks(content) }

    if (blocks.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        blocks.forEach { block ->
            when (block) {
                is BbCodeBlock.Quote -> {
                    BgmBbCodeQuote(
                        quote = block,
                        style = style,
                        color = color,
                        onUrlClick = onUrlClick,
                    )
                }
                is BbCodeBlock.Image -> {
                    BgmBbCodeImage(
                        image = block,
                        onUrlClick = onUrlClick,
                    )
                }
                is BbCodeBlock.Paragraph -> {
                    BgmBbCodeParagraph(
                        paragraph = block,
                        style = style,
                        color = color,
                        onUrlClick = onUrlClick,
                    )
                }
            }
        }
    }
}

/**
 * 引用卡片渲染
 */
@Composable
private fun BgmBbCodeQuote(
    quote: BbCodeBlock.Quote,
    style: TextStyle,
    color: Color,
    onUrlClick: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
        border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = modifier.fillMaxWidth().padding(vertical = 2.dp),
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
            )
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (quote.author != null) {
                    Text(
                        text = "引用 @${quote.author} 说：",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                BgmBbCodeParagraph(
                    paragraph = remember(quote.content) { BgmBbCodeParser.parseParagraph(quote.content) },
                    style = style.copy(fontSize = (style.fontSize.value * 0.9f).sp),
                    color = color.copy(alpha = 0.85f),
                    onUrlClick = onUrlClick,
                )
            }
        }
    }
}

/**
 * 图片卡片渲染
 */
@Composable
private fun BgmBbCodeImage(
    image: BbCodeBlock.Image,
    onUrlClick: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        onClick = { onUrlClick?.invoke(image.url) },
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
    ) {
        AsyncImage(
            model = image.url,
            contentDescription = "评论图片",
            contentScale = ContentScale.Fit,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp)
                    .clip(RoundedCornerShape(8.dp)),
        )
    }
}

/**
 * 富文本段落（含表情行内排版、黑幕揭晓交互及样式文本）
 */
@Composable
private fun BgmBbCodeParagraph(
    paragraph: BbCodeBlock.Paragraph,
    style: TextStyle,
    color: Color,
    onUrlClick: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val revealedMasks = remember { mutableStateMapOf<String, Boolean>() }

    val inlineContent =
        remember(paragraph.elements) {
            val map = mutableMapOf<String, InlineTextContent>()
            paragraph.elements.filterIsInstance<BbInlineElement.Sticker>().forEach { sticker ->
                val key = "sticker_${sticker.stickerId}_${sticker.hashCode()}"
                map[key] =
                    InlineTextContent(
                        Placeholder(
                            width = 20.sp,
                            height = 20.sp,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                        ),
                    ) {
                        AsyncImage(
                            model = sticker.url,
                            contentDescription = sticker.code,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
            }
            map
        }

    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
    val onSurface = MaterialTheme.colorScheme.onSurface

    val annotatedString =
        remember(paragraph, revealedMasks.toMap(), primaryColor, onSurface) {
            buildAnnotatedString {
                paragraph.elements.forEach { element ->
                    when (element) {
                        is BbInlineElement.Plain -> {
                            append(element.text)
                        }
                        is BbInlineElement.Styled -> {
                            val textDecorations =
                                when {
                                    element.isUnderline && element.isStrikethrough ->
                                        TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
                                    element.isUnderline -> TextDecoration.Underline
                                    element.isStrikethrough -> TextDecoration.LineThrough
                                    else -> null
                                }
                            withStyle(
                                SpanStyle(
                                    fontWeight = if (element.isBold) FontWeight.Bold else null,
                                    fontStyle = if (element.isItalic) FontStyle.Italic else null,
                                    textDecoration = textDecorations,
                                    color = if (element.url != null) primaryColor else Color.Unspecified,
                                ),
                            ) {
                                if (element.url != null) {
                                    pushLink(
                                        LinkAnnotation.Url(
                                            url = element.url,
                                            linkInteractionListener =
                                                onUrlClick?.let { onClick ->
                                                    { onClick(element.url) }
                                                },
                                        ),
                                    )
                                    append(element.text)
                                    pop()
                                } else {
                                    append(element.text)
                                }
                            }
                        }
                        is BbInlineElement.Mask -> {
                            val isRevealed = revealedMasks[element.id] == true
                            val link =
                                LinkAnnotation.Clickable(
                                    tag = element.id,
                                    linkInteractionListener = {
                                        revealedMasks[element.id] = !isRevealed
                                    },
                                )
                            pushLink(link)
                            withStyle(
                                SpanStyle(
                                    background =
                                        if (isRevealed) {
                                            primaryContainer.copy(alpha = 0.5f)
                                        } else {
                                            onSurface.copy(alpha = 0.85f)
                                        },
                                    color = if (isRevealed) onPrimaryContainer else Color.Transparent,
                                ),
                            ) {
                                append(element.text.ifEmpty { " " })
                            }
                            pop()
                        }
                        is BbInlineElement.Sticker -> {
                            val key = "sticker_${element.stickerId}_${element.hashCode()}"
                            appendInlineContent(id = key, alternateText = element.code)
                        }
                    }
                }
            }
        }

    Text(
        text = annotatedString,
        inlineContent = inlineContent,
        style = style,
        color = color,
        modifier = modifier,
    )
}
