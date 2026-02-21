package com.aptos.example.wallet.ui.screen.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aptos.example.wallet.ui.component.ErrorDialog
import com.aptos.example.wallet.ui.component.LoadingOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onAccountCreated: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.accountCreated) {
        if (state.accountCreated && state.mode == OnboardingMode.IMPORT) {
            onAccountCreated()
        }
    }

    if (state.error != null) {
        ErrorDialog(message = state.error!!, onDismiss = viewModel::clearError)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (state.mode) {
            OnboardingMode.CHOICE -> ChoiceContent(
                onCreateClick = viewModel::selectCreate,
                onImportClick = viewModel::selectImport,
            )
            OnboardingMode.CREATE -> CreateContent(
                mnemonic = state.generatedMnemonic,
                onContinue = onAccountCreated,
                onBack = viewModel::goBack,
            )
            OnboardingMode.IMPORT -> ImportContent(
                input = state.mnemonicInput,
                onInputChange = viewModel::updateMnemonicInput,
                onConfirm = viewModel::confirmImport,
                onBack = viewModel::goBack,
            )
        }
        if (state.isLoading) {
            LoadingOverlay()
        }
    }
}

@Composable
private fun ChoiceContent(
    onCreateClick: () -> Unit,
    onImportClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Aptos Wallet",
            style = MaterialTheme.typography.headlineLarge,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Manage your Aptos assets",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onCreateClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Create New Wallet")
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = onImportClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Import Existing Wallet")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateContent(
    mnemonic: String?,
    onContinue: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Back Up Mnemonic") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = "Write down these 12 words in order and keep them safe. " +
                    "You will need them to recover your wallet.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(24.dp))
            if (mnemonic != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Text(
                        text = mnemonic,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("I've Saved My Mnemonic")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportContent(
    input: String,
    onInputChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import Wallet") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
        ) {
            Text(
                text = "Enter your 12-word mnemonic phrase separated by spaces.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                label = { Text("Mnemonic phrase") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Import")
            }
        }
    }
}
