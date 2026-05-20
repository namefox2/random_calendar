package com.letsgo.randomcalendar.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ViewModelFactory(private vararg val args: Any) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return try {
            val constructors = modelClass.constructors
            val constructor = constructors.first { it.parameterTypes.size == args.size }
            constructor.newInstance(*args) as T
        } catch (e: Exception) {
            throw IllegalArgumentException("Cannot create ${modelClass.name}: ${e.message}", e)
        }
    }
}
