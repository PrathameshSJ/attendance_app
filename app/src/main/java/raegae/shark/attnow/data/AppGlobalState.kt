package raegae.shark.attnow.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppGlobalState {
    private val _isImporting = MutableStateFlow(false)
    val isImporting = _isImporting.asStateFlow()

    fun setImporting(importing: Boolean) {
        _isImporting.value = importing
    }

    private val _importResultLog = MutableStateFlow<String?>(null)
    val importResultLog = _importResultLog.asStateFlow()

    fun setImportResult(log: String) {
        _importResultLog.value = log
    }

    fun clearImportResult() {
        _importResultLog.value = null
    }
}
