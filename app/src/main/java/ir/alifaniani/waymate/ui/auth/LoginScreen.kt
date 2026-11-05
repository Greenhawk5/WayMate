package ir.alifaniani.waymate.ui.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.alifaniani.waymate.ui.common.BannerType
import ir.alifaniani.waymate.ui.common.TacticalFeedbackBanner
import ir.alifaniani.waymate.ui.common.TacticalPrimaryButton
import ir.alifaniani.waymate.ui.common.TacticalScreenContainer
import ir.alifaniani.waymate.ui.common.TacticalTextField

/**
 * Polished authentication screen adhering to the WayMate Dark Tactical design system.
 *
 * Implements:
 * - Proper Persian typography and RTL layout
 * - High-contrast OLED dark styling
 * - Seamless keyboard avoidance and scrollability on small screens
 * - Transparent synthetic account guidance for development demonstration
 */
@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLoginClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    TacticalScreenContainer(modifier = modifier) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "سامانه تاکتیکی وی‌میت",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "مدیریت مکانی و اشراف اطلاعاتی عملیات",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Synthetic Credentials Development Banner
        TacticalFeedbackBanner(
            title = "محیط توسعه و آزمون (داده‌های ساختگی)",
            message = "فرمانده: commander_demo | گذرواژه: demo123\nسرتیم: patrol_lead_demo | گذرواژه: demo123",
            type = BannerType.INFO
        )

        Spacer(modifier = Modifier.height(10.dp))

        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            ir.alifaniani.waymate.ui.common.TacticalOutlinedButton(
                text = "ورود آزمایشی فرمانده",
                onClick = {
                    onUsernameChanged("commander_demo")
                    onPasswordChanged("demo123")
                },
                modifier = Modifier.weight(1f)
            )
            ir.alifaniani.waymate.ui.common.TacticalOutlinedButton(
                text = "ورود آزمایشی سرتیم",
                onClick = {
                    onUsernameChanged("patrol_lead_demo")
                    onPasswordChanged("demo123")
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Username Field
        TacticalTextField(
            value = uiState.usernameInput,
            onValueChange = onUsernameChanged,
            label = "نام کاربری (لاتین)",
            enabled = !uiState.isLoading,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password Field with Visibility Toggle
        TacticalTextField(
            value = uiState.passwordInput,
            onValueChange = onPasswordChanged,
            label = "گذرواژه",
            isPassword = true,
            enabled = !uiState.isLoading,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    onLoginClicked()
                }
            )
        )

        if (uiState.errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            TacticalFeedbackBanner(
                title = "خطای اعتبارسنجی",
                message = uiState.errorMessage,
                type = BannerType.ERROR
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        TacticalPrimaryButton(
            text = "ورود به سامانه",
            onClick = onLoginClicked,
            isLoading = uiState.isLoading,
            enabled = !uiState.isLoading
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}
