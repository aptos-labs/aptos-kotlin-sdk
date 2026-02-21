package com.aptos.example.wallet.data.repository

import com.aptos.example.wallet.data.storage.SecureStorage
import com.aptos.example.wallet.domain.model.Network
import com.aptos.sdk.Aptos
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.coEvery
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AccountRepositoryImplTest {

    private val secureStorage = mockk<SecureStorage>()
    private val aptosProvider = mockk<AptosProvider>()
    private val mockAptos = mockk<Aptos>()
    private lateinit var repository: AccountRepositoryImpl

    @BeforeEach
    fun setup() {
        repository = AccountRepositoryImpl(secureStorage, aptosProvider)
        every { aptosProvider.get(any()) } returns mockAptos
    }

    @Test
    fun `hasAccount delegates to storage`() {
        every { secureStorage.hasAccount() } returns true
        repository.hasAccount() shouldBe true

        every { secureStorage.hasAccount() } returns false
        repository.hasAccount() shouldBe false
    }

    @Test
    fun `createAccount generates and stores keys`() = runTest {
        justRun { secureStorage.saveMnemonic(any()) }
        justRun { secureStorage.savePrivateKeyHex(any()) }

        val account = repository.createAccount()

        account.address shouldStartWith "0x"
        account.balanceOctas shouldBe 0uL
        verify { secureStorage.saveMnemonic(any()) }
        verify { secureStorage.savePrivateKeyHex(any()) }
    }

    @Test
    fun `importAccount stores mnemonic and private key`() = runTest {
        justRun { secureStorage.saveMnemonic(any()) }
        justRun { secureStorage.savePrivateKeyHex(any()) }

        val phrase = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
        val account = repository.importAccount(phrase)

        account.address shouldStartWith "0x"
        verify { secureStorage.saveMnemonic(any()) }
        verify { secureStorage.savePrivateKeyHex(any()) }
    }

    @Test
    fun `getAccount loads from storage and fetches balance`() = runTest {
        justRun { secureStorage.saveMnemonic(any()) }
        justRun { secureStorage.savePrivateKeyHex(any()) }
        val created = repository.createAccount()

        every { secureStorage.getPrivateKeyHex() } returns "0x" + "ab".repeat(32)
        coEvery { mockAptos.getBalance(any()) } returns 500_000_000uL

        val account = repository.getAccount(Network.TESTNET)
        account.balanceOctas shouldBe 500_000_000uL
    }

    @Test
    fun `deleteAccount clears storage`() {
        justRun { secureStorage.clear() }

        repository.deleteAccount()

        verify { secureStorage.clear() }
    }

    @Test
    fun `getMnemonicPhrase delegates to storage`() {
        every { secureStorage.getMnemonic() } returns "test phrase"
        repository.getMnemonicPhrase() shouldBe "test phrase"
    }
}
