package com.example.ui.utils

import com.example.data.model.TransactionType

data class ParsedVoiceRecord(
    val amount: Double?,
    val category: String?,
    val type: TransactionType,
    val merchantOrNote: String,
    val detectedCurrency: String? = null
)

object VoiceBookkeepingParser {

    fun parse(spokenText: String): ParsedVoiceRecord {
        val trimmed = spokenText.trim()
        
        // 1. Determine Transaction Type
        val isIncome = trimmed.contains("收入") || 
                       trimmed.contains("薪水") || 
                       trimmed.contains("薪資") || 
                       trimmed.contains("獎金") || 
                       trimmed.contains("中獎") || 
                       trimmed.contains("分紅") ||
                       trimmed.contains("領薪")

        val txType = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE

        // 2. Extract Amount
        val digitMatch = Regex("""\d+(\.\d+)?""").find(trimmed)
        var amount: Double? = digitMatch?.value?.toDoubleOrNull()

        if (amount == null) {
            amount = parseChineseNumber(trimmed)
        }

        // 3. Detect Currency
        val detectedCurrency = when {
            trimmed.contains("日圓") || trimmed.contains("日幣") || trimmed.contains("日元") -> "JPY"
            trimmed.contains("美元") || trimmed.contains("美金") -> "USD"
            trimmed.contains("歐元") -> "EUR"
            trimmed.contains("韓元") || trimmed.contains("韓幣") -> "KRW"
            trimmed.contains("人民幣") -> "CNY"
            trimmed.contains("港幣") -> "HKD"
            trimmed.contains("英鎊") -> "GBP"
            trimmed.contains("泰銖") -> "THB"
            trimmed.contains("台幣") || trimmed.contains("新台幣") -> "TWD"
            else -> null
        }

        // 4. Category Detection
        val category = when {
            isIncome -> when {
                trimmed.contains("獎金") || trimmed.contains("紅利") -> "獎金紅利"
                trimmed.contains("中獎") || trimmed.contains("發票") -> "中獎發票"
                trimmed.contains("副業") || trimmed.contains("兼職") -> "副業兼職"
                trimmed.contains("投資") || trimmed.contains("股票") -> "投資收益"
                else -> "正職薪資"
            }
            trimmed.contains("咖啡") || trimmed.contains("拉麵") || trimmed.contains("午餐") ||
            trimmed.contains("晚餐") || trimmed.contains("早餐") || trimmed.contains("吃") ||
            trimmed.contains("飯") || trimmed.contains("麵") || trimmed.contains("便當") ||
            trimmed.contains("飲料") || trimmed.contains("麥當勞") || trimmed.contains("星巴克") ||
            trimmed.contains("手搖") || trimmed.contains("火鍋") || trimmed.contains("宵夜") -> "餐飲美食"

            trimmed.contains("捷運") || trimmed.contains("公車") || trimmed.contains("計程車") ||
            trimmed.contains("Uber") || trimmed.contains("高鐵") || trimmed.contains("火車") ||
            trimmed.contains("加油") || trimmed.contains("停車") || trimmed.contains("悠遊卡") -> "交通通勤"

            trimmed.contains("超市") || trimmed.contains("全家") || trimmed.contains("7-11") ||
            trimmed.contains("衣服") || trimmed.contains("鞋") || trimmed.contains("買") ||
            trimmed.contains("購物") || trimmed.contains("蝦皮") || trimmed.contains("屈臣氏") -> "日常購物"

            trimmed.contains("電影") || trimmed.contains("遊戲") || trimmed.contains("門票") ||
            trimmed.contains("Netflix") || trimmed.contains("唱歌") || trimmed.contains("KTV") -> "休閒娛樂"

            trimmed.contains("水費") || trimmed.contains("電費") || trimmed.contains("瓦斯") ||
            trimmed.contains("房租") || trimmed.contains("網路費") || trimmed.contains("居家") -> "居家水電"

            trimmed.contains("看病") || trimmed.contains("診所") || trimmed.contains("醫院") ||
            trimmed.contains("藥") || trimmed.contains("掛號") || trimmed.contains("感冒") -> "醫療保健"

            trimmed.contains("書") || trimmed.contains("雜誌") || trimmed.contains("學費") ||
            trimmed.contains("補習") || trimmed.contains("課程") -> "學習進修"

            trimmed.contains("請客") || trimmed.contains("紅包") || trimmed.contains("送禮") ||
            trimmed.contains("禮物") -> "社交人情"

            else -> "餐飲美食"
        }

        // 5. Clean merchant / note
        val cleanedNote = trimmed
            .replace(Regex("""\d+(\.\d+)?(元|塊|日圓|日幣|日元|美元|美金|歐元|韓元|韓幣|人民幣|港幣|英鎊|泰銖|台幣|新台幣)?"""), "")
            .replace("今天", "")
            .replace("昨天", "")
            .replace("花了", "")
            .replace("買了", "")
            .replace("吃了", "")
            .replace("記帳", "")
            .trim()

        return ParsedVoiceRecord(
            amount = amount,
            category = category,
            type = txType,
            merchantOrNote = if (cleanedNote.isNotBlank()) cleanedNote else trimmed,
            detectedCurrency = detectedCurrency
        )
    }

    private fun parseChineseNumber(str: String): Double? {
        val map = mapOf(
            '零' to 0, '一' to 1, '二' to 2, '兩' to 2, '三' to 3,
            '四' to 4, '五' to 5, '六' to 6, '七' to 7, '八' to 8, '九' to 9
        )
        // Check simple patterns like 一百八十, 兩百, 五十
        if (str.contains("百") || str.contains("十") || str.contains("千")) {
            var total = 0
            var temp = 0
            for (char in str) {
                val digit = map[char]
                if (digit != null) {
                    temp = digit
                } else if (char == '千') {
                    total += (if (temp == 0) 1 else temp) * 1000
                    temp = 0
                } else if (char == '百') {
                    total += (if (temp == 0) 1 else temp) * 100
                    temp = 0
                } else if (char == '十') {
                    total += (if (temp == 0) 1 else temp) * 10
                    temp = 0
                }
            }
            total += temp
            if (total > 0) return total.toDouble()
        }
        return null
    }
}
