package com.ipon.app.ui.screens.reflection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipon.app.data.local.MoodRating
import com.ipon.app.data.repository.DailyReflectionRepository
import com.ipon.app.data.repository.DayInReview
import com.ipon.app.util.Money
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ReflectionViewModel(private val repository: DailyReflectionRepository) : ViewModel() {

    private val todayKey: String = dayKeyFor(Calendar.getInstance())
    private val todayRange: Pair<Long, Long> = dayRangeMillisFor(Calendar.getInstance())

    val uiState: StateFlow<DayInReview> = repository
        .observeDayInReview(todayKey, todayRange.first, todayRange.second)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            DayInReview(todayKey, Money.ZERO, Money.ZERO, 0, null)
        )

    fun saveReflection(mood: MoodRating, note: String?) {
        viewModelScope.launch {
            repository.saveReflection(todayKey, mood, note)
        }
    }

    companion object {
        fun dayKeyFor(calendar: Calendar): String =
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)

        fun dayRangeMillisFor(calendar: Calendar): Pair<Long, Long> {
            val start = (calendar.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val end = (start.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, 1)
                add(Calendar.MILLISECOND, -1)
            }
            return start.timeInMillis to end.timeInMillis
        }
    }
}
