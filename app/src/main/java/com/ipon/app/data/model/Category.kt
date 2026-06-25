package com.ipon.app.data.model

/**
 * Starter category set. Real classification (Section 3 of the proposal) is
 * meant to come from a small on-device model trained on labeled PH
 * merchant/SMS strings -- that dataset does not exist yet, so
 * [com.ipon.app.util.MerchantClassifier] in this prototype uses a transparent
 * keyword-rule fallback instead of pretending to be a trained model. See that
 * file's header comment for the honest scope of what's implemented here.
 */
enum class Category(val displayName: String, val emoji: String) {
    TRANSPO("Transpo", "🛺"),
    FOOD("Food", "🍜"),
    BILLS("Bills", "💡"),
    GROCERIES("Groceries", "🛒"),
    SHOPPING("Shopping", "🛍️"),
    HEALTH("Health", "💊"),
    ENTERTAINMENT("Entertainment", "🎬"),
    SALARY("Salary", "💰"),
    OTHER("Other", "📝");

    companion object {
        fun fromDisplayName(name: String): Category =
            entries.find { it.displayName.equals(name, ignoreCase = true) } ?: OTHER
    }
}
