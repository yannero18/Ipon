package com.ipon.app.util

import com.ipon.app.data.local.TransactionType
import com.ipon.app.data.model.ExpenseCategory
import com.ipon.app.data.model.IncomeCategory
import com.ipon.app.data.model.TransactionCategory

/**
 * Classifies a raw merchant/source string (e.g. "STAMARIA-TODA-04",
 * "PERA PADALA - GCASH", "SWELDO MAY") into a [TransactionCategory].
 *
 * HONEST SCOPE NOTE: Section 3 of the proposal describes this as a small
 * on-device foundation model. That requires a labeled dataset of real PH
 * receipt/SMS merchant strings, which does not exist yet. Building or
 * fine-tuning a model without that data would just be guessing.
 *
 * What's implemented here instead is a transparent, deterministic
 * keyword-rule classifier covering common Philippine expense AND income
 * patterns: jeepney/tricycle/Grab fares and toll/fuel, sari-sari stores and
 * wet markets, utility billers (Meralco, Maynilad, PLDT, Globe, cable,
 * condo/HOA dues), government fees and contributions (SSS, PhilHealth,
 * Pag-IBIG, BIR, LTO, barangay/NBI clearances), padala/remittance senders
 * (Palawan Express, Cebuana, LBC, GCash/Maya transfers), OFW remittance
 * receipts, sweldo/payroll deposits, freelance/gig income, and small-business
 * "tindahan" income. Rules are scoped per [TransactionType] so an expense
 * string can never accidentally match an income category and vice versa.
 *
 * This is meant to be replaced by an actual trained classifier later -- the
 * [MerchantClassifier] interface is the seam where that swap happens, so the
 * rest of the app (repository, UI) does not need to change when a real
 * model lands.
 */
interface MerchantClassifier {
    fun classify(rawMerchantString: String, type: TransactionType): TransactionCategory?
}

class KeywordRuleMerchantClassifier : MerchantClassifier {

