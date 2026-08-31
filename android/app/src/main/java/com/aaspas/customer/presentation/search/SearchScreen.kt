package com.aaspas.customer.presentation.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aaspas.customer.AasPasApplication
import com.aaspas.customer.R
import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.core.location.AndroidLocationProvider
import com.aaspas.customer.domain.model.Category
import com.aaspas.customer.domain.model.SelectedLocation
import com.aaspas.customer.core.auth.SessionState
import com.aaspas.customer.presentation.components.EmptyState
import com.aaspas.customer.presentation.components.ErrorState
import com.aaspas.customer.presentation.components.OfferCard
import com.aaspas.customer.presentation.theme.AasPasColors
import com.aaspas.customer.presentation.theme.AasPasRadius
import com.aaspas.customer.presentation.theme.AasPasSizes
import com.aaspas.customer.presentation.theme.AasPasSpacing
import kotlinx.coroutines.launch

@Composable
fun SearchRoute(
    onOfferClick: (String) -> Unit,
    onNavigateToLogin: (() -> Unit) -> Unit,
) {
    val app = LocalContext.current.applicationContext as AasPasApplication
    val viewModel: SearchViewModel = viewModel(
        factory = SearchViewModelFactory(
            app.searchRepository,
            app.favoritesRepository,
            app.authRepository,
        ),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedLocation by app.selectedLocationStore.selectedLocation.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val locationProvider = AndroidLocationProvider(context)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val sessionState by app.authRepository.sessionState.collectAsStateWithLifecycle()
    var initialLocationApplied by remember { mutableStateOf(false) }

    fun applyLocation(location: SelectedLocation) {
        if (location.hasCoordinates) {
            viewModel.setLocation(location.latitude, location.longitude)
        } else if (locationProvider.hasLocationPermission()) {
            scope.launch {
                val coords = locationProvider.getLastLocation()
                viewModel.setLocation(coords?.latitude, coords?.longitude)
            }
        }
    }

    fun handleSaveError(error: AppError) {
        scope.launch {
            val message = when (error) {
                AppError.NoInternet -> context.getString(R.string.error_network)
                AppError.Timeout -> context.getString(R.string.error_timeout)
                else -> context.getString(R.string.error_server)
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    fun handleOfferSave(offerId: String) {
        viewModel.toggleOfferSave(offerId) { outcome ->
            when (outcome) {
                SearchViewModel.SaveToggleOutcome.NeedLogin -> {
                    onNavigateToLogin { viewModel.toggleOfferSave(offerId) {} }
                }
                is SearchViewModel.SaveToggleOutcome.Error -> handleSaveError(outcome.error)
                SearchViewModel.SaveToggleOutcome.Success -> Unit
            }
        }
    }

    LaunchedEffect(sessionState) {
        viewModel.onSessionChanged(sessionState == SessionState.LoggedIn)
    }

    LaunchedEffect(Unit) {
        viewModel.loadCategories()
        applyLocation(selectedLocation)
        initialLocationApplied = true
    }

    LaunchedEffect(selectedLocation) {
        if (!initialLocationApplied) return@LaunchedEffect
        applyLocation(selectedLocation)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SearchScreen(
            uiState = uiState,
            onQueryChange = viewModel::onQueryChange,
            onCategorySelected = viewModel::onCategorySelected,
            onRetry = viewModel::retry,
            onOfferClick = onOfferClick,
            onOfferSaveClick = ::handleOfferSave,
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    uiState: SearchUiState,
    onQueryChange: (String) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onRetry: () -> Unit,
    onOfferClick: (String) -> Unit,
    onOfferSaveClick: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.nav_search)) })
        },
        containerColor = AasPasColors.Background,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            SearchInputField(
                query = uiState.query,
                onQueryChange = onQueryChange,
                modifier = Modifier.padding(
                    horizontal = AasPasSpacing.lg,
                    vertical = AasPasSpacing.sm,
                ),
            )
            if (uiState.categories.isNotEmpty()) {
                CategoryFilterRow(
                    categories = uiState.categories,
                    selectedCategoryId = uiState.selectedCategoryId,
                    onCategorySelected = onCategorySelected,
                )
            }
            when (uiState.status) {
                SearchLoadStatus.Idle -> {
                    EmptyState(
                        title = stringResource(R.string.search_idle_title),
                        hint = stringResource(R.string.search_idle_hint),
                        modifier = Modifier.padding(AasPasSpacing.lg),
                    )
                }
                SearchLoadStatus.Loading -> SearchLoading()
                SearchLoadStatus.Empty -> EmptyState(
                    title = stringResource(R.string.search_empty_title),
                    hint = stringResource(R.string.search_empty_hint),
                    modifier = Modifier.padding(AasPasSpacing.lg),
                )
                SearchLoadStatus.Error -> ErrorState(
                    error = uiState.error,
                    onRetry = onRetry,
                    fallbackMessageResId = R.string.error_search,
                    modifier = Modifier.padding(AasPasSpacing.lg),
                )
                SearchLoadStatus.Loaded -> {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            horizontal = AasPasSpacing.lg,
                            vertical = AasPasSpacing.sm,
                        ),
                        verticalArrangement = Arrangement.spacedBy(AasPasSpacing.md),
                    ) {
                        item {
                            Text(
                                text = stringResource(R.string.search_results_count, uiState.total),
                                style = MaterialTheme.typography.bodyMedium,
                                color = AasPasColors.TextSecondary,
                            )
                        }
                        items(uiState.offers, key = { it.id }) { offer ->
                            OfferCard(
                                offer = offer,
                                onClick = { onOfferClick(offer.id) },
                                onSaveClick = { onOfferSaveClick(offer.id) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchInputField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .height(AasPasSizes.searchBarHeight + AasPasSpacing.sm),
        placeholder = {
            Text(
                text = stringResource(R.string.search_hint),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        leadingIcon = {
            androidx.compose.material3.Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = stringResource(R.string.search_hint),
                tint = AasPasColors.TextSecondary,
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(AasPasRadius.pill),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = AasPasColors.Surface,
            unfocusedContainerColor = AasPasColors.Surface,
            focusedBorderColor = AasPasColors.PurplePrimary,
            unfocusedBorderColor = AasPasColors.Border,
        ),
    )
}

@Composable
private fun CategoryFilterRow(
    categories: List<Category>,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = AasPasSpacing.lg),
        horizontalArrangement = Arrangement.spacedBy(AasPasSpacing.sm),
    ) {
        items(categories, key = { it.id }) { category ->
            val selected = category.id == selectedCategoryId
            AssistChip(
                onClick = { onCategorySelected(category.id) },
                label = { Text(text = category.name) },
                shape = RoundedCornerShape(AasPasRadius.pill),
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (selected) AasPasColors.PurplePrimary else AasPasColors.PurpleLight,
                    labelColor = if (selected) AasPasColors.White else AasPasColors.PurpleDark,
                ),
            )
        }
    }
}

@Composable
private fun SearchLoading() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = AasPasColors.PurplePrimary)
    }
}
