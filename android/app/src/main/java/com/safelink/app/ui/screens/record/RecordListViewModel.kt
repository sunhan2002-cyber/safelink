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
import kotlinx.coroutines.flow.map
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

    /**
     * 보기 방식 — 최신순 / 상황별.
     * 탭을 옮겼다 돌아와도 고른 방식이 유지되도록 화면이 아니라 여기(액티비티 범위 ViewModel)에 둔다.
     */
    private val _viewMode = MutableStateFlow(RecordViewMode.LATEST)
    val viewMode: StateFlow<RecordViewMode> = _viewMode.asStateFlow()

    fun setViewMode(mode: RecordViewMode) {
        _viewMode.value = mode
    }

    /** 상황별로 묶은 목록 — 위험도 필터가 적용된 [records] 를 그대로 묶는다. */
    val groups: StateFlow<List<RecordGroup>> =
        records.map { groupBySituation(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * 펼쳐 둔 묶음. 처음에는 비어 있고, 화면은 이 값이 비어 있을 때 가장 위의 묶음 하나만 펼쳐 보여준다.
     * 사용자가 한 번이라도 접거나 펼치면 그 선택을 따른다.
     */
    private val _expandedGroups = MutableStateFlow<Set<String>?>(null)
    val expandedGroups: StateFlow<Set<String>?> = _expandedGroups.asStateFlow()

    fun toggleGroup(key: String, currentlyExpanded: Set<String>) {
        _expandedGroups.value = if (key in currentlyExpanded) currentlyExpanded - key else currentlyExpanded + key
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

    fun delete(id: String) {
        viewModelScope.launch { runCatching { repository.delete(id) } }
    }
}
