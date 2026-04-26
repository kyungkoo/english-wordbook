package com.example.vocabai

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

object AppColors {
  val Paper = Color(0xFFFBF7ED)
  val PaperDeep = Color(0xFFF1E7D3)
  val Surface = Color(0xFFFFFCF5)
  val Ink = Color(0xFF24312D)
  val Muted = Color(0xFF68756F)
  val Primary = Color(0xFF2E6F5E)
  val PrimarySoft = Color(0xFFE2EFE8)
  val Review = Color(0xFFB7791F)
  val ReviewSoft = Color(0xFFFFF1D6)
  val Danger = Color(0xFFB4493E)
  val Line = Color(0xFFE0D5C3)
  val Success = Primary
  val Background = Paper
}

val NotebookShape = RoundedCornerShape(12.dp)
val ButtonShape = RoundedCornerShape(10.dp)

@Composable
fun NotebookCard(
  modifier: Modifier = Modifier,
  containerColor: Color = AppColors.Surface,
  content: @Composable () -> Unit,
) {
  Card(
    modifier = modifier,
    shape = NotebookShape,
    colors = CardDefaults.cardColors(containerColor = containerColor),
    border = BorderStroke(1.dp, AppColors.Line),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
  ) {
    content()
  }
}

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppColors.Ink)
    if (action != null && onAction != null) {
      TextButton(onClick = onAction) {
        Text(action)
      }
    }
  }
}

@Composable
fun ProgressPill(text: String, modifier: Modifier = Modifier, accent: Color = AppColors.Primary) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(999.dp),
    colors = CardDefaults.cardColors(containerColor = if (accent == AppColors.Review) AppColors.ReviewSoft else AppColors.PrimarySoft),
  ) {
    Text(
      text = text,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
      color = accent,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.Bold,
    )
  }
}

@Composable
fun PrimaryActionButton(
  text: String,
  icon: ImageVector? = null,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  onClick: () -> Unit,
) {
  Button(
    onClick = onClick,
    modifier = modifier.heightIn(min = 48.dp),
    enabled = enabled,
    shape = ButtonShape,
    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
  ) {
    if (icon != null) {
      Icon(icon, contentDescription = null)
      Text("  ")
    }
    Text(text)
  }
}

@Composable
fun IconTextButton(
  text: String,
  icon: ImageVector,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  OutlinedButton(
    onClick = onClick,
    modifier = modifier.heightIn(min = 44.dp),
    shape = ButtonShape,
    border = BorderStroke(1.dp, AppColors.Line),
  ) {
    Icon(icon, contentDescription = null, tint = AppColors.Primary)
    Text("  $text", color = AppColors.Ink)
  }
}

@Composable
fun CompactIconButton(
  icon: ImageVector,
  contentDescription: String,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  IconButton(onClick = onClick, modifier = modifier.size(40.dp)) {
    Icon(icon, contentDescription = contentDescription, tint = AppColors.Primary)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordTopBar(
  title: String,
  navigationLabel: String? = null,
  onNavigation: (() -> Unit)? = null,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
) {
  TopAppBar(
    title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, color = AppColors.Ink, fontWeight = FontWeight.Bold) },
    navigationIcon = {
      if (navigationLabel != null && onNavigation != null) {
        TextButton(onClick = onNavigation) {
          Text(navigationLabel, color = AppColors.Primary)
        }
      }
    },
    actions = {
      if (actionLabel != null && onAction != null) {
        TextButton(onClick = onAction) {
          Text(actionLabel, color = AppColors.Primary, fontWeight = FontWeight.Bold)
        }
      }
    },
    colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Paper),
  )
}
