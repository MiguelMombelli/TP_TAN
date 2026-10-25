package com.tan.presentation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SmokeViewModel @Inject constructor() : ViewModel() {
    val mensaje = "Hilt funciona"
}