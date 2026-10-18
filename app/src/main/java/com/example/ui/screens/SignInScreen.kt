package com.example.ui.screens

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.AuthManager
import com.example.ui.components.ParchmentCard
import com.example.ui.components.PerforatedDivider
import com.example.ui.components.WaxSealBadge
import com.example.ui.theme.CourierPrimeFont
import com.example.ui.theme.EspressoInk
import com.example.ui.theme.EspressoMuted
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.ParchmentBg
import com.example.ui.theme.ParchmentBorder
import com.example.ui.theme.ParchmentCard
import com.example.ui.theme.ParchmentSurface
import com.example.ui.theme.PlayfairFont
import com.example.ui.theme.SealingWaxRed
import com.example.ui.theme.VintageGold
import com.example.ui.theme.WarmSepia

@Composable
fun SignInScreen(
    onAuthSuccess: () -> Unit,
    onGuestClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            ParchmentCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(16.dp)),
                backgroundColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Vintage Header Seal
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .shadow(4.dp, CircleShape)
                            .border(2.dp, VintageGold, CircleShape)
                            .background(WarmSepia.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = "Retro Ledger",
                            tint = WarmSepia,
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "時光帳本",
                        fontFamily = PlayfairFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "RETRO FINANCIAL LEDGER • EST. 1893",
                        fontFamily = CourierPrimeFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = VintageGold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    PerforatedDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "手帳紙質觸感 • 多錢包資產 • 智能預算 • 復古圖表\n登入後即可在所有裝置同步您的私人帳本。",
                        fontFamily = MaterialTheme.typography.bodyMedium.fontFamily,
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (errorMessage != null) {
                        Surface(
                            color = SealingWaxRed.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, SealingWaxRed.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                color = SealingWaxRed,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (context is Activity) {
                                isLoading = true
                                errorMessage = null
                                AuthManager.launchGoogleSignIn(
                                    activity = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        isLoading = false
                                        onAuthSuccess()
                                    },
                                    onAuthError = { err ->
                                        isLoading = false
                                        errorMessage = err
                                    },
                                    onAuthCancelled = {
                                        isLoading = false
                                    },
                                    scope = scope
                                )
                            }
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WarmSepia,
                            contentColor = ParchmentSurface
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("google_sign_in_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = ParchmentSurface,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "使用 Google 帳號安全開啟帳本",
                                    fontFamily = MaterialTheme.typography.titleMedium.fontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    androidx.compose.material3.OutlinedButton(
                        onClick = onGuestClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("guest_mode_button"),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.5.dp, WarmSepia),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = WarmSepia
                        )
                    ) {
                        Text(
                            text = "訪客模式快速體驗 (免登入)",
                            fontFamily = MaterialTheme.typography.titleMedium.fontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "本應用程式遵循嚴格端點安全規則，您的每一筆收支均獨立隔離加密保護。",
                        fontSize = 11.sp,
                        color = EspressoMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
