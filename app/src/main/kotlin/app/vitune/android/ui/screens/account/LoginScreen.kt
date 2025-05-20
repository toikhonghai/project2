package app.vitune.android.ui.screens.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.vitune.android.utils.semiBold
import app.vitune.android.utils.toast
import app.vitune.core.ui.LocalAppearance
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.media3.common.util.Log
import app.vitune.android.R
import app.vitune.android.ui.components.themed.IconButton
import androidx.compose.foundation.layout.size

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    Log.d("LoginScreen", "LoginScreen is being composed")
    val (colorPalette, typography) = LocalAppearance.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var showPassword by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Login",
            style = typography.xxl.semiBold,
            color = colorPalette.text
        )
        Spacer(modifier = Modifier.padding(16.dp))
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
            },
            label = { Text("Email") },
            textStyle = typography.xs,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        Spacer(modifier = Modifier.padding(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
            },
            label = { Text("Password") },
            textStyle = typography.xs,
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(
                    onClick = { showPassword = !showPassword },
                    icon = if (showPassword) R.drawable.visibility else R.drawable.visibility_off,
                    color = colorPalette.text,
                    modifier = Modifier.size(24.dp)
                )
            }
        )
        Spacer(modifier = Modifier.padding(16.dp))
        Button(
            onClick = {
                coroutineScope.launch {
                    isLoading = true
                    try {
                        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                            .await()
                        isLoading = false
                        onLoginSuccess()
                    } catch (e: Exception) {
                        isLoading = false
                        context.toast("Login failed: ${e.message}")
                    }
                }
            },
            enabled = !isLoading && email.isNotEmpty() && password.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login")
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(
            onClick = {
                Log.d("LoginScreen", "TextButton clicked: Navigating to RegisterScreen")
                onNavigateToRegister()  // Fixed: Actually calling the function instead of just referencing it
            }
        ) {
            Text("Don't have an account? Register")
        }
    }
}