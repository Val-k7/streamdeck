package com.androidcontroldeck.data.feedback

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class FeedbackReport(
    val rating: Int,
    val comment: String,
    val includeLogs: Boolean,
    val diagnostics: String,
    val createdAt: Long = System.currentTimeMillis(),
)

class FeedbackRepository {
    private val reports = MutableStateFlow<List<FeedbackReport>>(emptyList())
    val latestReports: StateFlow<List<FeedbackReport>> = reports.asStateFlow()

    suspend fun submit(report: FeedbackReport) {
        reports.update { current -> current + report }
    }
}
