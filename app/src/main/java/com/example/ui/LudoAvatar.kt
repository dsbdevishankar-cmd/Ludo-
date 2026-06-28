package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.Player

object AvatarAssets {
    val presetAvatars = listOf(
        Icons.Default.Face,
        Icons.Default.Person,
        Icons.Default.AccountBox,
        Icons.Default.Star,
        Icons.Default.Build,
        Icons.Default.Favorite,
        Icons.Default.ThumbUp,
        Icons.Default.Warning
    )

    val backgroundGradients = mapOf(
        "mystic" to Brush.linearGradient(listOf(Color(0xFF6750A4), Color(0xFF3F51B5))),
        "neon" to Brush.linearGradient(listOf(Color(0xFFFF007F), Color(0xFF7B1FA2))),
        "zen" to Brush.linearGradient(listOf(Color(0xFF00C853), Color(0xFF009688))),
        "sunset" to Brush.linearGradient(listOf(Color(0xFFFF5722), Color(0xFFE91E63))),
        "aqua" to Brush.linearGradient(listOf(Color(0xFF00B0FF), Color(0xFF2979FF)))
    )

    val backgroundColors = mapOf(
        "mystic" to Color(0xFF6750A4),
        "neon" to Color(0xFFFF007F),
        "zen" to Color(0xFF00C853),
        "sunset" to Color(0xFFFF5722),
        "aqua" to Color(0xFF00B0FF)
    )

    val icons = mapOf(
        "person" to Icons.Default.Person,
        "star" to Icons.Default.Star,
        "face" to Icons.Default.Face,
        "thumb_up" to Icons.Default.ThumbUp,
        "favorite" to Icons.Default.Favorite,
        "settings" to Icons.Default.Settings,
        "build" to Icons.Default.Build,
        "refresh" to Icons.Default.Refresh
    )

    val iconColors = mapOf(
        "white" to Color.White,
        "gold" to Color(0xFFFFC107),
        "cyan" to Color(0xFF00E5FF),
        "yellow" to Color(0xFFFFFF00),
        "lime" to Color(0xFF00FF00)
    )
}

@Composable
fun LudoAvatar(
    player: Player,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 44.dp,
    iconSize: Dp = 24.dp
) {
    LudoAvatar(
        useCustomAvatar = player.useCustomAvatar,
        avatarIndex = player.avatarIndex,
        customAvatarBg = player.customAvatarBg,
        customAvatarIcon = player.customAvatarIcon,
        customAvatarIconColor = player.customAvatarIconColor,
        customAvatarBorder = player.customAvatarBorder,
        modifier = modifier,
        avatarSize = avatarSize,
        iconSize = iconSize
    )
}

@Composable
fun LudoAvatar(
    useCustomAvatar: Boolean,
    avatarIndex: Int,
    customAvatarBg: String,
    customAvatarIcon: String,
    customAvatarIconColor: String,
    customAvatarBorder: String,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 44.dp,
    iconSize: Dp = 24.dp
) {
    val borderStroke = when (customAvatarBorder) {
        "gold" -> BorderStroke(2.dp, Color(0xFFFFC107))
        "cyber" -> BorderStroke(2.dp, Color(0xFF00FF00))
        "glowing" -> BorderStroke(
            2.dp,
            Brush.sweepGradient(listOf(Color(0xFF00E5FF), Color(0xFFFF007F), Color(0xFF00E5FF)))
        )
        else -> BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    }

    Box(
        modifier = modifier
            .size(avatarSize)
            .clip(CircleShape)
            .then(
                if (useCustomAvatar) {
                    val brush = AvatarAssets.backgroundGradients[customAvatarBg] ?: AvatarAssets.backgroundGradients["mystic"]!!
                    Modifier.background(brush)
                } else {
                    Modifier.background(Color(0xFF6750A4)) // BentoPrimary default
                }
            )
            .border(borderStroke, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (useCustomAvatar) {
            val iconVec = AvatarAssets.icons[customAvatarIcon] ?: Icons.Default.Star
            val tintColor = AvatarAssets.iconColors[customAvatarIconColor] ?: Color.White
            Icon(
                imageVector = iconVec,
                contentDescription = "Avatar Icon",
                tint = tintColor,
                modifier = Modifier.size(iconSize)
            )
        } else {
            val iconVec = AvatarAssets.presetAvatars.getOrElse(avatarIndex) { Icons.Default.Person }
            Icon(
                imageVector = iconVec,
                contentDescription = "Avatar Icon",
                tint = Color.White,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}
