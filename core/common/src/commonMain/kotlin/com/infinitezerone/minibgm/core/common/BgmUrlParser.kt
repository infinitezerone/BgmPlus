package com.infinitezerone.minibgm.core.common

/**
 * Bangumi 链接资源类型
 */
sealed interface BgmLink {
    /** 条目链接：/subject/12345 */
    data class Subject(
        val subjectId: Long,
    ) : BgmLink

    /** 角色链接：/character/12345 或 /crt/12345 */
    data class Character(
        val characterId: Long,
    ) : BgmLink

    /** 人物/制作人员链接：/person/12345 或 /prsn/12345 */
    data class Person(
        val personId: Long,
    ) : BgmLink

    /** 单集链接：/ep/12345 */
    data class Episode(
        val episodeId: Long,
    ) : BgmLink

    /** 讨论帖链接：/subject/topic/12345 或 /group/topic/12345 或 /rakuen/topic/... */
    data class Topic(
        val topicId: Long,
        val type: String = "subject",
    ) : BgmLink

    /** 用户空间链接：/user/12345 或 /user/username */
    data class User(
        val username: String,
    ) : BgmLink

    /** 非 Bangumi 或未识别的外部链接 */
    data class External(
        val url: String,
    ) : BgmLink
}

/**
 * Bangumi 网页链接解析与识别工具
 */
object BgmUrlParser {
    private val BGM_DOMAINS =
        setOf(
            "bgm.tv",
            "bangumi.tv",
            "chii.in",
        )

    private val SUBJECT_REGEX = Regex("""^/subject/(\d+)(?:/.*)?$""")
    private val CHARACTER_REGEX = Regex("""^/(?:character|crt)/(\d+)(?:/.*)?$""")
    private val PERSON_REGEX = Regex("""^/(?:person|prsn)/(\d+)(?:/.*)?$""")
    private val EPISODE_REGEX = Regex("""^/ep/(\d+)(?:/.*)?$""")
    private val TOPIC_REGEX = Regex("""^/(?:(subject|group)/topic|rakuen/topic/(\w+))/(\d+)(?:/.*)?$""")
    private val USER_REGEX = Regex("""^/user/([a-zA-Z0-9_-]+)(?:/.*)?$""")

    /**
     * 将给定的 URL 解析为特定的 [BgmLink] 资源类型
     */
    fun parse(rawUrl: String): BgmLink {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return BgmLink.External(rawUrl)

        // 统一提取路径（去掉 query 参数和 hash 锚点）
        val path: String
        val isBangumiDomain: Boolean

        when {
            // 相对路径：如 /subject/12345
            trimmed.startsWith("/") && !trimmed.startsWith("//") -> {
                isBangumiDomain = true
                path = extractPath(trimmed)
            }
            // 协议相对链接：如 //bgm.tv/subject/12345
            trimmed.startsWith("//") -> {
                val fullUrl = "https:$trimmed"
                isBangumiDomain = checkIsBgmDomain(fullUrl)
                path = if (isBangumiDomain) extractPathFromAbsolute(fullUrl) else ""
            }
            // 带有 scheme：http:// 或 https://
            trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) -> {
                isBangumiDomain = checkIsBgmDomain(trimmed)
                path = if (isBangumiDomain) extractPathFromAbsolute(trimmed) else ""
            }
            // 无 scheme 域名开头：如 bgm.tv/subject/12345
            isBgmDomainPrefix(trimmed) -> {
                val fullUrl = "https://$trimmed"
                isBangumiDomain = true
                path = extractPathFromAbsolute(fullUrl)
            }
            else -> {
                return BgmLink.External(trimmed)
            }
        }

        if (!isBangumiDomain || path.isEmpty()) {
            return BgmLink.External(trimmed)
        }

        // 解析匹配各类资源
        SUBJECT_REGEX.matchEntire(path)?.let { match ->
            val id = match.groupValues[1].toLongOrNull()
            if (id != null && id > 0) return BgmLink.Subject(id)
        }

        CHARACTER_REGEX.matchEntire(path)?.let { match ->
            val id = match.groupValues[1].toLongOrNull()
            if (id != null && id > 0) return BgmLink.Character(id)
        }

        PERSON_REGEX.matchEntire(path)?.let { match ->
            val id = match.groupValues[1].toLongOrNull()
            if (id != null && id > 0) return BgmLink.Person(id)
        }

        EPISODE_REGEX.matchEntire(path)?.let { match ->
            val id = match.groupValues[1].toLongOrNull()
            if (id != null && id > 0) return BgmLink.Episode(id)
        }

        TOPIC_REGEX.matchEntire(path)?.let { match ->
            val type = match.groupValues[1].ifBlank { match.groupValues[2] }.ifBlank { "subject" }
            val id = match.groupValues[3].toLongOrNull()
            if (id != null && id > 0) return BgmLink.Topic(topicId = id, type = type)
        }

        USER_REGEX.matchEntire(path)?.let { match ->
            val username = match.groupValues[1]
            if (username.isNotBlank()) return BgmLink.User(username)
        }

        return BgmLink.External(trimmed)
    }

    /**
     * 将识别出的 [BgmLink] 格式化为原生简洁可读的展示文案
     */
    fun formatDisplayLabel(link: BgmLink): String =
        when (link) {
            is BgmLink.Subject -> "条目 #${link.subjectId}"
            is BgmLink.Character -> "角色 #${link.characterId}"
            is BgmLink.Person -> "人物 #${link.personId}"
            is BgmLink.Episode -> "单集 #${link.episodeId}"
            is BgmLink.Topic -> "讨论 #${link.topicId}"
            is BgmLink.User -> "@${link.username}"
            is BgmLink.External -> link.url
        }

    private fun checkIsBgmDomain(url: String): Boolean {
        val domain = extractDomain(url) ?: return false
        val cleanDomain = domain.lowercase().substringBefore(":")
        return BGM_DOMAINS.any { bgmDomain ->
            cleanDomain == bgmDomain || cleanDomain.endsWith(".$bgmDomain")
        }
    }

    private fun isBgmDomainPrefix(url: String): Boolean {
        val firstPart = url.substringBefore("/").lowercase().substringBefore(":")
        return BGM_DOMAINS.any { bgmDomain ->
            firstPart == bgmDomain || firstPart.endsWith(".$bgmDomain")
        }
    }

    private fun extractDomain(url: String): String? {
        val withoutScheme =
            when {
                url.startsWith("https://", ignoreCase = true) -> url.substring(8)
                url.startsWith("http://", ignoreCase = true) -> url.substring(7)
                url.startsWith("//") -> url.substring(2)
                else -> url
            }
        return withoutScheme.substringBefore("/").ifBlank { null }
    }

    private fun extractPathFromAbsolute(url: String): String {
        val withoutScheme =
            when {
                url.startsWith("https://", ignoreCase = true) -> url.substring(8)
                url.startsWith("http://", ignoreCase = true) -> url.substring(7)
                url.startsWith("//") -> url.substring(2)
                else -> url
            }
        val slashIndex = withoutScheme.indexOf('/')
        if (slashIndex == -1) return "/"
        val rawPath = withoutScheme.substring(slashIndex)
        return extractPath(rawPath)
    }

    private fun extractPath(pathWithQueryOrHash: String): String {
        val withoutQuery = pathWithQueryOrHash.substringBefore('?')
        return withoutQuery.substringBefore('#')
    }
}
