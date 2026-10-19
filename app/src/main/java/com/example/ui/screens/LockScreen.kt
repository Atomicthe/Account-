package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Lock

@Composable
fun LockScreen(
    onUnlock: () -> Unit,
    activity: androidx.fragment.app.FragmentActivity,
    password: String?,
    isBiometricEnabled: Boolean
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var inputPassword by remember { mutableStateOf("") }
    
    LaunchedEffect(isBiometricEnabled) {
        if (isBiometricEnabled) {
            com.example.auth.BiometricHelper.authenticate(
                activity = activity,
                title = "指紋解鎖",
                subtitle = "請確認您的指紋",
                onSuccess = { onUnlock() },
                onError = { /* Handle error */ }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
            Text("應用程式已鎖定", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Lock,
                contentDescription = "Lock",
                modifier = Modifier.size(64.dp)
            )
            
            if (!password.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedTextField(
                    value = inputPassword,
                    onValueChange = { inputPassword = it },
                    label = { Text("請輸入密碼") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    if (inputPassword == password) {
                        onUnlock()
                    }
                }) {
                    Text("解鎖")
                }
            }
        }
    }
}
