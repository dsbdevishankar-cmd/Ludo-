package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserStats
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    stats: UserStats,
    viewModel: GameViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Temporary customizer states
    var nickname by remember { mutableStateOf(stats.nickname) }
    var useCustomAvatar by remember { mutableStateOf(stats.useCustomAvatar) }
    var selectedAvatarIdx by remember { mutableStateOf(stats.avatarIndex) }
    var customBg by remember { mutableStateOf(stats.customAvatarBg) }
    var customIcon by remember { mutableStateOf(stats.customAvatarIcon) }
    var customIconColor by remember { mutableStateOf(stats.customAvatarIconColor) }
    var customBorder by remember { mutableStateOf(stats.customAvatarBorder) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Profile Studio",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = BentoText
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("profile_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Dashboard",
                            tint = BentoText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BentoBg
                )
            )
        },
        containerColor = BentoBg,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Live Preview & Nickname Card (Bento Widget)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BentoBorder, shape = RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Avatar Live Preview
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LudoAvatar(
                            useCustomAvatar = useCustomAvatar,
                            avatarIndex = selectedAvatarIdx,
                            customAvatarBg = customBg,
                            customAvatarIcon = customIcon,
                            customAvatarIconColor = customIconColor,
                            customAvatarBorder = customBorder,
                            avatarSize = 90.dp,
                            iconSize = 48.dp
                        )
                    }

                    // Nickname TextField
                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { if (it.length <= 15) nickname = it },
                        label = { Text("Profile Nickname") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_nickname_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BentoPrimary,
                            unfocusedBorderColor = BentoBorder,
                            focusedLabelColor = BentoPrimary
                        )
                    )

                    // Level / XP Progress inside preview
                    val xpNeeded = stats.level * 200
                    val progress = (stats.xp.toFloat() / xpNeeded).coerceIn(0f, 1f)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Lvl ${stats.level} Mastery",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BentoPrimary
                            )
                            Text(
                                text = "${stats.xp}/$xpNeeded XP",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BentoText
                            )
                        }
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = BentoPrimary,
                            trackColor = BentoSecondaryContainer
                        )
                    }
                }
            }

            // Avatar Designer panel (Bento Widget)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BentoBorder, shape = RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "AVATAR DESIGNER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoPrimary,
                        letterSpacing = 1.sp
                    )

                    // Selector Mode: Presets vs Custom
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BentoSecondaryContainer, shape = RoundedCornerShape(14.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Presets Option Tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    color = if (!useCustomAvatar) Color.White else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { useCustomAvatar = false }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Preset Avatars",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!useCustomAvatar) BentoPrimary else Color.Gray
                            )
                        }

                        // Custom Option Tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    color = if (useCustomAvatar) Color.White else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { useCustomAvatar = true }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Custom Creator",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (useCustomAvatar) BentoPrimary else Color.Gray
                            )
                        }
                    }

                    // Content based on choice
                    if (!useCustomAvatar) {
                        // Preset Avatars Selection
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Choose from preset figures:",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                AvatarAssets.presetAvatars.forEachIndexed { idx, icon ->
                                    val isSelected = selectedAvatarIdx == idx
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                color = if (isSelected) BentoPrimaryContainer else BentoSecondaryContainer.copy(alpha = 0.5f),
                                                shape = CircleShape
                                            )
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) BentoPrimary else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable { selectedAvatarIdx = idx }
                                            .testTag("preset_avatar_btn_$idx"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (isSelected) BentoPrimary else BentoText.copy(alpha = 0.7f),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Custom Avatar Creator controls
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // 1. Background Theme Row
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "1. Background Aura",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoText
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AvatarAssets.backgroundGradients.keys.forEach { bgName ->
                                        val isSelected = customBg == bgName
                                        val colorRep = AvatarAssets.backgroundColors[bgName] ?: Color.LightGray
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(colorRep, CircleShape)
                                                .border(
                                                    width = if (isSelected) 2.5.dp else 1.dp,
                                                    color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.4f),
                                                    shape = CircleShape
                                                )
                                                .clickable { customBg = bgName }
                                                .testTag("custom_bg_btn_$bgName")
                                        )
                                    }
                                }
                            }

                            // 2. Emblem Row
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "2. Emblem Symbol",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoText
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AvatarAssets.icons.keys.forEach { iconName ->
                                        val isSelected = customIcon == iconName
                                        val iconVec = AvatarAssets.icons[iconName]!!
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(
                                                    color = if (isSelected) BentoPrimaryContainer else BentoSecondaryContainer,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .border(
                                                    width = if (isSelected) 1.5.dp else 0.dp,
                                                    color = if (isSelected) BentoPrimary else Color.Transparent,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable { customIcon = iconName }
                                                .testTag("custom_icon_btn_$iconName"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = iconVec,
                                                contentDescription = iconName,
                                                tint = if (isSelected) BentoPrimary else BentoText.copy(alpha = 0.6f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // 3. Emblem Color Row
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "3. Emblem Color",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoText
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AvatarAssets.iconColors.keys.forEach { colName ->
                                        val isSelected = customIconColor == colName
                                        val colorVal = AvatarAssets.iconColors[colName]!!
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(colorVal, CircleShape)
                                                .border(
                                                    width = if (isSelected) 2.dp else 1.dp,
                                                    color = if (isSelected) BentoPrimary else Color.LightGray,
                                                    shape = CircleShape
                                                )
                                                .clickable { customIconColor = colName }
                                                .testTag("custom_color_btn_$colName")
                                        )
                                    }
                                }
                            }

                            // 4. Border Frame Row
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "4. Border Aura Ring",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoText
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    val borders = listOf(
                                        "none" to "Standard",
                                        "gold" to "Golden",
                                        "cyber" to "Cyber",
                                        "glowing" to "Rainbow"
                                    )
                                    borders.forEach { (bName, bLabel) ->
                                        val isSelected = customBorder == bName
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(
                                                    color = if (isSelected) BentoPrimaryContainer else BentoSecondaryContainer,
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .border(
                                                    width = if (isSelected) 1.5.dp else 0.dp,
                                                    color = if (isSelected) BentoPrimary else Color.Transparent,
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .clickable { customBorder = bName }
                                                .testTag("custom_border_btn_$bName")
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = bLabel,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) BentoPrimary else BentoText.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Achievements & Badges (Bento Grid level 2)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BentoBorder, shape = RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "ACHIEVEMENTS & BADGES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoPrimary,
                        letterSpacing = 1.sp
                    )

                    // Badges logic and list
                    val badgeList = listOf(
                        BadgeInfo(
                            id = "novice",
                            title = "Ludo Novice",
                            desc = "Completed 1st match",
                            isUnlocked = stats.matchesPlayed > 0,
                            icon = Icons.Default.Face,
                            color = Color(0xFF00C853)
                        ),
                        BadgeInfo(
                            id = "streak",
                            title = "Unstoppable",
                            desc = "Reached 3+ Win Streak",
                            isUnlocked = stats.winStreak >= 3,
                            icon = Icons.Default.Star,
                            color = Color(0xFFFFC107)
                        ),
                        BadgeInfo(
                            id = "wealthy",
                            title = "Gold Miner",
                            desc = "Accumulate 1,500+ Coins",
                            isUnlocked = stats.coins >= 1500,
                            icon = Icons.Default.ShoppingCart,
                            color = Color(0xFFFF9800)
                        ),
                        BadgeInfo(
                            id = "mastery",
                            title = "Rising Master",
                            desc = "Achieved Level 2+",
                            isUnlocked = stats.level >= 2,
                            icon = Icons.Default.ThumbUp,
                            color = Color(0xFF2196F3)
                        ),
                        BadgeInfo(
                            id = "stylist",
                            title = "Royal Stylist",
                            desc = "Equipped a custom theme",
                            isUnlocked = stats.selectedTheme != "classic",
                            icon = Icons.Default.Favorite,
                            color = Color(0xFFE91E63)
                        ),
                        BadgeInfo(
                            id = "vocalist",
                            title = "Vocalist Pro",
                            desc = "Mic configured & active",
                            isUnlocked = stats.voiceChatEnabled,
                            icon = Icons.Default.Build,
                            color = Color(0xFF7B1FA2)
                        )
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        badgeList.chunked(2).forEach { rowBadges ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowBadges.forEach { badge ->
                                    var showDescToast by remember { mutableStateOf(false) }

                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                Toast.makeText(
                                                    context,
                                                    "${badge.title}: ${badge.desc} (${if (badge.isUnlocked) "Unlocked!" else "Locked"})",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                            .border(
                                                1.dp,
                                                if (badge.isUnlocked) badge.color.copy(alpha = 0.5f) else BentoBorder.copy(alpha = 0.5f),
                                                RoundedCornerShape(16.dp)
                                            ),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (badge.isUnlocked) badge.color.copy(alpha = 0.04f) else BentoSecondaryContainer.copy(alpha = 0.2f)
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .background(
                                                        color = if (badge.isUnlocked) badge.color.copy(alpha = 0.15f) else Color.LightGray.copy(alpha = 0.3f),
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (badge.isUnlocked) badge.icon else Icons.Default.Lock,
                                                    contentDescription = badge.title,
                                                    tint = if (badge.isUnlocked) badge.color else Color.Gray,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Column {
                                                Text(
                                                    text = badge.title,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (badge.isUnlocked) BentoText else Color.Gray,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = if (badge.isUnlocked) "Unlocked" else "Locked",
                                                    fontSize = 10.sp,
                                                    color = if (badge.isUnlocked) badge.color else Color.Gray,
                                                    fontWeight = FontWeight.SemiBold
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

            // Save changes floating bar
            Button(
                onClick = {
                    if (nickname.isNotBlank()) {
                        viewModel.updateProfile(
                            nickname = nickname,
                            avatarIndex = selectedAvatarIdx,
                            useCustomAvatar = useCustomAvatar,
                            customAvatarBg = customBg,
                            customAvatarIcon = customIcon,
                            customAvatarIconColor = customIconColor,
                            customAvatarBorder = customBorder
                        )
                        Toast.makeText(context, "Changes Saved Successfully!", Toast.LENGTH_SHORT).show()
                        onBack()
                    } else {
                        Toast.makeText(context, "Nickname cannot be empty", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = BentoPrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_profile_button_wide")
            ) {
                Text(
                    text = "SAVE STUDIO CHANGES",
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    fontSize = 14.sp
                )
            }
        }
    }
}

data class BadgeInfo(
    val id: String,
    val title: String,
    val desc: String,
    val isUnlocked: Boolean,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)
