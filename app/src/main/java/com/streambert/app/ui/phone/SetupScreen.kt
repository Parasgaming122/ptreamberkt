package com.streambert.app.ui.phone

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.streambert.app.data.local.Prefs
import com.streambert.app.data.repository.MediaRepository

@Composable
fun SetupScreen(
    navController: NavController,
    onSetupComplete: () -> Unit = {},
) {
    val ctx = LocalContext.current
    var apiKey by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Logo/Title
        Text(
            "STREAMBERT",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp,
            ),
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Your personal streaming companion",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(48.dp))

        // API Key input
        Text(
            "Enter your TMDB API Read Access Token",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Go to themoviedb.org/settings/api → Create API Key → copy the \"API Read Access Token\"",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("API Read Access Token (Bearer)") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (apiKey.isBlank()) {
                    Toast.makeText(ctx, "Please enter a valid API Read Access Token", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                loading = true
                com.streambert.app.data.repository.MediaRepository.configureApi(apiKey, "en-US")
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    loading = false
                    com.streambert.app.data.local.Prefs.setTmdbKey(ctx, apiKey)
                    onSetupComplete()
                }, 500)
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = apiKey.isNotBlank() && !loading,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            if (loading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Text("Get Started", fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Skip option
        TextButton(onClick = { onSetupComplete() }) {
            Text("Skip for now", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}