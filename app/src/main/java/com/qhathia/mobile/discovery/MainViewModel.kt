package com.qhathia.mobile.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.discovery.core.domain.usecase.ClearCartUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * App-shell ViewModel. Owns the cross-screen actions the NavHost needs but no single feature can own —
 * currently just clearing the cart when the off-Home inactivity timeout elapses.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val clearCartUseCase: ClearCartUseCase,
) : ViewModel() {

    /** Empties the cart (invoked by the off-Home inactivity timer). */
    fun clearCart() {
        viewModelScope.launch { clearCartUseCase() }
    }
}
