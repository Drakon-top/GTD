package com.gtd.android.ui.workspace

val LUCIDE_TO_EMOJI = mapOf(
    "book-open" to "\uD83D\uDCDA",
    "briefcase" to "\uD83D\uDCBC",
    "home" to "\uD83C\uDFE0",
    "target" to "\uD83C\uDFAF",
    "activity" to "\uD83C\uDFC3",
    "palette" to "\uD83C\uDFA8",
    "music" to "\uD83C\uDFB5",
    "wallet" to "\uD83D\uDCB0",
    "shopping-cart" to "\uD83D\uDED2",
    "plane" to "✈\uFE0F",
    "wrench" to "\uD83D\uDD27",
    "graduation-cap" to "\uD83C\uDF93",
    "camera" to "\uD83D\uDCF7",
    "coffee" to "☕",
    "globe" to "\uD83C\uDF0D",
    "headphones" to "\uD83C\uDFA7",
    "laptop" to "\uD83D\uDCBB",
    "map" to "\uD83D\uDDFA\uFE0F",
    "phone" to "\uD83D\uDCDE",
    "scissors" to "✂\uFE0F",
    "smile" to "\uD83D\uDE04",
    "sun" to "☀\uFE0F",
    "umbrella" to "☂\uFE0F",
    "utensils" to "\uD83C\uDF74",
    "bike" to "\uD83D\uDEB2",
    "car" to "\uD83D\uDE97",
    "film" to "\uD83C\uDFAC",
    "gift" to "\uD83C\uDF81",
    "key" to "\uD83D\uDD11",
    "leaf" to "\uD83C\uDF3F",
    "mountain" to "⛰\uFE0F",
    "sparkles" to "✨",
    "dumbbell" to "\uD83C\uDFCB\uFE0F",
    "heart" to "❤\uFE0F",
    "star" to "⭐",
)

private val EMOJI_TO_LUCIDE = LUCIDE_TO_EMOJI.entries.associate { (k, v) -> v to k }

fun resolveCategoryIcon(icon: String?): String {
    if (icon == null) return "\uD83C\uDFF7\uFE0F"
    LUCIDE_TO_EMOJI[icon]?.let { return it }
    return icon.ifEmpty { "\uD83C\uDFF7\uFE0F" }
}

fun normalizeCategoryIconKey(icon: String?): String? {
    if (icon == null) return null
    if (icon in LUCIDE_TO_EMOJI) return icon
    EMOJI_TO_LUCIDE[icon]?.let { return it }
    return icon
}
