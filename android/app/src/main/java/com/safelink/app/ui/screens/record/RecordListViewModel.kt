package com.safelink.app.ui.screens.record

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.safelink.app.data.model.RiskLevel
import com.safelink.app.data.repository.RecordItem
import com.safelink.app.data.repository.RecordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 검사 기록 목록 상태 (Task 7.1).
 *
 * 기록은 기기 내 Room DB에서 Flow로 흘러오므로, 새 분석·진단이 저장되면 화면이 자동 갱신된다.
 */
class RecordListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RecordRepository(application)

    /** 위험도 필터 — null이면 전체 */
    private val _filter = MutableStateFlow<RiskLevel?>(null)
    val filter: StateFlow<RiskLevel?> = _filter.asStateFlow()

    /** 필터가 적용된 목록 — 화면에서는 이 값만 그리면 된다. */
    val records: StateFlow<List<RecordItem>> =
        combine(repository.observeRecords(), _filter) { list, level ->
            if (level == null) list else list.filter { it.riskLevel == level }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setFilter(level: RiskLevel?) {
        _filter.value = level
    }

    fun updateMemo(id: String, memo: String?) {
        viewModelScope.launch {
            runCatching { repository.updateMemo(id, memo?.takeIf { it.isNotBlank() }) }
        }
    }

    suspend fun findById(id: String): RecordItem? = runCatching { repository.findById(id) }.getOrNull()

    fun deleteAll() {
        viewModelScope.launch { runCatching { repository.deleteAll() } }
    }
}
