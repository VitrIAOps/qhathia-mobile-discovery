package com.qhathia.mobile.discovery.feature.productdetail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.qhathia.mobile.discovery.core.domain.model.DetailErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.ShareOutcome
import com.qhathia.mobile.discovery.core.domain.model.ShareRejection
import com.qhathia.mobile.discovery.core.domain.model.DetailException
import com.qhathia.mobile.discovery.core.domain.usecase.AddToCartUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.GetProductDetailUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.GetProductSuggestionsUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveCartUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ShareQuoteUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ValidateConsumerNameUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ValidateConsumerPhoneUseCase
import com.qhathia.mobile.discovery.core.testing.FakeCartRepository
import com.qhathia.mobile.discovery.core.testing.FakeCartShareRepository
import com.qhathia.mobile.discovery.core.testing.FakeProductDetailRepository
import com.qhathia.mobile.discovery.core.testing.MainDispatcherExtension
import com.qhathia.mobile.discovery.core.testing.ProductDetailFixtures
import com.qhathia.mobile.discovery.feature.productdetail.model.ProductDetailUiEffect
import com.qhathia.mobile.discovery.feature.productdetail.model.ProductDetailUiEvent
import com.qhathia.mobile.discovery.feature.productdetail.model.ProductDetailUiState
import com.qhathia.mobile.discovery.feature.productdetail.navigation.ARG_PRODUCT_ID
import com.qhathia.mobile.discovery.feature.productdetail.ui.ProductDetailViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class ProductDetailViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private fun viewModel(
        productId: String = "p1",
        repository: FakeProductDetailRepository = FakeProductDetailRepository(),
        cartRepository: FakeCartRepository = FakeCartRepository(),
        shareRepository: FakeCartShareRepository = FakeCartShareRepository(),
    ) = ProductDetailViewModel(
        savedStateHandle = SavedStateHandle(mapOf(ARG_PRODUCT_ID to productId)),
        getProductDetail = GetProductDetailUseCase(repository),
        getSuggestions = GetProductSuggestionsUseCase(repository),
        observeCart = ObserveCartUseCase(cartRepository),
        addToCart = AddToCartUseCase(cartRepository),
        shareQuote = ShareQuoteUseCase(shareRepository),
        validateName = ValidateConsumerNameUseCase(),
        validatePhone = ValidateConsumerPhoneUseCase(),
    )

    /** A view model whose detail is loaded, so the share dialog can open. */
    private fun loadedViewModel(shareRepository: FakeCartShareRepository = FakeCartShareRepository()) =
        viewModel(
            repository = FakeProductDetailRepository(result = Result.success(ProductDetailFixtures.productDetail(id = "p1"))),
            shareRepository = shareRepository,
        )

    /** Opens the share dialog and enters valid consumer data. */
    private fun ProductDetailViewModel.openShareWithValidData() {
        onEvent(ProductDetailUiEvent.ShareWhatsApp)
        onEvent(ProductDetailUiEvent.ShareNameChanged("Ana Pérez"))
        onEvent(ProductDetailUiEvent.SharePhoneChanged("987 654 321"))
    }

    @Test
    fun given_product_when_initialized_then_loaded_with_related() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductDetailRepository(
            result = Result.success(ProductDetailFixtures.productDetail(id = "p1", name = "Taladro")),
            suggestionsResult = Result.success(ProductDetailFixtures.suggestions()),
        )
        val vm = viewModel(repository = repository)

        val status = vm.uiState.value.status
        assertTrue(status is ProductDetailUiState.Status.Loaded)
        assertEquals("Taladro", (status as ProductDetailUiState.Status.Loaded).detail.name)
        val related = vm.uiState.value.relatedProducts
        assertTrue(related is ProductDetailUiState.RelatedProducts.Loaded)
        assertTrue((related as ProductDetailUiState.RelatedProducts.Loaded).items.isNotEmpty())
        assertEquals(listOf("p1"), repository.requestedIds)
        assertEquals(listOf("p1"), repository.suggestionRequestedIds)
    }

    @Test
    fun given_empty_suggestions_when_initialized_then_related_empty() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductDetailRepository(suggestionsResult = Result.success(emptyList()))
        val vm = viewModel(repository = repository)

        assertEquals(ProductDetailUiState.RelatedProducts.Empty, vm.uiState.value.relatedProducts)
    }

    @Test
    fun given_suggestions_failure_when_initialized_then_related_empty() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductDetailRepository(
            result = Result.success(ProductDetailFixtures.productDetail()),
            suggestionsResult = Result.failure(DetailException(DetailErrorCategory.DetailApi)),
        )
        val vm = viewModel(repository = repository)

        // The detail still loads even though suggestions failed.
        assertTrue(vm.uiState.value.status is ProductDetailUiState.Status.Loaded)
        assertEquals(ProductDetailUiState.RelatedProducts.Empty, vm.uiState.value.relatedProducts)
    }

    @Test
    fun given_related_product_tapped_then_emits_navigate_to_detail() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()

        vm.uiEffect.test {
            vm.onEvent(ProductDetailUiEvent.RelatedProductTapped("rel-42"))
            assertEquals(ProductDetailUiEffect.NavigateToDetail("rel-42"), awaitItem())
        }
    }

    @Test
    fun given_related_product_add_to_cart_then_adds_that_product() = runTest(mainDispatcher.dispatcher) {
        val related = ProductDetailFixtures.suggestions(count = 3)
        val repository = FakeProductDetailRepository(suggestionsResult = Result.success(related))
        val cartRepository = FakeCartRepository()
        val vm = viewModel(repository = repository, cartRepository = cartRepository)

        val target = related.first()
        vm.onEvent(ProductDetailUiEvent.RelatedProductAddToCart(target.id))

        val line = cartRepository.current.items.single()
        assertEquals(target.id, line.productId)
        assertEquals(1, line.quantity)
    }

    @Test
    fun given_unknown_related_id_when_add_to_cart_then_cart_unchanged() = runTest(mainDispatcher.dispatcher) {
        val cartRepository = FakeCartRepository()
        val vm = viewModel(cartRepository = cartRepository)

        vm.onEvent(ProductDetailUiEvent.RelatedProductAddToCart("does-not-exist"))

        assertTrue(cartRepository.current.items.isEmpty())
    }

    @Test
    fun given_404_when_initialized_then_not_found_status() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductDetailRepository(
            result = Result.failure(DetailException(DetailErrorCategory.NotFound)),
        )
        val vm = viewModel(repository = repository)

        val status = vm.uiState.value.status
        assertEquals(ProductDetailUiState.ErrorKind.NotFound, (status as ProductDetailUiState.Status.Error).kind)
    }

    @Test
    fun given_network_error_when_initialized_then_network_status() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductDetailRepository(
            result = Result.failure(DetailException(DetailErrorCategory.Network)),
        )
        val vm = viewModel(repository = repository)

        val status = vm.uiState.value.status
        assertEquals(ProductDetailUiState.ErrorKind.Network, (status as ProductDetailUiState.Status.Error).kind)
    }

    @Test
    fun given_error_when_retry_succeeds_then_loaded() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductDetailRepository(
            result = Result.failure(DetailException(DetailErrorCategory.DetailApi)),
        )
        val vm = viewModel(repository = repository)
        assertTrue(vm.uiState.value.status is ProductDetailUiState.Status.Error)

        repository.result = Result.success(ProductDetailFixtures.productDetail())
        vm.onEvent(ProductDetailUiEvent.Retry)

        assertTrue(vm.uiState.value.status is ProductDetailUiState.Status.Loaded)
        assertEquals(2, repository.callCount)
    }

    @Test
    fun given_back_when_tapped_then_emits_navigate_back() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()

        vm.uiEffect.test {
            vm.onEvent(ProductDetailUiEvent.Back)
            assertEquals(ProductDetailUiEffect.NavigateBack, awaitItem())
        }
    }

    @Test
    fun given_confirm_new_search_then_emits_navigate_home_idle() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()

        vm.uiEffect.test {
            vm.onEvent(ProductDetailUiEvent.NewSearch)
            assertTrue(vm.uiState.value.showCloseConfirmDialog)
            vm.onEvent(ProductDetailUiEvent.ConfirmCloseSearch)
            assertEquals(ProductDetailUiEffect.NavigateHomeIdle, awaitItem())
        }
        assertFalse(vm.uiState.value.showCloseConfirmDialog)
    }

    @Test
    fun given_dialog_when_dismissed_then_hidden() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()

        vm.onEvent(ProductDetailUiEvent.NewSearch)
        vm.onEvent(ProductDetailUiEvent.DismissDialog)

        assertFalse(vm.uiState.value.showCloseConfirmDialog)
    }

    @Test
    fun given_quantity_increased_then_add_to_cart_uses_quantity() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductDetailRepository(
            result = Result.success(ProductDetailFixtures.productDetail(id = "p1")),
        )
        val cartRepository = FakeCartRepository()
        val vm = viewModel(repository = repository, cartRepository = cartRepository)

        vm.onEvent(ProductDetailUiEvent.IncreaseQuantity)
        vm.onEvent(ProductDetailUiEvent.AddToCart)

        val line = cartRepository.current.items.single()
        assertEquals("p1", line.productId)
        assertEquals(2, line.quantity)
        assertEquals(2, vm.uiState.value.cartItemCount)
    }

    @Test
    fun given_consult_stock_when_increased_then_quantity_capped_at_one() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductDetailRepository(
            result = Result.success(ProductDetailFixtures.productDetail(availability = "CONSULT")),
        )
        val vm = viewModel(repository = repository)

        vm.onEvent(ProductDetailUiEvent.IncreaseQuantity)

        assertEquals(1, vm.uiState.value.quantity) // CONSULT caps at 1
    }

    @Test
    fun given_quantity_at_one_when_decreased_then_stays_one() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel(
            repository = FakeProductDetailRepository(result = Result.success(ProductDetailFixtures.productDetail())),
        )

        vm.onEvent(ProductDetailUiEvent.DecreaseQuantity)

        assertEquals(1, vm.uiState.value.quantity)
    }

    @Test
    fun given_open_cart_then_emits_open_cart_effect() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel(
            repository = FakeProductDetailRepository(result = Result.success(ProductDetailFixtures.productDetail())),
        )

        vm.uiEffect.test {
            vm.onEvent(ProductDetailUiEvent.OpenCart)
            assertEquals(ProductDetailUiEffect.OpenCart, awaitItem())
        }
    }

    // --- Share-quote dialog ---

    @Test
    fun given_detail_not_loaded_when_share_tapped_then_dialog_stays_hidden() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel(
            repository = FakeProductDetailRepository(result = Result.failure(DetailException(DetailErrorCategory.DetailApi))),
        )

        vm.onEvent(ProductDetailUiEvent.ShareWhatsApp)

        assertFalse(vm.uiState.value.share.visible)
    }

    @Test
    fun given_loaded_when_share_tapped_then_dialog_opens() = runTest(mainDispatcher.dispatcher) {
        val vm = loadedViewModel()

        vm.onEvent(ProductDetailUiEvent.ShareWhatsApp)

        assertTrue(vm.uiState.value.share.visible)
        assertFalse(vm.uiState.value.share.isShareEnabled) // empty form
    }

    @Test
    fun given_invalid_then_valid_data_when_entered_then_share_enables() = runTest(mainDispatcher.dispatcher) {
        val vm = loadedViewModel()
        vm.onEvent(ProductDetailUiEvent.ShareWhatsApp)

        vm.onEvent(ProductDetailUiEvent.ShareNameChanged("Ana"))
        vm.onEvent(ProductDetailUiEvent.SharePhoneChanged("123"))
        assertFalse(vm.uiState.value.share.isShareEnabled) // phone invalid

        vm.onEvent(ProductDetailUiEvent.SharePhoneChanged("987 654 321"))
        assertTrue(vm.uiState.value.share.isShareEnabled)
    }

    @Test
    fun given_valid_data_when_confirm_share_then_success_with_this_product() = runTest(mainDispatcher.dispatcher) {
        val shareRepository = FakeCartShareRepository()
        val vm = loadedViewModel(shareRepository)
        vm.openShareWithValidData()

        vm.onEvent(ProductDetailUiEvent.ConfirmShare)

        val submission = vm.uiState.value.share.submission
        assertTrue(submission is ProductDetailUiState.ShareSubmission.Success)
        assertEquals(1, (submission as ProductDetailUiState.ShareSubmission.Success).summary.size)
        assertEquals(1, shareRepository.callCount)
        assertEquals(listOf("p1"), shareRepository.lastCall?.items?.map { it.productId })
        // The ViewModel forwards the raw form value; the data layer normalizes it before the API.
        assertEquals("987 654 321", shareRepository.lastCall?.phone)
    }

    @Test
    fun given_share_fails_when_confirm_then_error_after_retries() = runTest(mainDispatcher.dispatcher) {
        val shareRepository = FakeCartShareRepository().apply { failWith() }
        val vm = loadedViewModel(shareRepository)
        vm.openShareWithValidData()

        vm.onEvent(ProductDetailUiEvent.ConfirmShare)

        assertEquals(ProductDetailUiState.ShareSubmission.Error, vm.uiState.value.share.submission)
        assertEquals(3, shareRepository.callCount) // retried up to the max
    }

    @Test
    fun given_share_answers_failed_when_confirm_then_not_delivered_and_no_retry() =
        runTest(mainDispatcher.dispatcher) {
            val shareRepository = FakeCartShareRepository().apply { notDelivered("FAILED") }
            val vm = loadedViewModel(shareRepository)
            vm.openShareWithValidData()

            vm.onEvent(ProductDetailUiEvent.ConfirmShare)

            assertEquals(ProductDetailUiState.ShareSubmission.NotDelivered, vm.uiState.value.share.submission)
            assertEquals(1, shareRepository.callCount)
        }

    @Test
    fun given_share_rejected_400_when_confirm_then_error_sent_once() = runTest(mainDispatcher.dispatcher) {
        val shareRepository = FakeCartShareRepository(ShareOutcome.Rejected(ShareRejection.Unavailable))
        val vm = loadedViewModel(shareRepository)
        vm.openShareWithValidData()

        vm.onEvent(ProductDetailUiEvent.ConfirmShare)

        assertEquals(ProductDetailUiState.ShareSubmission.Error, vm.uiState.value.share.submission)
        assertEquals(1, shareRepository.callCount)
    }

    @Test
    fun given_invalid_data_when_confirm_share_then_no_submission() = runTest(mainDispatcher.dispatcher) {
        val shareRepository = FakeCartShareRepository()
        val vm = loadedViewModel(shareRepository)
        vm.onEvent(ProductDetailUiEvent.ShareWhatsApp) // opened, but no data entered

        vm.onEvent(ProductDetailUiEvent.ConfirmShare)

        assertEquals(ProductDetailUiState.ShareSubmission.Idle, vm.uiState.value.share.submission)
        assertEquals(0, shareRepository.callCount)
    }

    @Test
    fun given_success_when_accept_then_dialog_closes_and_form_resets() = runTest(mainDispatcher.dispatcher) {
        val vm = loadedViewModel()
        vm.openShareWithValidData()
        vm.onEvent(ProductDetailUiEvent.ConfirmShare)
        assertTrue(vm.uiState.value.share.submission is ProductDetailUiState.ShareSubmission.Success)

        vm.onEvent(ProductDetailUiEvent.AcceptShareSuccess)

        val share = vm.uiState.value.share
        assertFalse(share.visible)
        assertEquals("", share.name)
        assertEquals("", share.phone)
        assertEquals(ProductDetailUiState.ShareSubmission.Idle, share.submission)
    }

    @Test
    fun given_error_when_dismissed_then_returns_to_form() = runTest(mainDispatcher.dispatcher) {
        val vm = loadedViewModel(FakeCartShareRepository().apply { failWith() })
        vm.openShareWithValidData()
        vm.onEvent(ProductDetailUiEvent.ConfirmShare)
        assertEquals(ProductDetailUiState.ShareSubmission.Error, vm.uiState.value.share.submission)

        vm.onEvent(ProductDetailUiEvent.DismissShareError)

        val share = vm.uiState.value.share
        assertTrue(share.visible) // still in the dialog
        assertEquals(ProductDetailUiState.ShareSubmission.Idle, share.submission)
        assertEquals("Ana Pérez", share.name) // entered data preserved for the retry
    }
}
