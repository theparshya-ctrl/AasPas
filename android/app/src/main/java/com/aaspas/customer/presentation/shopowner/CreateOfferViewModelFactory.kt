package com.aaspas.customer.presentation.shopowner



import android.app.Application

import androidx.lifecycle.ViewModel

import androidx.lifecycle.ViewModelProvider

import com.aaspas.customer.domain.repository.ShopOwnerRepository



class CreateOfferViewModelFactory(

    private val application: Application,

    private val repository: ShopOwnerRepository,

    private val shopId: String,

    private val shopName: String,

    private val shopPhotoUrl: String?,

    private val offerId: String?,

) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")

    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        if (modelClass.isAssignableFrom(CreateOfferViewModel::class.java)) {

            return CreateOfferViewModel(application, repository, shopId, shopName, shopPhotoUrl, offerId) as T

        }

        throw IllegalArgumentException("Unknown ViewModel class")

    }

}


