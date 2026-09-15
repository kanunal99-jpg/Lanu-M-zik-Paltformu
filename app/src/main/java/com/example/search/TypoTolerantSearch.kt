package com.example.search

/**
 * Provider-agnostic, offline-safe matcher for short music search queries.
 * It first uses substring matching, then a bounded edit-distance check to tolerate
 * small typos without turning search into an expensive full-text algorithm.
 */
object TypoTolerantSearch {
    fun matches(query: String, candidate: String): Boolean {
        val q = normalize(query)
        val c = normalize(candidate)
        if (q.isBlank()) return true
        if (c.contains(q)) return true
        if (q.length < 4) return false

        val tokens = c.split(Regex("\\s+"))
        return tokens.any { token ->
            val target = token.takeIf { it.length >= 4 } ?: return@any false
            val maxDistance = when {
                q.length <= 5 -> 1
                q.length <= 9 -> 2
                else -> 3
            }
            editDistanceBounded(q, target, maxDistance) <= maxDistance
        }
    }

    fun normalize(value: String): String = value.trim().lowercase()
        .replace('ı', 'i').replace('İ', 'i')
        .replace('ş', 's').replace('Ş', 's')
        .replace('ğ', 'g').replace('Ğ', 'g')
        .replace('ü', 'u').replace('Ü', 'u')
        .replace('ö', 'o').replace('Ö', 'o')
        .replace('ç', 'c').replace('Ç', 'c')

    private fun editDistanceBounded(a: String, b: String, bound: Int): Int {
        if (kotlin.math.abs(a.length - b.length) > bound) return bound + 1
        var previous = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            val current = IntArray(b.length + 1)
            current[0] = i
            var rowMin = current[0]
            for (j in 1..b.length) {
                current[j] = minOf(
                    previous[j] + 1,
                    current[j - 1] + 1,
                    previous[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1
                )
                rowMin = minOf(rowMin, current[j])
            }
            if (rowMin > bound) return bound + 1
            previous = current
        }
        return previous[b.length]
    }
}
