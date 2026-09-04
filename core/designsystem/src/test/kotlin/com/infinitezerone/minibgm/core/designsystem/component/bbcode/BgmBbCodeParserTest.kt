package com.infinitezerone.minibgm.core.designsystem.component.bbcode

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BgmBbCodeParserTest {
    @Test
    fun parseBlocks_extractsQuoteWithAuthor() {
        val input =
            """
            [quote][b]蒅南[/b] 说: 更正一下，应该是PKM[/quote]
            lycoris应该都不是正常人类
            """.trimIndent()

        val blocks = BgmBbCodeParser.parseBlocks(input)
        assertEquals(2, blocks.size)

        val quote = blocks[0] as BbCodeBlock.Quote
        assertEquals("蒅南", quote.author)
        assertEquals("更正一下，应该是PKM", quote.content)

        val paragraph = blocks[1] as BbCodeBlock.Paragraph
        assertEquals("lycoris应该都不是正常人类", paragraph.rawText)
    }

    @Test
    fun parseBlocks_extractsQuoteWithoutAuthor() {
        val input =
            """
            [quote]这是一段无作者的纯引用[/quote]
            下面是回复内容
            """.trimIndent()

        val blocks = BgmBbCodeParser.parseBlocks(input)
        assertEquals(2, blocks.size)

        val quote = blocks[0] as BbCodeBlock.Quote
        assertNull(quote.author)
        assertEquals("这是一段无作者的纯引用", quote.content)
    }

    @Test
    fun parseBlocks_extractsImageBlock() {
        val input =
            """
            还真有
            [img]https://i0.hdslb.com/bfs/album/332c0e7874bf59fddd963021750fd05e45570bb8.png[/img]
            做的好啊
            """.trimIndent()

        val blocks = BgmBbCodeParser.parseBlocks(input)
        assertEquals(3, blocks.size)

        assertTrue(blocks[0] is BbCodeBlock.Paragraph)
        val imgBlock = blocks[1] as BbCodeBlock.Image
        assertEquals("https://i0.hdslb.com/bfs/album/332c0e7874bf59fddd963021750fd05e45570bb8.png", imgBlock.url)
        assertTrue(blocks[2] is BbCodeBlock.Paragraph)
    }

    @Test
    fun parseParagraph_extractsBangumiStickers() {
        val input = "幽默和平日本(bgm38) 花之塔确实好听(bgm66) 冲(bgm09)"
        val paragraph = BgmBbCodeParser.parseParagraph(input)

        val stickers = paragraph.elements.filterIsInstance<BbInlineElement.Sticker>()
        assertEquals(3, stickers.size)
        assertEquals(38, stickers[0].stickerId)
        assertEquals("https://lain.bgm.tv/img/smiles/tv/38.gif", stickers[0].url)
        assertEquals(66, stickers[1].stickerId)
        assertEquals("https://lain.bgm.tv/img/smiles/tv/66.gif", stickers[1].url)
        assertEquals(9, stickers[2].stickerId)
        assertEquals("https://lain.bgm.tv/img/smiles/tv/09.gif", stickers[2].url)
    }

    @Test
    fun parseParagraph_extractsMaskSpoiler() {
        val input = "开头：[mask]你说fazhi我都觉得搞笑[/mask] 手提机枪太帅了"
        val paragraph = BgmBbCodeParser.parseParagraph(input)

        val masks = paragraph.elements.filterIsInstance<BbInlineElement.Mask>()
        assertEquals(1, masks.size)
        assertEquals("你说fazhi我都觉得搞笑", masks[0].text)
        assertTrue(masks[0].id.startsWith("mask_"))
    }

    @Test
    fun parseParagraph_extractsStyles_bold_italic_strike_url() {
        val input = "这是[b]粗体[/b]和[s]删除线[/s]以及[url=https://bgm.tv]番组计划[/url]"
        val paragraph = BgmBbCodeParser.parseParagraph(input)

        val styledElements = paragraph.elements.filterIsInstance<BbInlineElement.Styled>()
        assertEquals(3, styledElements.size)

        assertEquals("粗体", styledElements[0].text)
        assertTrue(styledElements[0].isBold)

        assertEquals("删除线", styledElements[1].text)
        assertTrue(styledElements[1].isStrikethrough)

        assertEquals("番组计划", styledElements[2].text)
        assertEquals("https://bgm.tv", styledElements[2].url)
    }

    @Test
    fun parseBlocks_handlesMalformedAndEmptySafely() {
        val emptyBlocks = BgmBbCodeParser.parseBlocks("")
        assertTrue(emptyBlocks.isEmpty())

        val unclosedBlocks = BgmBbCodeParser.parseBlocks("[b]未闭合的粗体[mask]未闭合黑幕")
        assertEquals(1, unclosedBlocks.size)
        assertTrue(unclosedBlocks[0] is BbCodeBlock.Paragraph)
    }
}
