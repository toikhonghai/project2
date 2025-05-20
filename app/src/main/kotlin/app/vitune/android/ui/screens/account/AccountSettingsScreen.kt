package app.vitune.android.ui.screens.account

import android.util.Log
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import app.vitune.android.MainActivity
import app.vitune.android.ui.screens.GlobalRoutes
import app.vitune.android.ui.screens.loginRoute
import app.vitune.compose.routing.RouteHandler
import app.vitune.core.ui.LocalAppearance
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import app.vitune.android.R
import app.vitune.android.ui.components.themed.Header
import app.vitune.android.ui.components.themed.IconButton
import app.vitune.android.utils.toast
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun AccountSettingsScreen() {
    RouteHandler {
        GlobalRoutes()
        Content {
            val (colorPalette, typography) = LocalAppearance.current
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            val currentUser = FirebaseAuth.getInstance().currentUser

            var displayName by rememberSaveable { mutableStateOf(currentUser?.displayName ?: "") }
            var newEmail by rememberSaveable { mutableStateOf(currentUser?.email ?: "") }
            var newPassword by rememberSaveable { mutableStateOf("") }
            var currentPassword by rememberSaveable { mutableStateOf("") }
            var showCurrentPassword by rememberSaveable { mutableStateOf(false) }
            var showNewPassword by rememberSaveable { mutableStateOf(false) }
            var isLoading by rememberSaveable { mutableStateOf(false) }
            var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
            var showLogoutDialog by rememberSaveable { mutableStateOf(false) }


            // Track original values to detect changes
            val originalEmail = remember { currentUser?.email ?: "" }
            val originalDisplayName = remember { currentUser?.displayName ?: "" }

            // Determine if any changes were made
            val hasChanges = displayName != originalDisplayName ||
                    newEmail != originalEmail ||
                    newPassword.isNotEmpty()

            // Lắng nghe trạng thái đăng nhập (dự phòng)
            DisposableEffect(Unit) {
                val authListener = FirebaseAuth.AuthStateListener { auth ->
                    if (auth.currentUser == null) {
                        context.toast("Vui lòng đăng nhập lại sau khi xác minh email.")
                        loginRoute()
                    }
                }
                FirebaseAuth.getInstance().addAuthStateListener(authListener)
                onDispose {
                    FirebaseAuth.getInstance().removeAuthStateListener(authListener)
                }
            }

            if (showLogoutDialog) {
                AlertDialog(
                    onDismissRequest = { showLogoutDialog = false },
                    text = { Text(stringResource(R.string.logout_dialog_message), style = typography.m) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    FirebaseAuth.getInstance().signOut()
                                    (context as MainActivity).forceRefresh = true
                                }
                            }
                        ) {
                            Text(stringResource(R.string.confirm), color = colorPalette.accent)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLogoutDialog = false }) {
                            Text(stringResource(R.string.cancel), color = colorPalette.textSecondary)
                        }
                    }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorPalette.background0)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 36.dp, bottom = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = pop,
                            icon = R.drawable.chevron_back,
                            color = colorPalette.text,
                            modifier = Modifier.size(24.dp)
                        )

                        IconButton(
                            onClick = { showLogoutDialog = true },
                            icon = R.drawable.logout,
                            color = colorPalette.text,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Header(
                            title = stringResource(R.string.account_settings),
                            modifier = Modifier.align(Alignment.End),
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = displayName,
                            onValueChange = {
                                displayName = it
                                errorMessage = null
                            },
                            label = { Text(stringResource(R.string.display_name)) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            isError = errorMessage != null
                        )

                        OutlinedTextField(
                            value = newEmail,
                            onValueChange = {
                                newEmail = it
                                errorMessage = null
                            },
                            label = { Text(text = stringResource(R.string.email)) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            isError = errorMessage != null
                        )

                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = {
                                newPassword = it
                                errorMessage = null
                            },
                            label = { Text(stringResource(R.string.new_password_optional)) },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            isError = errorMessage != null,
                            trailingIcon = {
                                IconButton(
                                    onClick = { showNewPassword = !showNewPassword },
                                    icon = if (showNewPassword) R.drawable.visibility else R.drawable.visibility_off,
                                    color = colorPalette.text,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        )

                        if (hasChanges) {
                            OutlinedTextField(
                                value = currentPassword,
                                onValueChange = {
                                    currentPassword = it
                                    errorMessage = null
                                },
                                label = { Text(text = stringResource(R.string.current_password)) },
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = if (showCurrentPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                isError = errorMessage != null,
                                trailingIcon = {
                                    IconButton(
                                        onClick = { showCurrentPassword = !showCurrentPassword },
                                        icon = if (showCurrentPassword) R.drawable.visibility else R.drawable.visibility_off,
                                        color = colorPalette.text,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            )

                            // Add "Forgot Password" text button
                            TextButton(
                                onClick = {
                                    coroutineScope.launch {
                                        try {
                                            isLoading = true
                                            errorMessage = null
                                            val userEmail = currentUser?.email
                                            if (userEmail != null) {
                                                FirebaseAuth.getInstance().sendPasswordResetEmail(userEmail).await()
                                                context.toast("Email đặt lại mật khẩu đã được gửi đến $userEmail")
                                            } else {
                                                errorMessage = "Không tìm thấy email tài khoản"
                                            }
                                        } catch (e: Exception) {
                                            Log.e("AccountSettings", "Forgot password error", e)
                                            errorMessage = "Lỗi: ${e.message}"
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(
                                    stringResource(R.string.forgot_password),
                                    color = colorPalette.accent,
                                    style = typography.xs
                                )
                            }
                        }

                        errorMessage?.let { message ->
                            Text(
                                text = message,
                                color = colorPalette.red,
                                style = typography.xs,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        isLoading = true
                                        errorMessage = null

                                        // Don't require password verification if no changes
                                        if (!hasChanges) {
                                            context.toast("Không có thông tin thay đổi")
                                            isLoading = false
                                            return@launch
                                        }

                                        // Require current password for any changes
                                        if (currentPassword.isEmpty()) {
                                            errorMessage = "Vui lòng nhập mật khẩu hiện tại để xác nhận"
                                            isLoading = false
                                            return@launch
                                        }

                                        val user = FirebaseAuth.getInstance().currentUser
                                        val userEmail = user?.email

                                        if (userEmail.isNullOrEmpty()) {
                                            errorMessage = "Không thể xác định email người dùng"
                                            isLoading = false
                                            return@launch
                                        }

                                        // Kiểm tra phương thức đăng nhập
                                        val providers = user?.providerData?.map { it.providerId } ?: emptyList()
                                        Log.d("AccountSettings", "Providers: $providers")

                                        if (!providers.contains(EmailAuthProvider.PROVIDER_ID)) {
                                            errorMessage = "Tài khoản của bạn không sử dụng đăng nhập bằng email/mật khẩu"
                                            isLoading = false
                                            return@launch
                                        }

                                        // Reauthenticate user
                                        try {
                                            val credential = EmailAuthProvider.getCredential(userEmail, currentPassword)
                                            Log.d("AccountSettings", "Reauthenticating user with email: $userEmail")
                                            user.reauthenticate(credential).await()
                                            Log.d("AccountSettings", "Reauthentication successful")
                                        } catch (e: Exception) {
                                            Log.e("AccountSettings", "Reauthentication failed", e)
                                            errorMessage = "Xác thực thất bại: Mật khẩu hiện tại không đúng"
                                            isLoading = false
                                            return@launch
                                        }

                                        // Update email if changed
                                        if (newEmail != originalEmail) {
                                            // Kiểm tra định dạng email
                                            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                                                errorMessage = "Email không hợp lệ"
                                                isLoading = false
                                                return@launch
                                            }

                                            // Gửi email xác minh
                                            try {
                                                user.verifyBeforeUpdateEmail(newEmail).await()
                                                context.toast("Email xác minh đã được gửi đến $newEmail. Vui lòng xác minh trước khi cập nhật.")
                                                Log.d("AccountSettings", "Verification email sent to $newEmail")
                                                // Chuyển hướng ngay về LoginScreen sau khi gửi email xác minh
                                                loginRoute()
                                                return@launch
                                            } catch (e: Exception) {
                                                Log.e("AccountSettings", "Error sending verification email", e)
                                                errorMessage = "Không thể gửi email xác minh: ${e.message}"
                                                isLoading = false
                                                return@launch
                                            }
                                        }

                                        // Nếu không thay đổi email, tiếp tục xử lý các thay đổi khác
                                        // Update display name if changed
                                        try {
                                            if (displayName != originalDisplayName) {
                                                val profileUpdates = UserProfileChangeRequest.Builder()
                                                    .setDisplayName(displayName)
                                                    .build()
                                                user.updateProfile(profileUpdates).await()
                                                Log.d("AccountSettings", "Display name updated successfully")
                                            }
                                        } catch (e: Exception) {
                                            Log.e("AccountSettings", "Error updating display name", e)
                                            errorMessage = "Không thể cập nhật tên hiển thị: ${e.message}"
                                            isLoading = false
                                            return@launch
                                        }

                                        // Update password if provided
                                        try {
                                            if (newPassword.isNotEmpty()) {
                                                user.updatePassword(newPassword).await()
                                                Log.d("AccountSettings", "Password updated successfully")
                                            }
                                        } catch (e: Exception) {
                                            Log.e("AccountSettings", "Error updating password", e)
                                            errorMessage = "Không thể cập nhật mật khẩu: ${e.message}"
                                            isLoading = false
                                            return@launch
                                        }

                                        // Hiển thị thông báo thành công
                                        context.toast("Cập nhật thông tin thành công")

                                        // Reset các trường nếu người dùng không bị đăng xuất
                                        if (FirebaseAuth.getInstance().currentUser != null) {
                                            currentPassword = ""
                                            newPassword = ""
                                        }

                                    } catch (e: Exception) {
                                        Log.e("AccountSettings", "Update failed", e)
                                        errorMessage = "Cập nhật thất bại: ${e.message}"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            enabled = !isLoading && hasChanges,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = colorPalette.onAccent,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(stringResource(R.string.update_info))
                            }
                        }
                    }
                }
            }
        }
    }
}