package com.lightship.safestring.ui.composable

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lightship.safestring.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel(),
    onItemClick: (String) -> Unit
) {
    val context = LocalContext.current
    val stringList by viewModel.stringList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val scope = rememberCoroutineScope()
    
    var showAddDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        authenticateWithBiometrics(
            context = context,
            onSuccess = {
                viewModel.loadStringList()
            },
            onError = { errorCode, errorMessage ->
                Toast.makeText(
                    context,
                    "Authentication required: $errorMessage",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Secure String Vault",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add secure string")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (stringList.isEmpty()) {
                Text(
                    text = "No secure strings stored yet.\nClick + to add one.",
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(stringList) { stringName ->
                        StringItem(
                            name = stringName,
                            onClick = { onItemClick(stringName) }
                        )
                    }
                }
            }
            
            if (showAddDialog) {
                AddStringDialog(
                    onDismissRequest = { showAddDialog = false },
                    onSave = { name, value ->
                        if (name.isNotBlank()) {
                            viewModel.saveString(name, value)
                            showAddDialog = false
                            scope.launch {
                                delay(300)  // Small delay for the UI to update
                                Toast.makeText(context, "String saved securely", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun StringItem(name: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

fun authenticateWithBiometrics(
    context: android.content.Context,
    onSuccess: () -> Unit,
    onError: (Int, String) -> Unit
) {
    val biometricManager = BiometricManager.from(context)
    
    when (biometricManager.canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
    )) {
        BiometricManager.BIOMETRIC_SUCCESS -> {
            val executor = ContextCompat.getMainExecutor(context)
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }
                
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errorCode, errString.toString())
                }
            }
            
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Authenticate to View Secure Strings")
                .setSubtitle("Use your biometric or device credential to continue")
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                            BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build()
            
            BiometricPrompt(context as androidx.fragment.app.FragmentActivity, executor, callback)
                .authenticate(promptInfo)
        }
        else -> {
            // If biometric authentication is not available, proceed without it
            Toast.makeText(
                context,
                "Biometric authentication not available. Using standard security.",
                Toast.LENGTH_SHORT
            ).show()
            onSuccess()
        }
    }
}