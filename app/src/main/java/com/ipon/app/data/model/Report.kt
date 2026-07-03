package com.ipon.app.data.model

import com.ipon.app.data.local.ReportEntity

data class Report(
    val id: String,
    val periodYearMonth: String,
    val title: String,
    val content: String,
    val createdAtEpochMillis: Long
)

fun ReportEntity.toDomain(): Report = Report(
    id = id,
    periodYearMonth = periodYearMonth,
    title = title,
    content = content,
    createdAtEpochMillis = createdAtEpochMillis
)

fun Report.toEntity(): ReportEntity = ReportEntity(
    id = id,
    periodYearMonth = periodYearMonth,
    title = title,
    content = content,
    createdAtEpochMillis = createdAtEpochMillis
)
