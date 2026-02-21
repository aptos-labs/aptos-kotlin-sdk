package com.aptos.example.wallet.di

import com.aptos.example.wallet.data.repository.AccountRepository
import com.aptos.example.wallet.data.repository.AccountRepositoryImpl
import com.aptos.example.wallet.data.repository.TransactionRepository
import com.aptos.example.wallet.data.repository.TransactionRepositoryImpl
import com.aptos.example.wallet.data.storage.SecureStorage
import com.aptos.example.wallet.data.storage.SecureStorageImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    abstract fun bindSecureStorage(impl: SecureStorageImpl): SecureStorage

    @Binds
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    @Binds
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository
}
