package com.ipon.app.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ipon.app.data.local.IponDatabase
import com.ipon.app.data.repository.TransactionRepository
import com.ipon.app.ui.screens.addtransaction.AddTransactionViewModel
import com.ipon.app.ui.screens.insights.InsightsViewModel
import com.ipon.app.ui.screens.ledger.LedgerViewModel
import com.ipon.app.util.HapticCurrencyFeedback
import com.ipon.app.util.KeywordRuleMerchantClassifier

/**
 * Manual dependency container for the prototype. A real app would likely
 * reach for Hilt here, but a single object graph keeps this skeleton
 * dependency-free and easy to read end to end without generated code.
 */
class IponAppContainer(context: Context) {

    private val database = IponDatabase.getInstance(context)
    val repository = TransactionRepository(database.transactionDao())
    val merchantClassifier = KeywordRuleMerchantClassifier()
    val hapticFeedback = HapticCurrencyFeedback(context)
}

class IponViewModelFactory(private val container: IponAppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        LedgerViewModel::class.java -> LedgerViewModel(container.repository) as T
        AddTransactionViewModel::class.java -> AddTransactionViewModel(
            container.repository,
            container.merchantClassifier
        ) as T
        InsightsViewModel::class.java -> InsightsViewModel(container.repository) as T
        else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
