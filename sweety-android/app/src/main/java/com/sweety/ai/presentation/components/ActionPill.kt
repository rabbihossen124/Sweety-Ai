package com.sweety.ai.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sweety.ai.presentation.theme.ElectricCyan
import com.sweety.ai.presentation.theme.NeonBlue
import com.sweety.ai.presentation.theme.SurfaceDark
import com.sweety.ai.presentation.theme.TextPrimary
import com.sweety.ai.presentation.theme.Typography

@Composable
fun ActionPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = label,
        style = Typography.labelSmall.copy(color = TextPrimary, fontSize = 12.sp),
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceDark)
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(listOf(ElectricCyan.copy(alpha = 0.6f), NeonBlue.copy(alpha = 0.3f))),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    )
}

