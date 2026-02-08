package com.example.parktrack.ui.admin.components
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ActivityChart(data: List<Int>) {

    val max = (data.maxOrNull() ?: 1).coerceAtLeast(1)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

        Text(
            text = "Last 6 Hours Activity",
            style = MaterialTheme.typography.titleMedium
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEach { count ->
                val heightRatio = count.toFloat() / max.toFloat()

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(heightRatio)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            repeat(6) { index ->
                Text(
                    text = "${6 - index}h",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
