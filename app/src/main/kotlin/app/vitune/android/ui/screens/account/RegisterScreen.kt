package app.vitune.android.ui.screens.account

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource // Import này cần thiết
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vitune.android.R // Import R class của dự án
import app.vitune.android.utils.semiBold
import app.vitune.android.utils.toast
import app.vitune.core.ui.LocalAppearance
import app.vitune.core.ui.surface
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val (colorPalette, typography) = LocalAppearance.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPassword by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(color = colorPalette.background0),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 })
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "ViTune",
                        fontSize = 64.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFBB86FC),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = colorPalette.surface.copy(alpha = 0.9f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.register),
                        style = typography.xxl.semiBold,
                        color = colorPalette.text,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(stringResource(R.string.email)) },
                        textStyle = typography.xs,
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = stringResource(R.string.email), tint = colorPalette.text) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        isError = errorMessage != null,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colorPalette.text,
                            unfocusedBorderColor = colorPalette.text.copy(alpha = 0.5f),
                            focusedLabelColor = colorPalette.text,
                            unfocusedLabelColor = colorPalette.text.copy(alpha = 0.7f),
                            unfocusedTextColor = colorPalette.text,
                            focusedTextColor = colorPalette.text,
                            cursorColor = colorPalette.text,
                            errorBorderColor = colorPalette.red,
                            errorLabelColor = colorPalette.red
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.password)) },
                        textStyle = typography.xs,
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = stringResource(R.string.password), tint = colorPalette.text) },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = errorMessage != null,
                        trailingIcon = {
                            IconButton(
                                onClick = { showPassword = !showPassword },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = if (showPassword) R.drawable.visibility else R.drawable.visibility_off),
                                    contentDescription = if (showPassword) "Hide password" else "Show password",
                                    tint = colorPalette.text.copy(alpha = 0.7f)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colorPalette.text,
                            unfocusedBorderColor = colorPalette.text.copy(alpha = 0.5f),
                            focusedLabelColor = colorPalette.text,
                            unfocusedLabelColor = colorPalette.text.copy(alpha = 0.7f),
                            unfocusedTextColor = colorPalette.text,
                            focusedTextColor = colorPalette.text,
                            cursorColor = colorPalette.text,
                            errorBorderColor = colorPalette.red,
                            errorLabelColor = colorPalette.red
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.name)) },
                        textStyle = typography.xs,
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = stringResource(R.string.name), tint = colorPalette.text) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        isError = errorMessage != null,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colorPalette.text,
                            unfocusedBorderColor = colorPalette.text.copy(alpha = 0.5f),
                            focusedLabelColor = colorPalette.text,
                            unfocusedLabelColor = colorPalette.text.copy(alpha = 0.7f),
                            unfocusedTextColor = colorPalette.text,
                            focusedTextColor = colorPalette.text,
                            cursorColor = colorPalette.text,
                            errorBorderColor = colorPalette.red,
                            errorLabelColor = colorPalette.red
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    errorMessage?.let { message ->
                        Text(
                            text = message,
                            color = colorPalette.red,
                            style = typography.xs,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                when {
                                    email.isBlank() -> {
                                        errorMessage = context.getString(R.string.email_cannot_be_empty)
                                    }
                                    password.length < 6 -> {
                                        errorMessage = context.getString(R.string.password_too_short)
                                    }
                                    else -> {
                                        isLoading = true
                                        errorMessage = null
                                        try {
                                            val authResult = FirebaseAuth.getInstance()
                                                .createUserWithEmailAndPassword(email, password)
                                                .await()
                                            val profileUpdates = UserProfileChangeRequest.Builder()
                                                .setDisplayName(name.takeIf { it.isNotBlank() })
                                                .build()
                                            authResult.user?.updateProfile(profileUpdates)?.await()
                                            isLoading = false
                                            withContext(Dispatchers.Main) { // Đảm bảo toast chạy trên Main thread
                                                context.toast(context.getString(R.string.registration_success))
                                            }
                                            onRegisterSuccess()
                                        } catch (e: Exception) {
                                            isLoading = false
                                            withContext(Dispatchers.Main) {
                                                context.toast(context.getString(R.string.register_failed, e.message))
                                            }
                                            errorMessage = e.message
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colorPalette.background0),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = colorPalette.text, modifier = Modifier.size(24.dp))
                        } else {
                            Text(stringResource(R.string.register), style = typography.xs.copy(fontWeight = FontWeight.Bold), color = colorPalette.text)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = onNavigateToLogin) {
                        Text(
                            text = stringResource(R.string.already_have_account),
                            color = colorPalette.text.copy(alpha = 0.7f),
                            style = typography.xs
                        )
                    }
                }
            }
        }
    }
}