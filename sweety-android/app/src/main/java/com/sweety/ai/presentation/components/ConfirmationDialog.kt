package com.sweety.ai.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.sweety.ai.data.PendingConfirmation
import com.sweety.ai.presentation.theme.AccentDanger
import com.sweety.ai.presentation.theme.AccentSuccess
import com.sweety.ai.presentation.theme.CardBackground
import com.sweety.ai.presentation.theme.ElectricCyan
import com.sweety.ai.presentation.theme.SurfaceDark
import com.sweety.ai.presentation.theme.TextPrimary
import com.sweety.ai.presentation.theme.TextSecondary
import com.sweety.ai.presentation.theme.Typography

@Composable
fun ConfirmationDialog(
    confirmation: PendingConfirmation,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceDark)
                .border(
                    width = 1.2.dp,
                    brush = Brush.linearGradient(listOf(ElectricCyan, CardBackground)),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SWEETY AI CONFIRMATION",
                style = Typography.labelSmall.copy(color = ElectricCyan)
            )

            Text(
                text = confirmation.description,
                style = Typography.titleMedium,
                color = TextPrimary,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        confirmation.onCancel()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.linearGradient(listOf(AccentDanger, Color.Transparent))
                    )
                ) {
                    Text("বাতিল", color = AccentDanger)
                }

                Button(
                    onClick = {
                        confirmation.onConfirm()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentSuccess)
                ) {
                    Text("কনফার্ম", color = Color.Black)
                }
            }
        }
    }
}

