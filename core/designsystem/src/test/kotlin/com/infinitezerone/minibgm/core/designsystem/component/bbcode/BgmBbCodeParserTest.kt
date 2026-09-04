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

    @Test
    fun parseParagraph_autolinksRawUrls_andFormatsBangumiLabels() {
        val input = "大家可以看 https://bgm.tv/subject/328793。还有角色 https://bgm.tv/character/12345 很棒"
        val paragraph = BgmBbCodeParser.parseParagraph(input)

        val styled = paragraph.elements.filterIsInstance<BbInlineElement.Styled>()
        assertEquals(2, styled.size)

        assertEquals("条目 #328793", styled[0].text)
        assertEquals("https://bgm.tv/subject/328793", styled[0].url)
        assertTrue(styled[0].isUnderline)

        assertEquals("角色 #12345", styled[1].text)
        assertEquals("https://bgm.tv/character/12345", styled[1].url)

        val plains = paragraph.elements.filterIsInstance<BbInlineElement.Plain>()
        assertTrue(plains.any { it.text.contains("。还有角色 ") })
    }

    @Test
    fun parseParagraph_bbcodeUrl_formatsBangumiLinkIfNotCustom() {
        val tagWithoutCustomTitle = "[url]https://bangumi.tv/person/6789[/url]"
        val paragraph1 = BgmBbCodeParser.parseParagraph(tagWithoutCustomTitle)
        val styled1 = paragraph1.elements.filterIsInstance<BbInlineElement.Styled>().first()
        assertEquals("人物 #6789", styled1.text)
        assertEquals("https://bangumi.tv/person/6789", styled1.url)

        val tagWithCustomTitle = "[url=https://bgm.tv/subject/328793]葬送的芙莉莲[/url]"
        val paragraph2 = BgmBbCodeParser.parseParagraph(tagWithCustomTitle)
        val styled2 = paragraph2.elements.filterIsInstance<BbInlineElement.Styled>().first()
        assertEquals("葬送的芙莉莲", styled2.text)
        assertEquals("https://bgm.tv/subject/328793", styled2.url)
    }

    @Test
    fun parseParagraph_autolinksExternalUrls_preservesRawUrl() {
        val input = "访问 https://github.com 查看源码"
        val paragraph = BgmBbCodeParser.parseParagraph(input)
        val styled = paragraph.elements.filterIsInstance<BbInlineElement.Styled>().first()
        assertEquals("https://github.com", styled.text)
        assertEquals("https://github.com", styled.url)
    }
}
