package com.sweety.ai.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sweety.ai.data.ChatMessage
import com.sweety.ai.presentation.theme.AccentSuccess
import com.sweety.ai.presentation.theme.CardBackground
import com.sweety.ai.presentation.theme.ElectricCyan
import com.sweety.ai.presentation.theme.NeonBlue
import com.sweety.ai.presentation.theme.SurfaceDark
import com.sweety.ai.presentation.theme.TextDim
import com.sweety.ai.presentation.theme.TextPrimary
import com.sweety.ai.presentation.theme.Typography

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val isUser = message.isUser

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.85f),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            // Sender tag
            Text(
                text = if (isUser) "YOU" else "SWEETY AI",
                style = Typography.labelSmall.copy(
                    color = if (isUser) NeonBlue else ElectricCyan,
                    fontSize = 10.sp
                ),
                modifier = Modifier.padding(bottom = 2.dp, start = 4.dp, end = 4.dp)
            )

            // Message Bubble Card
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .background(
                        if (isUser) Brush.linearGradient(
                            listOf(Color(0xB80F2B48), Color(0xA80A1B2E))
                        )
                        else Brush.linearGradient(
                            listOf(Color(0xC410172A), Color(0xB0151F38))
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            if (isUser) listOf(NeonBlue.copy(alpha = 0.4f), Color.Transparent)
                            else listOf(ElectricCyan.copy(alpha = 0.3f), Color.Transparent)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column {
                    Text(
                        text = message.text,
                        style = Typography.bodyLarge,
                        color = TextPrimary
                    )

                    // Executed action badges
                    if (message.actions.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            message.actions.forEach { act ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(AccentSuccess.copy(alpha = 0.15f))
                                        .border(0.8.dp, AccentSuccess.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Bolt,
                                        contentDescription = null,
                                        tint = AccentSuccess,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = act.action.replace("_", " ").uppercase(),
                                        style = Typography.labelSmall.copy(
                                            color = AccentSuccess,
                                            fontSize = 9.sp
                                        ),
                                        modifier = Modifier.padding(start = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

