package com.ipon.app.data.repository

import com.ipon.app.data.local.IponDatabase

/**
 * App-wide data management, distinct from any single feature's repository.
 * Currently just wraps Room's built-in [androidx.room.RoomDatabase.clearAllTables],
 * but kept as its own repository (rather than reaching into IponDatabase
 * directly from a ViewModel) so this stays consistent with the rest of the
 * app's layering, and so future app-wide actions (e.g. a real export, once
 * that's built) have one obvious home.
 */
class AppDataRepository(private val database: IponDatabase) {

    /**
     * Permanently deletes every transaction, envelope, recurring template,
     * goal, contribution, reflection, AND learned category memory on this
     * device. There is no confirmation or undo at this layer -- the UI is
     * responsible for making sure the person really means it, since this
     * app has no cloud backup to fall back on by design (see the README's
     * privacy notes).
     *
     * clearAllTables() wipes every table registered in IponDatabase's
     * entities list automatically -- when MerchantCategoryMemoryEntity was
     * added, it started being covered by this call with no code change
     * needed here, which is exactly the point of using Room's built-in
     * method instead of hand-listing tables to clear.
     *
     * clearAllTables() is already synchronous/transactional internally in
     * Room, so this just needs to run off the main thread, which the
     * ViewModel's viewModelScope.launch (Dispatchers.Default by default for
     * non-IO work) already provides -- no extra withTransaction wrapping
     * needed or correct here.
     */
    fun clearAllData() {
        database.clearAllTables()
    }
}
