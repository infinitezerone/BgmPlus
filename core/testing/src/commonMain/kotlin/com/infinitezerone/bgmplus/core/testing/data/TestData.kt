package com.infinitezerone.bgmplus.core.testing.data

import com.infinitezerone.bgmplus.core.model.AirSchedule
import com.infinitezerone.bgmplus.core.model.Episode
import com.infinitezerone.bgmplus.core.model.Rating
import com.infinitezerone.bgmplus.core.model.SiteLink
import com.infinitezerone.bgmplus.core.model.Subject
import com.infinitezerone.bgmplus.core.model.SubjectImages
import com.infinitezerone.bgmplus.core.model.UserAvatar
import com.infinitezerone.bgmplus.core.model.UserProfile

val sampleSubject =
    Subject(
        id = 1001L,
        name = "葬送のフリーレン",
        nameCn = "葬送的芙莉莲",
        summary = "打倒魔王之后的勇者一行的后日谈。",
        date = "2023-09-29",
        eps = 28,
        totalEpisodes = 28,
        images =
            SubjectImages(
                large = "https://lain.bgm.tv/pic/cover/l/sample_1001.jpg",
                common = "https://lain.bgm.tv/pic/cover/c/sample_1001.jpg",
            ),
        rating =
            Rating(
                score = 8.9,
                total = 15000,
                rank = 1,
            ),
    )

val sampleEpisodeList =
    listOf(
        Episode(
            id = 2001L,
            sort = 1f,
            ep = 1f,
            name = "冒険の終わり",
            nameCn = "冒险的终结",
            duration = "24:00",
            airdate = "2023-09-29",
        ),
        Episode(
            id = 2002L,
            sort = 2f,
            ep = 2f,
            name = "別に魔法じゃなくたって…",
            nameCn = "就算不是魔法也…",
            duration = "24:00",
            airdate = "2023-09-29",
        ),
    )

val sampleAirScheduleList =
    listOf(
        AirSchedule(
            bgmId = 1001L,
            title = "葬送のフリーレン",
            titleCn = "葬送的芙莉莲",
            coverUrl = "https://lain.bgm.tv/pic/cover/l/sample_1001.jpg",
            ratingScore = 8.9,
            beginUtc = "2023-09-29T14:00:00.000Z",
            weekday = 5,
            timeCst = "22:00",
            timeJst = "23:00",
            siteLinks =
                listOf(
                    SiteLink(siteName = "bilibili", displayName = "哔哩哔哩", playUrl = "https://www.bilibili.com"),
                    SiteLink(siteName = "bahamut", displayName = "巴哈姆特", playUrl = "https://ani.gamer.com.tw"),
                ),
            nextEpisodeNumber = 1,
            isAiring = true,
        ),
    )

val sampleUserProfile =
    UserProfile(
        id = 42L,
        username = "infinitezerone",
        nickname = "零一",
        avatar =
            UserAvatar(
                large = "https://lain.bgm.tv/pic/user/l/sample_user.jpg",
            ),
        sign = "Stay hungry, stay foolish.",
    )

val sampleUserProfileAlt =
    UserProfile(
        id = 999L,
        username = "alt_user",
        nickname = "马甲二号",
        avatar =
            UserAvatar(
                large = "https://lain.bgm.tv/pic/user/l/sample_alt.jpg",
            ),
        sign = "Alt account for Galgame.",
    )

val sampleUserCollection =
    com.infinitezerone.bgmplus.core.model.UserCollection(
        userId = 42L,
        subjectId = 1001L,
        subjectType = 2,
        rate = 9,
        type = 3, // DOING (在看)
        comment = "神作无误！",
        epStatus = 12,
        volStatus = 0,
        updatedAt = "2026-08-31T09:00:00Z",
        subject = sampleSubject,
    )
