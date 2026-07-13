package com.ipon.app.data.backup

import com.ipon.app.data.local.DailyReflectionEntity
import com.ipon.app.data.local.DebtEntity
import com.ipon.app.data.local.DebtPaymentEntity
import com.ipon.app.data.local.EnvelopeEntity
import com.ipon.app.data.local.GoalContributionEntity
import com.ipon.app.data.local.GoalEntity
import com.ipon.app.data.local.MerchantCategoryMemoryEntity
import com.ipon.app.data.local.RecurringTemplateEntity
import com.ipon.app.data.local.ReportEntity
import com.ipon.app.data.local.TransactionEntity

fun TransactionEntity.toBackupDto() = TransactionBackupDto(
    id = id, amountMinorUnits = amountMinorUnits, type = type, category = category,
    merchantRaw = merchantRaw, note = note, occurredAtEpochMillis = occurredAtEpochMillis,
    createdAtEpochMillis = createdAtEpochMillis, isAutoCategorized = isAutoCategorized
)

fun TransactionBackupDto.toEntity() = TransactionEntity(
    id = id, amountMinorUnits = amountMinorUnits, type = type, category = category,
    merchantRaw = merchantRaw, note = note, occurredAtEpochMillis = occurredAtEpochMillis,
    createdAtEpochMillis = createdAtEpochMillis, isAutoCategorized = isAutoCategorized
)

fun EnvelopeEntity.toBackupDto() = EnvelopeBackupDto(
    id = id, category = category, periodYearMonth = periodYearMonth, capMinorUnits = capMinorUnits,
    createdAtEpochMillis = createdAtEpochMillis, priority = priority, customIcon = customIcon
)

fun EnvelopeBackupDto.toEntity() = EnvelopeEntity(
    id = id, category = category, periodYearMonth = periodYearMonth, capMinorUnits = capMinorUnits,
    createdAtEpochMillis = createdAtEpochMillis, priority = priority, customIcon = customIcon
)

fun RecurringTemplateEntity.toBackupDto() = RecurringTemplateBackupDto(
    id = id, label = label, amountMinorUnits = amountMinorUnits, type = type, category = category,
    merchantRaw = merchantRaw, frequency = frequency, dayOfPeriod = dayOfPeriod,
    lastConfirmedPeriodKey = lastConfirmedPeriodKey, isPaused = isPaused, createdAtEpochMillis = createdAtEpochMillis
)

fun RecurringTemplateBackupDto.toEntity() = RecurringTemplateEntity(
    id = id, label = label, amountMinorUnits = amountMinorUnits, type = type, category = category,
    merchantRaw = merchantRaw, frequency = frequency, dayOfPeriod = dayOfPeriod,
    lastConfirmedPeriodKey = lastConfirmedPeriodKey, isPaused = isPaused, createdAtEpochMillis = createdAtEpochMillis
)

fun GoalEntity.toBackupDto() = GoalBackupDto(
    id = id, label = label, targetMinorUnits = targetMinorUnits, emoji = emoji,
    isArchived = isArchived, deadline = deadline, createdAtEpochMillis = createdAtEpochMillis, imageUri = imageUri
)

fun GoalBackupDto.toEntity() = GoalEntity(
    id = id, label = label, targetMinorUnits = targetMinorUnits, emoji = emoji,
    isArchived = isArchived, deadline = deadline, createdAtEpochMillis = createdAtEpochMillis, imageUri = imageUri
)

fun GoalContributionEntity.toBackupDto() = GoalContributionBackupDto(
    id = id, goalId = goalId, amountMinorUnits = amountMinorUnits, note = note,
    contributedAtEpochMillis = contributedAtEpochMillis
)

fun GoalContributionBackupDto.toEntity() = GoalContributionEntity(
    id = id, goalId = goalId, amountMinorUnits = amountMinorUnits, note = note,
    contributedAtEpochMillis = contributedAtEpochMillis
)

fun DebtEntity.toBackupDto() = DebtBackupDto(
    id = id, label = label, originalBalanceMinorUnits = originalBalanceMinorUnits,
    interestRatePercent = interestRatePercent, isArchived = isArchived, createdAtEpochMillis = createdAtEpochMillis
)

fun DebtBackupDto.toEntity() = DebtEntity(
    id = id, label = label, originalBalanceMinorUnits = originalBalanceMinorUnits,
    interestRatePercent = interestRatePercent, isArchived = isArchived, createdAtEpochMillis = createdAtEpochMillis
)

fun DebtPaymentEntity.toBackupDto() = DebtPaymentBackupDto(
    id = id, debtId = debtId, amountMinorUnits = amountMinorUnits, paidAtEpochMillis = paidAtEpochMillis
)

fun DebtPaymentBackupDto.toEntity() = DebtPaymentEntity(
    id = id, debtId = debtId, amountMinorUnits = amountMinorUnits, paidAtEpochMillis = paidAtEpochMillis
)

fun DailyReflectionEntity.toBackupDto() = DailyReflectionBackupDto(
    id = id, dayKey = dayKey, mood = mood, note = note, createdAtEpochMillis = createdAtEpochMillis
)

fun DailyReflectionBackupDto.toEntity() = DailyReflectionEntity(
    id = id, dayKey = dayKey, mood = mood, note = note, createdAtEpochMillis = createdAtEpochMillis
)

fun MerchantCategoryMemoryEntity.toBackupDto() = MerchantCategoryMemoryBackupDto(
    merchantKey = merchantKey, type = type, category = category,
    confirmCount = confirmCount, lastUsedEpochMillis = lastUsedEpochMillis
)

fun MerchantCategoryMemoryBackupDto.toEntity() = MerchantCategoryMemoryEntity(
    merchantKey = merchantKey, type = type, category = category,
    confirmCount = confirmCount, lastUsedEpochMillis = lastUsedEpochMillis
)

fun ReportEntity.toBackupDto() = ReportBackupDto(
    id = id, periodYearMonth = periodYearMonth, title = title, content = content, createdAtEpochMillis = createdAtEpochMillis
)

fun ReportBackupDto.toEntity() = ReportEntity(
    id = id, periodYearMonth = periodYearMonth, title = title, content = content, createdAtEpochMillis = createdAtEpochMillis
)
