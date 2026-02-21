package com.aptos.example.wallet.ui.screen.send

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aptos.example.wallet.domain.model.SendResult
import com.aptos.example.wallet.ui.component.ErrorDialog
import com.aptos.example.wallet.ui.component.LoadingOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendScreen(
    onNavigateBack: () -> Unit,
    viewModel: SendViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    if (state.error != null) {
        ErrorDialog(message = state.error!!, onDismiss = viewModel::clearError)
    }

    if (state.result != null) {
        when (val result = state.result!!) {
            is SendResult.Success -> AlertDialog(
                onDismissRequest = {
                    viewModel.clearResult()
                    onNavigateBack()
                },
                title = { Text("Transaction Sent") },
                text = { Text("Hash: ${result.hash}") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.clearResult()
                        onNavigateBack()
                    }) { Text("OK") }
                },
            )
            is SendResult.Error -> AlertDialog(
                onDismissRequest = viewModel::clearResult,
                title = { Text("Transaction Failed") },
                text = { Text(result.message) },
                confirmButton = {
                    TextButton(onClick = viewModel::clearResult) { Text("OK") }
                },
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Send APT") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
            OutlinedTextField(
                value = state.recipientAddress,
                onValueChange = viewModel::updateRecipient,
                label = { Text("Recipient Address") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = {
                        val clipboard =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                        viewModel.updateRecipient(text)
                    }) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                    }
                },
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = state.amountApt,
                onValueChange = viewModel::updateAmount,
                label = { Text("Amount (APT)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = viewModel::send,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.isAddressValid && state.amountApt.isNotBlank() && !state.isSending,
            ) {
                Text(if (state.isSending) "Sending..." else "Confirm Send")
            }
        }

        if (state.isSending) {
            LoadingOverlay()
        }
    }
}
