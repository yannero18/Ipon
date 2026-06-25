package com.ipon.app.util

import com.ipon.app.data.model.Category

/**
 * Classifies a raw merchant/SMS string (e.g. "STAMARIA-TODA-04") into a
 * [Category].
 *
 * HONEST SCOPE NOTE: Section 3 of the proposal describes this as a small
 * on-device foundation model. That requires a labeled dataset of real PH
 * receipt/SMS merchant strings, which does not exist yet. Building or
 * fine-tuning a model without that data would just be guessing.
 *
 * What's implemented here instead is a transparent, deterministic
 * keyword-rule classifier: cheap, explainable, and a reasonable seed for
 * common Philippine merchant patterns. It is meant to be replaced by an
 * actual trained classifier later -- the [MerchantClassifier] interface is
 * the seam where that swap happens, so the rest of the app (repository, UI)
 * does not need to change when a real model lands.
 */
interface MerchantClassifier {
    fun classify(rawMerchantString: String): Category?
}

class KeywordRuleMerchantClassifier : MerchantClassifier {

    private val rules: List<Pair<Regex, Category>> = listOf(
        Regex("toda|tricycle|jeep|grab|angkas|joyride|lrt|mrt|bus|taxi", RegexOption.IGNORE_CASE) to Category.TRANSPO,
        Regex("jollibee|mcdo|karenderia|restaurant|cafe|kfc|chowking|food|grabfood|foodpanda", RegexOption.IGNORE_CASE) to Category.FOOD,
        Regex("meralco|maynilad|pldt|globe|smart|converge|water|electric", RegexOption.IGNORE_CASE) to Category.BILLS,
        Regex("sari-sari|sm market|puregold|robinsons|grocery|supermarket", RegexOption.IGNORE_CASE) to Category.GROCERIES,
        Regex("shopee|lazada|sm mall|uniqlo|mall", RegexOption.IGNORE_CASE) to Category.SHOPPING,
        Regex("mercury|watsons|rose pharmacy|hospital|clinic|drug", RegexOption.IGNORE_CASE) to Category.HEALTH,
        Regex("netflix|spotify|cinema|sm cinema|gross", RegexOption.IGNORE_CASE) to Category.ENTERTAINMENT,
        Regex("payroll|salary|sweldo", RegexOption.IGNORE_CASE) to Category.SALARY
    )

    override fun classify(rawMerchantString: String): Category? {
        val normalized = rawMerchantString.trim()
        if (normalized.isEmpty()) return null
        return rules.firstOrNull { (pattern, _) -> pattern.containsMatchIn(normalized) }?.second
    }
}
