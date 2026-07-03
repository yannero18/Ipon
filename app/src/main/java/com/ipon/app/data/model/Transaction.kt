package com.ipon.app.data.model

import com.ipon.app.data.local.TransactionEntity
import com.ipon.app.data.local.TransactionType
import com.ipon.app.util.Money

data class Transaction(
    val id: String,
    val amount: Money,
    val type: TransactionType,
    val category: String,
    val merchantRaw: String?,
    val note: String?,
    val occurredAtEpochMillis: Long,
    val isAutoCategorized: Boolean
)

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    amount = Money.ofMinorUnits(amountMinorUnits),
    type = type,
    category = category,
    merchantRaw = merchantRaw,
    note = note,
    occurredAtEpochMillis = occurredAtEpochMillis,
    isAutoCategorized = isAutoCategorized
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    amountMinorUnits = amount.minorUnits,
    type = type,
    category = category,
    merchantRaw = merchantRaw,
    note = note,
    occurredAtEpochMillis = occurredAtEpochMillis,
    isAutoCategorized = isAutoCategorized
)
