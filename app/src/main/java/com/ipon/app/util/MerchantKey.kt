package com.ipon.app.util

/**
 * Lowercase, trim, and collapse internal whitespace/punctuation noise so
 * "Jollibee", "jollibee ", and "JOLLIBEE!!" all map to the same key.
 * Deliberately simple -- this is normalization for exact-ish matching, not
 * fuzzy matching; "Jollibee SM Mall" and "Jollibee Ortigas" are still
 * treated as different merchants, which is correct (they're genuinely
 * different branches/contexts and a user might categorize them
 * differently).
 *
 * Lives in util/ (not inside CategoryMemoryRepository, where this used to
 * live) so both the repository layer (data/repository) and the model layer
 * (data/model, e.g. RecurringPatternSuggestion's detection logic) can use
 * the exact same normalization without the model layer having to depend on
 * a repository class just to call one pure string function.
 */
fun normalizeMerchantKey(raw: String): String =
    raw.trim()
        .lowercase()
        .replace(Regex("[^a-z0-9 ]"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
