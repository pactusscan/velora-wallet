package com.andrutstudio.velora.presentation.browser

import androidx.lifecycle.SavedStateHandle
import com.andrutstudio.velora.data.local.db.BrowserDao
import com.andrutstudio.velora.domain.model.Account
import com.andrutstudio.velora.domain.model.AccountType
import com.andrutstudio.velora.domain.model.Wallet
import com.andrutstudio.velora.domain.repository.WalletRepository
import com.andrutstudio.velora.domain.usecase.SendTransactionUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BrowserViewModelUnlockTest {

    private val account = Account("pc1rsigner", "Account 1", AccountType.ED25519, 0)
    private var unlocked = false

    private val walletRepository = mockk<WalletRepository>(relaxed = true) {
        every { observeWallet() } returns flowOf(Wallet(name = "Test", accounts = listOf(account)))
        every { isUnlocked() } answers { unlocked }
        every { isBiometricEnabled() } returns false
        coEvery { unlockWallet(any()) } answers {
            unlocked = firstArg<String>() == PASSWORD
            unlocked
        }
        coEvery { signMessage(any(), any()) } returns SIGNATURE
    }
    private val browserDao = mockk<BrowserDao>(relaxed = true) {
        every { observeHistory() } returns flowOf(emptyList())
        every { observeBookmarks() } returns flowOf(emptyList())
        coEvery { isSiteConnected(any()) } returns true
    }
    private val sendTransaction = mockk<SendTransactionUseCase>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() =
        BrowserViewModel(walletRepository, sendTransaction, browserDao, SavedStateHandle())

    @Test
    fun `confirming while locked opens the unlock dialog instead of signing`() {
        val vm = viewModel()
        vm.onSignMessageRequest("m1", "hello", "")

        vm.onConfirmSignMessage()

        assertTrue(vm.state.value.showUnlockDialog)
        assertNotNull(vm.state.value.pendingSignMessageRequest)
        coVerify(exactly = 0) { walletRepository.signMessage(any(), any()) }
    }

    @Test
    fun `correct password unlocks and resumes the signing`() = runTest {
        val vm = viewModel()
        val effects = mutableListOf<BrowserViewModel.Effect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.effect.collect { effects += it } }
        vm.onSignMessageRequest("m1", "hello", "")
        vm.onConfirmSignMessage()

        vm.onUnlock(PASSWORD)

        assertFalse(vm.state.value.showUnlockDialog)
        assertNull(vm.state.value.pendingSignMessageRequest)
        coVerify(exactly = 1) { walletRepository.signMessage("hello", account.address) }
        assertTrue(effects.contains(BrowserViewModel.Effect.ResolveSign("m1", SIGNATURE)))
    }

    @Test
    fun `wrong password keeps the dialog open with an error`() {
        val vm = viewModel()
        vm.onSignMessageRequest("m1", "hello", "")
        vm.onConfirmSignMessage()

        vm.onUnlock("wrong")

        assertTrue(vm.state.value.showUnlockDialog)
        assertEquals("Incorrect password", vm.state.value.unlockError)
        coVerify(exactly = 0) { walletRepository.signMessage(any(), any()) }
    }

    @Test
    fun `rejecting from the unlock dialog rejects the request`() = runTest {
        val vm = viewModel()
        val effects = mutableListOf<BrowserViewModel.Effect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.effect.collect { effects += it } }
        vm.onSignMessageRequest("m1", "hello", "")
        vm.onConfirmSignMessage()

        vm.onDismissUnlock()

        assertFalse(vm.state.value.showUnlockDialog)
        assertNull(vm.state.value.pendingSignMessageRequest)
        assertTrue(effects.contains(BrowserViewModel.Effect.RejectSign("m1", "User rejected")))
        coVerify(exactly = 0) { walletRepository.signMessage(any(), any()) }
    }

    @Test
    fun `transaction sign request also asks to unlock first`() {
        val vm = viewModel()
        vm.onSignRequest("r1", "pc1rto", "1000", "10", "")

        vm.onConfirmSign()

        assertTrue(vm.state.value.showUnlockDialog)
        coVerify(exactly = 0) { sendTransaction(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `unlocked wallet signs without the dialog`() {
        unlocked = true
        val vm = viewModel()
        vm.onSignMessageRequest("m1", "hello", "")

        vm.onConfirmSignMessage()

        assertFalse(vm.state.value.showUnlockDialog)
        coVerify(exactly = 1) { walletRepository.signMessage("hello", account.address) }
    }

    @Test
    fun `a new request while one is pending is rejected and cannot replace it`() = runTest {
        val vm = viewModel()
        val effects = mutableListOf<BrowserViewModel.Effect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.effect.collect { effects += it } }
        vm.onSignMessageRequest("m1", "hello", "")
        vm.onConfirmSignMessage()

        // The page fires another request behind the unlock dialog.
        vm.onSignMessageRequest("m2", "swapped", "")
        vm.onSignRequest("r2", "pc1rattacker", "1000", "10", "")
        assertTrue(effects.contains(BrowserViewModel.Effect.RejectSign("m2", BrowserViewModel.ERR_REQUEST_PENDING)))
        assertTrue(effects.contains(BrowserViewModel.Effect.RejectSign("r2", BrowserViewModel.ERR_REQUEST_PENDING)))
        assertNull(vm.state.value.pendingSignRequest)

        vm.onUnlock(PASSWORD)

        coVerify(exactly = 1) { walletRepository.signMessage("hello", account.address) }
        coVerify(exactly = 0) { walletRepository.signMessage("swapped", any()) }
        coVerify(exactly = 0) { sendTransaction(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `unlock does not resume when the approved request changed`() {
        val second = Account("pc1rsecond", "Account 2", AccountType.ED25519, 1)
        every { walletRepository.observeWallet() } returns
            flowOf(Wallet(name = "Test", accounts = listOf(account, second)))
        val vm = viewModel()
        vm.onSignMessageRequest("m1", "hello", "")
        vm.onConfirmSignMessage()

        vm.onSelectMessageSigner(second.address)
        vm.onUnlock(PASSWORD)

        assertFalse(vm.state.value.showUnlockDialog)
        assertNotNull(vm.state.value.pendingSignMessageRequest)
        coVerify(exactly = 0) { walletRepository.signMessage(any(), any()) }
    }

    private companion object {
        const val PASSWORD = "correct horse"
        const val SIGNATURE = "sig"
    }
}
