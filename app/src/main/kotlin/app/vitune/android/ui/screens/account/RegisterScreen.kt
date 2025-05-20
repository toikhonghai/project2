package app.vitune.android.ui.screens.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import app.vitune.android.R
import app.vitune.android.ui.components.themed.IconButton
import app.vitune.android.utils.semiBold
import app.vitune.android.utils.toast
import app.vitune.core.ui.LocalAppearance
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
    var errorMessage by remember { mutableStateOf<String?>(null) } // Thêm trạng thái để hiển thị lỗi
    var showPassword by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Register",
            style = typography.xxl.semiBold,
            color = colorPalette.text
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            textStyle = typography.xs,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = errorMessage != null
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            textStyle = typography.xs,
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = errorMessage != null,
            trailingIcon = {
                IconButton(
                    onClick = { showPassword = !showPassword },
                    icon = if (showPassword) R.drawable.visibility else R.drawable.visibility_off,
                    color = colorPalette.text,
                    modifier = Modifier.size(24.dp)
                )
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            textStyle = typography.xs,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            isError = errorMessage != null
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Hiển thị thông báo lỗi nếu có
        errorMessage?.let { message ->
            Text(
                text = message,
                color = colorPalette.red, // Màu đỏ để báo lỗi
                style = typography.xs,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Button(
            onClick = {
                coroutineScope.launch {
                    // Kiểm tra đầu vào trước khi gọi Firebase
                    when {
                        email.isBlank() -> {
                            errorMessage = "Email cannot be empty"
                        }
                        password.length < 6 -> {
                            errorMessage = "Password must be at least 6 characters"
                        }
                        else -> {
                            isLoading = true
                            errorMessage = null // Xóa thông báo lỗi cũ
                            try {
                                val authResult = FirebaseAuth.getInstance()
                                    .createUserWithEmailAndPassword(email, password)
                                    .await()
                                val profileUpdates = UserProfileChangeRequest.Builder()
                                    .setDisplayName(name.takeIf { it.isNotBlank() })
                                    .build()
                                authResult.user?.updateProfile(profileUpdates)?.await()
                                isLoading = false
                                onRegisterSuccess()
                            } catch (e: Exception) {
                                isLoading = false
                                // Chuyển sang luồng chính để hiển thị toast
                                withContext(Dispatchers.Main) {
                                    context.toast("Registration failed: ${e.message}")
                                }
                                errorMessage = e.message // Hiển thị lỗi trên giao diện
                            }
                        }
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Register")
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onNavigateToLogin) {
            Text("Already have an account? Login")
        }
    }
}