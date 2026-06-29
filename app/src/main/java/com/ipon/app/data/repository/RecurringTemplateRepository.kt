package com.ipon.app.data.repository

import androidx.room.withTransaction
import com.ipon.app.data.local.IponDatabase
import com.ipon.app.data.local.RecurringTemplateDao
import com.ipon.app.data.model.RecurringTemplate
import com.ipon.app.data.model.Transaction
import com.ipon.app.data.model.toDomain
import com.ipon.app.data.model.toEntity
import com.ipon.app.util.normalizeMerchantKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.UUID

class RecurringTemplateRepository(
    private val database: IponDatabase,
    private val dao: RecurringTemplateDao
) {

    fun observeAll(): Flow<List<RecurringTemplate>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeDue(calendar: Calendar = Calendar.getInstance()): Flow<List<RecurringTemplate>> =
        dao.observeActive().map { list ->
            list.map { it.toDomain() }.filter { it.isDueAsOf(calendar) }
        }

    /**
     * Normalized merchant keys of every existing template that has a
     * merchant string set, used by [com.ipon.app.data.model.detectRecurringPatterns]
     * to avoid re-suggesting a template for a merchant that's already
     * covered by one -- otherwise a confirmed Recurring template would
     * keep getting "want to turn this into a template?" nudges forever,
     * since its own confirmed transactions still show up in the ledger.
     */
    fun observeExistingMerchantKeys(): Flow<Set<String>> =
        observeAll().map { templates ->
            templates.mapNotNull { it.merchantRaw }
                .map { normalizeMerchantKey(it) }
                .filter { it.isNotEmpty() }
                .toSet()
        }

    suspend fun create(template: RecurringTemplate) {
        dao.insert(template.toEntity())
    }

    suspend fun update(template: RecurringTemplate) {
        dao.update(template.toEntity())
    }

    suspend fun delete(template: RecurringTemplate) {
        dao.delete(template.toEntity())
    }

    suspend fun setPaused(template: RecurringTemplate, paused: Boolean) {
        dao.update(template.copy(isPaused = paused).toEntity())
    }

    /**
     * Turns a due template into a real ledger transaction AND stamps the
     * template's [RecurringTemplate.lastConfirmedPeriodKey] for this period,
     * as one atomic database transaction -- if the app is killed mid-write,
     * we never end up with a logged transaction whose template still thinks
     * it's "due," which would just re-prompt the user for something they
     * already confirmed.
     */
    suspend fun confirmIntoTransaction(
        template: RecurringTemplate,
        calendar: Calendar = Calendar.getInstance(),
        occurredAtEpochMillis: Long = System.currentTimeMillis()
    ) {
        database.withTransaction {
            val transaction = Transaction(
                id = UUID.randomUUID().toString(),
                amount = template.amount,
                type = template.type,
                category = template.category.displayName,
                merchantRaw = template.merchantRaw,
                note = "Recurring: ${template.label}",
                occurredAtEpochMillis = occurredAtEpochMillis,
                isAutoCategorized = false
            )
            database.transactionDao().insert(transaction.toEntity())

            val updatedTemplate = template.copy(
                lastConfirmedPeriodKey = template.currentPeriodKey(calendar)
            )
            dao.update(updatedTemplate.toEntity())
        }
    }
}
