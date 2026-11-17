package com.example.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GlassCard
import com.example.ui.theme.HoloGradientButton
import com.example.ui.theme.HolographicBackdrop
import com.example.ui.theme.LuminousCyan
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.StatusPill
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold { innerPadding ->
        HolographicBackdrop(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 460.dp),
                    accentGradient = listOf(RoyalPurple, ElectricBlue, LuminousCyan, MetallicGold)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatusPill(label = "SINGLE ROOT OWNER CONSOLE", color = RoyalPurple)
                        Image(
                            painter = painterResource(id = R.drawable.img_pranav_auth_crest),
                            contentDescription = stringResource(id = R.string.cd_auth_crest),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(170.dp)
                                .clip(RoundedCornerShape(18.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            text = "PRANAV Control Center",
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Native multi-app governance, universal DEX hook verification, license & VIP orchestration, and encrypted Telegram bot management.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        if (errorMessage != null) {
                            Text(
                                text = errorMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = CrimsonError,
                                textAlign = TextAlign.Center
                            )
                        }

                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        } else {
                            HoloGradientButton(
                                text = stringResource(id = R.string.sign_in_google),
                                icon = Icons.Filled.VerifiedUser,
                                onClick = {
                                    errorMessage = null
                                    isLoading = true
                                    scope.launch {
                                        val res = GoogleSignInHelper.signInWithGoogle(context, credentialManager)
                                        isLoading = false
                                        res.fold(
                                            onSuccess = { onAuthSuccess() },
                                            onFailure = { err ->
                                                errorMessage = err.localizedMessage ?: "Google Sign-In failed."
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier.testTag("google_sign_in_button")
                            )
                        }

                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Zero-trust Firestore isolation • Hardware Keystore verification",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
