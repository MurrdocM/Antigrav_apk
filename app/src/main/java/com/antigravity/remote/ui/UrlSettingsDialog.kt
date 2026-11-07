package com.antigravity.remote.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.remote.data.PreferencesManager
import com.antigravity.remote.ui.theme.DangerColor
import com.antigravity.remote.ui.theme.PrimaryAccent
import com.antigravity.remote.ui.theme.SurfaceDark
import com.antigravity.remote.ui.theme.TextPrimary
import com.antigravity.remote.ui.theme.TextSecondary

@Composable
fun UrlSettingsDialog(
    currentUrl: String,
    urlHistory: List<String>,
    onDismiss: () -> Unit,
    onApplyUrl: (String) -> Unit,
    onResetDefault: () -> Unit,
    onClearSession: () -> Unit
) {
    var inputUrl by remember(currentUrl) { mutableStateOf(currentUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text(
                text = "Налаштування адреси",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = inputUrl,
                    onValueChange = { inputUrl = it },
                    label = { Text("URL сервера") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (urlHistory.isNotEmpty()) {
                    Text(
                        text = "Історія адрес:",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    urlHistory.forEach { item ->
                        Text(
                            text = item,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { inputUrl = item }
                                .padding(vertical = 6.dp)
                        )
                        HorizontalDivider(color = TextSecondary.copy(alpha = 0.2f))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            inputUrl = PreferencesManager.DEFAULT_URL
                            onResetDefault()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Дефолт", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onClearSession,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerColor),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Скинути сесію", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val formatted = if (inputUrl.startsWith("http://") || inputUrl.startsWith("https://")) {
                        inputUrl.trim()
                    } else {
                        "https://${inputUrl.trim()}"
                    }
                    onApplyUrl(formatted)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
            ) {
                Text("Перейти")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Скасувати", color = TextSecondary)
            }
        }
    )
}
