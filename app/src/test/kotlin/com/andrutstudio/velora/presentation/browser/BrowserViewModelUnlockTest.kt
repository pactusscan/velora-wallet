package com.andrutstudio.velora.presentation.browser

import androidx.lifecycle.SavedStateHandle
import com.andrutstudio.velora.R
import com.andrutstudio.velora.presentation.components.UiText
import com.andrutstudio.velora.data.local.AppPreferences
import com.andrutstudio.velora.data.local.FaviconStore
import com.andrutstudio.velora.data.local.db.BrowserDao
import com.andrutstudio.velora.domain.model.Account
import com.andrutstudio.velora.domain.model.AccountType
import com.andrutstudio.velora.domain.model.Wallet
import com.andrutstudio.velora.domain.repository.WalletRepository
import com.andrutstudio.velora.domain.usecase.SendTransactionUseCase
import com.andrutstudio.velora.domain.model.Amount
import com.andrutstudio.velora.domain.model.AnchorInfo
import com.andrutstudio.velora.domain.model.AnchorRequest
import com.andrutstudio.velora.domain.model.RawTransaction
import com.andrutstudio.velora.domain.repository.BlockchainRepository
import com.andrutstudio.velora.domain.usecase.SendAnchorUseCase
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

private const val ESTIMATED_FEE = 10_000_000L

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
    private var builtAnchor: AnchorRequest? = null

    // A plain fake: mockk cannot return the Amount value class from a suspend call
    private val chain = object : BlockchainRepository by mockk<BlockchainRepository>(relaxed = true) {
        override suspend fun getAnchor(address: String): AnchorInfo? = null
        override suspend fun calculateAnchorFee(deposit: Amount) = Amount.fromNanoPac(ESTIMATED_FEE)
        override suspend fun buildAnchorTransaction(request: AnchorRequest): RawTransaction {
            builtAnchor = request
            return RawTransaction("00", "", 0, request.fee)
        }
        override suspend fun broadcastTransaction(signedHex: String) = "tx"
    }
    private val sendAnchor by lazy { SendAnchorUseCase(chain, walletRepository) }
    private val sendBond by lazy { com.andrutstudio.velora.domain.usecase.SendBondUseCase(chain, walletRepository) }

    private val appPreferences = mockk<AppPreferences>(relaxed = true) {
        coEvery { bookmarksSeeded() } returns true
    }

    private val faviconStore = mockk<FaviconStore>(relaxed = true) {
        every { icons } returns kotlinx.coroutines.flow.MutableStateFlow(emptyMap())
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() =
        BrowserViewModel(walletRepository, sendTransaction, sendAnchor, sendBond, chain, browserDao, appPreferences, faviconStore, SavedStateHandle())

    private fun signAnchorRequest(vm: BrowserViewModel, fee: String) =
        vm.onSignAnchorRequest("a1", "set", "ab".repeat(32), "", "0", "", fee, "", "")

    @Test
    fun `the anchor fee on the sheet is the one that gets signed`() {
        unlocked = true
        val vm = viewModel()
        signAnchorRequest(vm, "0")

        assertEquals(ESTIMATED_FEE, vm.state.value.pendingAnchorRequest?.fee?.nanoPac)
        vm.onConfirmSignAnchor()

        assertEquals(ESTIMATED_FEE, builtAnchor?.fee?.nanoPac)
    }

    @Test
    fun `an anchor fee named by the dApp is shown before signing`() {
        unlocked = true
        val vm = viewModel()
        signAnchorRequest(vm, "5000000000")

        assertEquals(5_000_000_000L, vm.state.value.pendingAnchorRequest?.fee?.nanoPac)
        assertEquals(null, builtAnchor)
    }

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
        assertEquals(UiText.Res(R.string.error_incorrect_password), vm.state.value.unlockError)
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