    private val expenseRules: List<Pair<Regex, ExpenseCategory>> = listOf(
        Regex(
            "toda|tricycle|jeep|grab(?!food)|angkas|joyride|lrt|mrt|\\bbus\\b|taxi|padyak|habal|" +
                "tollway|toll fee|easytrip|autosweep|parking fee|gasoline|petron|shell|caltex|seaoil|jetti",
            RegexOption.IGNORE_CASE
        ) to ExpenseCategory.TRANSPO,
        Regex(
            "jollibee|mcdo|mang inasal|karenderia|restaurant|cafe|kfc|chowking|grabfood|foodpanda|" +
                "carinderia|turo-?turo|greenwich|shakeys|pizza|bonchon|max'?s|goldilocks|red ?ribbon|" +
                "starbucks|coffee|milktea|tea ?(shop|station)|inasal|lechon|samgyup|buffet|tapsilog|" +
                "silog|merienda|kainan",
            RegexOption.IGNORE_CASE
        ) to ExpenseCategory.FOOD,
        Regex(
            "meralco|maynilad|pldt|globe(?! gcash)|smart(?! padala)|converger|water bill|electric bill|" +
                "prepaid load|dito|skycable|cignal|cable tv|internet bill|wifi bill|postpaid bill|" +
                "load wallet|e-?load|condo dues|hoa dues|association dues|amortization|rent\\b",
            RegexOption.IGNORE_CASE
        ) to ExpenseCategory.BILLS,
        Regex(
            "sari-?sari|sm market|puregold|robinsons supermarket|grocery|supermarket|palengke|" +
                "talipapa|walter mart|south ?star|merkado|wet market|rustan'?s|landers|s&r|metro market",
            RegexOption.IGNORE_CASE
        ) to ExpenseCategory.GROCERIES,
        Regex(
            "shopee|lazada|sm mall|uniqlo|\\bmall\\b|tiangge|zalora|temu|tiktok shop|department store|" +
                "ukay-?ukay|bazaar|sm store|robinsons department|landmark|national bookstore",
            RegexOption.IGNORE_CASE
        ) to ExpenseCategory.SHOPPING,
        Regex(
            "mercury|watsons|rose pharmacy|hospital|clinic|botika|drug store|generika|st\\.? luke'?s|" +
                "makati med|dental|doctor'?s fee|consultation fee|laboratory fee|vitamins|medicine",
            RegexOption.IGNORE_CASE
        ) to ExpenseCategory.HEALTH,
        Regex(
            "netflix|spotify|cinema|sm cinema|ktv|videoke|disney\\+?|hbo|amazon prime|viu|iwantv|" +
                "concert ticket|movie ticket|game top-?up|steam|playstation|xbox|gcash games|bowling|" +
                "billiards|arcade",
            RegexOption.IGNORE_CASE
        ) to ExpenseCategory.ENTERTAINMENT,
        Regex(
            "tuition|school fee|matriculation|enrollment|book ?store|review center|review fee|" +
                "uniform fee|school supplies|project fee|thesis fee|graduation fee|board exam fee",
            RegexOption.IGNORE_CASE
        ) to ExpenseCategory.EDUCATION,
        Regex(
            "philhealth|\\bsss\\b|pag-?ibig|\\bbir\\b|nbi clearance|police clearance|barangay clearance|" +
                "cedula|community tax|business permit|mayor'?s permit|passport fee|drivers? license|" +
                "lto\\b|land transportation|real property tax|amilyar|government fees|gov'?t fee",
            RegexOption.IGNORE_CASE
        ) to ExpenseCategory.GOVERNMENT,
        Regex(
            "\\butang\\b|5-?6|loan payment|paid back|pautang|lending|pawnshop redeem|interest payment",
            RegexOption.IGNORE_CASE
        ) to ExpenseCategory.UTANG,
        Regex(
            "palawan express|cebuana|lbc padala|padala sent|remit(tance)? (sent|to)|" +
                "ml(?:huillier)?|western union sent|moneygram sent|padala (para sa|kay)",
            RegexOption.IGNORE_CASE
        ) to ExpenseCategory.PADALA
    )

    private val incomeRules: List<Pair<Regex, IncomeCategory>> = listOf(
        Regex(
            "payroll|salary|sweldo|13th month|payslip|backpay|overtime pay|bonus payout|allowance pay",
            RegexOption.IGNORE_CASE
        ) to IncomeCategory.SALARY,
        Regex(
            "freelance|sidelines|upwork|fiverr|project payment|gig|consultancy fee|commission|" +
                "honorarium|client payment",
            RegexOption.IGNORE_CASE
        ) to IncomeCategory.FREELANCE,
        Regex(
            "ofw|remit(tance)? (received|from)|padala (received|from)|western union|wise transfer in|" +
                "moneygram (received|from)|ria money|xoom",
            RegexOption.IGNORE_CASE
        ) to IncomeCategory.REMITTANCE,
        Regex(
            "tindahan|sari-?sari sales|online sell(ing)?|negosyo|store income|reseller (income|profit)|" +
                "shopee seller|lazada seller|tiktok shop (income|seller)|rental income",
            RegexOption.IGNORE_CASE
        ) to IncomeCategory.BUSINESS,
        Regex(
            "\\bgift\\b|abuloy|allowance|baon|aginaldo|pasalubong money|cash gift",
            RegexOption.IGNORE_CASE
        ) to IncomeCategory.GIFT
    )

    override fun classify(rawMerchantString: String, type: TransactionType): TransactionCategory? {
        val normalized = normalizeMerchantKey(rawMerchantString)
        if (normalized.isEmpty()) return null

        return when (type) {
            TransactionType.EXPENSE ->
                expenseRules.firstOrNull { (pattern, _) -> pattern.containsMatchIn(normalized) }?.second
            TransactionType.INCOME ->
                incomeRules.firstOrNull { (pattern, _) -> pattern.containsMatchIn(normalized) }?.second
        }
    }
}
