package com.dartdl.app.ui.page.settings.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dartdl.app.database.objects.CookieProfile
import com.dartdl.app.util.DatabaseUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.webkit.CookieManager
import com.dartdl.app.util.DownloadUtil
import com.dartdl.app.util.FileUtil
import com.dartdl.app.App.Companion.context
import com.dartdl.app.util.FileUtil.getCookiesFile
import com.dartdl.app.util.DownloadUtil.toCookiesFileContent

class CookiesViewModel : ViewModel() {
    companion object {
        const val NEW_PROFILE_ID = 0
    }

    data class ViewState(
        val editingCookieProfile: CookieProfile =
            CookieProfile(id = NEW_PROFILE_ID, url = "", content = "")
    )

    val cookiesFlow = DatabaseUtil.getCookiesFlow()

    private val mutableStateFlow = MutableStateFlow(ViewState())
    val stateFlow = mutableStateFlow.asStateFlow()
    
    private val _rawCookies = MutableStateFlow<List<Cookie>>(emptyList())
    val rawCookies: StateFlow<List<Cookie>> = _rawCookies.asStateFlow()
    private val state
        get() = stateFlow.value

    fun setEditingProfile(
        cookieProfile: CookieProfile =
            CookieProfile(id = NEW_PROFILE_ID, url = "https://www.google.com", content = "")
    ) {
        mutableStateFlow.update { it.copy(editingCookieProfile = cookieProfile) }
    }

    fun deleteCookieProfile(cookieProfile: CookieProfile = state.editingCookieProfile) {
        viewModelScope.launch(Dispatchers.IO) { DatabaseUtil.deleteCookieProfile(cookieProfile) }
    }

    fun generateNewCookies(content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            mutableStateFlow.update {
                val newProfile = it.editingCookieProfile.copy(content = content)
                DatabaseUtil.updateCookieProfile(newProfile)
                it.copy(editingCookieProfile = newProfile)
            }
        }
    }

    fun updateUrl(url: String) {
        setEditingProfile(cookieProfile = state.editingCookieProfile.copy(url = url))
    }

    fun updateContent(content: String) =
        mutableStateFlow.update {
            it.copy(editingCookieProfile = it.editingCookieProfile.copy(content = content))
        }

    fun updateCookieProfile(profile: CookieProfile = state.editingCookieProfile) {
        viewModelScope.launch(Dispatchers.IO) {
            if (profile.id == NEW_PROFILE_ID) {
                DatabaseUtil.insertCookieProfile(profile)
            } else {
                DatabaseUtil.updateCookieProfile(profile)
            }
            refreshRawCookies()
        }
    }

    fun refreshRawCookies() {
        viewModelScope.launch(Dispatchers.IO) {
            DownloadUtil.getCookieListFromDatabase().getOrNull()?.let {
                _rawCookies.value = it
                FileUtil.writeContentToFile(it.toCookiesFileContent(), context.getCookiesFile())
            }
        }
    }

    fun clearAll() {
        viewModelScope.launch(Dispatchers.IO) {
            // Clear WebView cookies
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
            
            // Clear managed profiles
            DatabaseUtil.clearAllCookieProfiles()
            
            _rawCookies.value = emptyList()
            FileUtil.writeContentToFile("", context.getCookiesFile())
        }
    }
}
