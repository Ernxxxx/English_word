package com.example.englishword.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.englishword.ui.study.EvaluationResult
import com.example.englishword.ui.theme.EnglishWordTheme
import com.example.englishword.ui.theme.EvaluationAgain
import com.example.englishword.ui.theme.EvaluationKnown
import com.example.englishword.ui.theme.EvaluationLater
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Evaluation buttons component for rating word knowledge.
 * Shows three buttons: Again (red), Later (yellow), Known (green).
 */
@Composable
fun EvaluationButtons(
    onEvaluate: (EvaluationResult) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var isTapLocked by remember { mutableStateOf(false) }

    fun onEvaluateWithDebounce(result: EvaluationResult) {
        if (!enabled || isTapLocked) return
        isTapLocked = true
        onEvaluate(result)
        scope.launch {
            delay(300)
            isTapLocked = false
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        EvaluationButton(
            text = "まだ",
            icon = Icons.Default.Close,
            backgroundColor = EvaluationAgain,
            onClick = { onEvaluateWithDebounce(EvaluationResult.AGAIN) },
            enabled = enabled && !isTapLocked,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(12.dp))

        EvaluationButton(
            text = "あとで",
            icon = Icons.Default.Refresh,
            backgroundColor = EvaluationLater,
            onClick = { onEvaluateWithDebounce(EvaluationResult.LATER) },
            enabled = enabled && !isTapLocked,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(12.dp))

        EvaluationButton(
            text = "覚えた",
            icon = Icons.Default.Check,
            backgroundColor = EvaluationKnown,
            onClick = { onEvaluateWithDebounce(EvaluationResult.KNOWN) },
            enabled = enabled && !isTapLocked,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun EvaluationButton(
    text: String,
    icon: ImageVector,
    backgroundColor: Color,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = backgroundColor.copy(alpha = 0.5f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 1.dp,
            pressedElevation = 0.dp
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EvaluationButtonsPreview() {
    EnglishWordTheme {
        EvaluationButtons(
            onEvaluate = {},
            enabled = true,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EvaluationButtonsDisabledPreview() {
    EnglishWordTheme {
        EvaluationButtons(
            onEvaluate = {},
            enabled = false,
            modifier = Modifier.padding(16.dp)
        )
    }
}
