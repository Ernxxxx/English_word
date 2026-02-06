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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.englishword.ui.study.EvaluationResult
import com.example.englishword.ui.theme.EnglishWordTheme
import com.example.englishword.ui.theme.EvaluationAgain
import com.example.englishword.ui.theme.EvaluationAgainDark
import com.example.englishword.ui.theme.EvaluationKnown
import com.example.englishword.ui.theme.EvaluationKnownDark
import com.example.englishword.ui.theme.EvaluationLater
import com.example.englishword.ui.theme.EvaluationLaterDark

// Evaluation colors are now defined in Color.kt as:
// EvaluationAgain, EvaluationAgainDark, EvaluationLater, EvaluationLaterDark, EvaluationKnown, EvaluationKnownDark

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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Again button (red/orange)
        EvaluationButton(
            text = "まだ",
            icon = Icons.Default.Close,
            backgroundColor = EvaluationAgain,
            pressedColor = EvaluationAgainDark,
            onClick = { onEvaluate(EvaluationResult.AGAIN) },
            enabled = enabled,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Later button (yellow)
        EvaluationButton(
            text = "あとで",
            icon = Icons.Default.Refresh,
            backgroundColor = EvaluationLater,
            pressedColor = EvaluationLaterDark,
            onClick = { onEvaluate(EvaluationResult.LATER) },
            enabled = enabled,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Known button (green)
        EvaluationButton(
            text = "覚えた",
            icon = Icons.Default.Check,
            backgroundColor = EvaluationKnown,
            pressedColor = EvaluationKnownDark,
            onClick = { onEvaluate(EvaluationResult.KNOWN) },
            enabled = enabled,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Individual evaluation button - simplified without animation state issues.
 */
@Composable
private fun EvaluationButton(
    text: String,
    icon: ImageVector,
    backgroundColor: Color,
    pressedColor: Color,
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
            contentColor = Color.White,
            disabledContainerColor = backgroundColor.copy(alpha = 0.5f),
            disabledContentColor = Color.White.copy(alpha = 0.7f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 2.dp
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

/**
 * Japanese version of EvaluationButtons.
 */
@Composable
fun EvaluationButtonsJapanese(
    onEvaluate: (EvaluationResult) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Again button (red/orange)
        EvaluationButton(
            text = "まだ",
            icon = Icons.Default.Close,
            backgroundColor = EvaluationAgain,
            pressedColor = EvaluationAgainDark,
            onClick = { onEvaluate(EvaluationResult.AGAIN) },
            enabled = enabled,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Later button (yellow)
        EvaluationButton(
            text = "あとで",
            icon = Icons.Default.Refresh,
            backgroundColor = EvaluationLater,
            pressedColor = EvaluationLaterDark,
            onClick = { onEvaluate(EvaluationResult.LATER) },
            enabled = enabled,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Known button (green)
        EvaluationButton(
            text = "覚えた",
            icon = Icons.Default.Check,
            backgroundColor = EvaluationKnown,
            pressedColor = EvaluationKnownDark,
            onClick = { onEvaluate(EvaluationResult.KNOWN) },
            enabled = enabled,
            modifier = Modifier.weight(1f)
        )
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
