package com.aaspas.customer.presentation.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aaspas.customer.domain.repository.CategoryOffersRepository

class CategoryOffersViewModelFactory(
    private val repository: CategoryOffersRepository,
    private val categoryId: String,
    private val categoryName: String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CategoryOffersViewModel::class.java)) {
            return CategoryOffersViewModel(repository, categoryId, categoryName) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
