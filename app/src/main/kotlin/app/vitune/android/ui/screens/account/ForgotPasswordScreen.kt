package app.vitune.android.ui.screens.account

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vitune.android.R
import app.vitune.android.utils.semiBold
import app.vitune.android.utils.toast
import app.vitune.core.ui.LocalAppearance
import app.vitune.core.ui.surface
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun ForgotPasswordScreen(
    onEmailSent: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val (colorPalette, typography) = LocalAppearance.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var email by rememberSaveable { mutableStateOf("") }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

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
                        text = stringResource(R.string.enter_your_email_reset_password),
                        style = typography.xs,
                        color = colorPalette.text.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(stringResource(R.string.email)) },
                        textStyle = typography.xs,
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email Icon", tint = colorPalette.text) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        isError = errorMessage != null,
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
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            if (email.isBlank()) {
                                errorMessage = context.getString(R.string.email_cannot_be_empty)
                                return@Button
                            }
                            errorMessage = null
                            isLoading = true
                            coroutineScope.launch {
                                try {
                                    FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                                        .await()
                                    isLoading = false
                                    context.toast(context.getString(R.string.password_reset_email_sent, email))
                                    onEmailSent() // Gọi callback khi gửi thành công
                                } catch (e: Exception) {
                                    isLoading = false
                                    errorMessage = context.getString(R.string.login_failed, e.message)
                                    context.toast(errorMessage!!)
                                }
                            }
                        },
                        enabled = !isLoading && email.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colorPalette.background0),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = colorPalette.text,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(stringResource(R.string.send_link), style = typography.xs.copy(fontWeight = FontWeight.Bold), color = colorPalette.text)
                        }
                    }

                    errorMessage?.let { message ->
                        Text(
                            text = message,
                            color = colorPalette.red,
                            style = typography.xs,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = onNavigateBack) {
                        Text(
                            text = stringResource(R.string.go_back),
                            color = colorPalette.text.copy(alpha = 0.7f),
                            style = typography.xs
                        )
                    }
                }
            }
        }
    }
}