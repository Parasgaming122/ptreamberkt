package com.streambert.app.data.api

import com.streambert.app.data.model.AgeRating

// ── Age rating countries (matching Streambert exactly) ──
data class RatingCountry(val code: String, val label: String)

val RATING_COUNTRIES = listOf(
    RatingCountry("US", "United States (MPAA / TV Parental)"),
    RatingCountry("DE", "Germany (FSK)"),
    RatingCountry("GB", "United Kingdom (BBFC)"),
    RatingCountry("FR", "France (CNC)"),
    RatingCountry("AU", "Australia (ACB)"),
    RatingCountry("NZ", "New Zealand (OFLC)"),
    RatingCountry("BR", "Brazil (DEJUS)"),
    RatingCountry("CA", "Canada (CRTC)"),
    RatingCountry("JP", "Japan (EIRIN)"),
)

// Certification-to-minimum-age mappings
private val CERT_TO_AGE = mapOf(
    "US" to mapOf(
        "g" to 0, "nr" to 0, "not rated" to 0, "unrated" to 0,
        "tv-y" to 0, "tv-y7" to 7, "tv-g" to 0,
        "pg" to 7, "tv-pg" to 7, "pg-13" to 13, "tv-13" to 13, "tv-14" to 14,
        "r" to 17, "nc-17" to 18, "tv-ma" to 18, "x" to 18,
    ),
    "DE" to mapOf(
        "fsk 0" to 0, "0" to 0, "ab 0" to 0,
        "fsk 6" to 6, "6" to 6, "ab 6" to 6,
        "fsk 12" to 12, "12" to 12, "ab 12" to 12,
        "fsk 16" to 16, "16" to 16, "ab 16" to 16,
        "fsk 18" to 18, "18" to 18, "ab 18" to 18,
    ),
    "GB" to mapOf("u" to 0, "uc" to 0, "pg" to 7, "12a" to 12, "12" to 12, "15" to 15, "18" to 18, "r18" to 18),
    "FR" to mapOf("u" to 0, "g" to 0, "tous publics" to 0, "10" to 10, "12" to 12, "16" to 16, "18" to 18),
    "AU" to mapOf(
        "g" to 0, "pg" to 7, "m" to 15, "ma" to 15, "ma 15+" to 15, "ma15+" to 15,
        "r" to 18, "r 18+" to 18, "r18+" to 18, "x 18+" to 18, "x18+" to 18, "rc" to 18,
    ),
    "NZ" to mapOf("g" to 0, "pg" to 7, "m" to 0, "r13" to 13, "r15" to 15, "r16" to 16, "r18" to 18, "rp13" to 13, "rp16" to 16),
    "BR" to mapOf("l" to 0, "livre" to 0, "10" to 10, "12" to 12, "14" to 14, "16" to 16, "18" to 18),
    "CA" to mapOf("g" to 0, "pg" to 7, "14a" to 14, "18a" to 18, "r" to 18, "a" to 18, "13+" to 13, "16+" to 16, "18+" to 18),
    "JP" to mapOf("g" to 0, "pg12" to 12, "pg-12" to 12, "r15" to 15, "r-15" to 15, "r18" to 18, "r-18" to 18, "rz-18" to 18),
)

fun certToMinAge(cert: String?, countryCode: String): Int? {
    if (cert.isNullOrBlank()) return null
    val map = CERT_TO_AGE[countryCode] ?: CERT_TO_AGE["US"]!!
    val key = cert.trim().lowercase()
    if (key in map) return map[key]
    val stripped = key.replace("\\s+".toRegex(), "")
    for ((k, v) in map) {
        if (k.replace("\\s+".toRegex(), "") == stripped) return v
    }
    return null
}

fun isRestricted(contentMinAge: Int?, ageLimitSetting: Int?): Boolean {
    if (ageLimitSetting == null) return false
    if (contentMinAge == null) return false
    return contentMinAge > ageLimitSetting
}