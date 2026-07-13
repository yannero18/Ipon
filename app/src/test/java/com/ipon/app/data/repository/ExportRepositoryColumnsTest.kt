package com.ipon.app.data.repository

import com.ipon.app.data.local.DebtEntity
import com.ipon.app.data.local.GoalEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.reflect.full.primaryConstructor

/**
 * ExportRepository's CSV headers are hand-written strings, not derived
 * from the entities they describe -- there's no compiler check tying
 * `"id,label,target,emoji,deadline,archived,image_uri"` to what
 * GoalEntity actually declares. That's exactly how the CSV export ended
 * up missing `imageUri` (added for the GoSave redesign) and then also
 * missing `feeMinorUnits`/`dueDate` (added for the discount-loan debt
 * feature) -- both landed on the entity without anyone remembering to
 * touch the CSV headers too.
 *
 * This test can't derive the CSV automatically, but it can make that
 * exact mistake loud: it counts each entity's fields via reflection and
 * checks against the CSV's known column count for that section. If you
 * add or remove a field on [DebtEntity] or [GoalEntity], this test breaks
 * until you consciously update both the count below AND
 * ExportRepository's matching header line -- the whole point is to make
 * "I forgot the CSV" fail a test instead of silently shipping.
 *
 * Both entities currently omit `createdAtEpochMillis` from their CSV
 * columns on purpose (same as Transaction's `date` column only exporting
 * `occurredAtEpochMillis`, not `createdAtEpochMillis`), hence the -1.
 */
class ExportRepositoryColumnsTest {

    @Test
    fun `DebtEntity field count matches DEBTS CSV column count, minus the omitted createdAt field`() {
        val fieldCount = DebtEntity::class.primaryConstructor?.parameters?.size
            ?: error("Could not reflect DebtEntity's constructor")
        val csvColumnCount = "id,label,original_balance,interest_rate,archived,fee,due_date".split(",").size

        assertEquals(
            "DebtEntity gained/lost a field -- check whether ExportRepository's DEBTS header needs updating too",
            fieldCount - 1,
            csvColumnCount
        )
    }

    @Test
    fun `GoalEntity field count matches GOALS CSV column count, minus the omitted createdAt field`() {
        val fieldCount = GoalEntity::class.primaryConstructor?.parameters?.size
            ?: error("Could not reflect GoalEntity's constructor")
        val csvColumnCount = "id,label,target,emoji,deadline,archived,image_uri".split(",").size

        assertEquals(
            "GoalEntity gained/lost a field -- check whether ExportRepository's GOALS header needs updating too",
            fieldCount - 1,
            csvColumnCount
        )
    }
}
