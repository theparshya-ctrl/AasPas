package com.aaspas.customer.presentation.search



import androidx.lifecycle.ViewModel

import androidx.lifecycle.ViewModelProvider

import com.aaspas.customer.domain.repository.AuthRepository

import com.aaspas.customer.domain.repository.FavoritesRepository

import com.aaspas.customer.domain.repository.SearchRepository



class SearchViewModelFactory(

    private val repository: SearchRepository,

    private val favoritesRepository: FavoritesRepository,

    private val authRepository: AuthRepository,

) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")

    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {

            return SearchViewModel(repository, favoritesRepository, authRepository) as T

        }

        throw IllegalArgumentException("Unknown ViewModel class")

    }

}


