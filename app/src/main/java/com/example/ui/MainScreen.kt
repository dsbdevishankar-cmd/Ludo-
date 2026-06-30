package com.example.ui

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameMode
import com.example.model.Player
import com.example.model.LudoColor
import com.example.ui.theme.BentoBg
import com.example.ui.theme.BentoBorder
import com.example.ui.theme.BentoPrimary
import com.example.ui.theme.BentoPrimaryContainer
import com.example.ui.theme.BentoOnPrimaryContainer
import com.example.ui.theme.BentoSecondaryContainer
import com.example.ui.theme.BentoPinkContainer
import com.example.ui.theme.BentoPinkText
import com.example.ui.theme.BentoText
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: GameViewModel,
    onStartGame: (GameMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.userStats.collectAsState()
    val matchmaking by viewModel.matchmakingState.collectAsState()
    val gameState by viewModel.gameState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var activeSubScreen by remember { mutableStateOf(MainSubScreen.DASHBOARD) }

    // Permission launcher for voice chat
    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleLocalVoiceChat(true)
        } else {
            viewModel.toggleLocalVoiceChat(false)
            android.widget.Toast.makeText(context, "Voice chat requires microphone permission", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun handleVoiceChatToggle(enabled: Boolean) {
        if (enabled) {
            val hasPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (hasPerm) {
                viewModel.toggleLocalVoiceChat(true)
            } else {
                micLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
            }
        } else {
            viewModel.toggleLocalVoiceChat(false)
        }
    }

    Scaffold(
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BentoBg)
        ) {
            when (activeSubScreen) {
                MainSubScreen.DASHBOARD -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                
                // 1. TOP APP BAR (Material Design 3 / Bento Header)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile/Avatar & Nickname
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .clickable { activeSubScreen = MainSubScreen.PROFILE }
                            .testTag("profile_button")
                    ) {
                        LudoAvatar(
                            player = Player(
                                color = LudoColor.GREEN,
                                name = stats.nickname,
                                isBot = false,
                                avatarIndex = stats.avatarIndex,
                                useCustomAvatar = stats.useCustomAvatar,
                                customAvatarBg = stats.customAvatarBg,
                                customAvatarIcon = stats.customAvatarIcon,
                                customAvatarIconColor = stats.customAvatarIconColor,
                                customAvatarBorder = stats.customAvatarBorder
                            ),
                            avatarSize = 44.dp,
                            iconSize = 24.dp
                        )
                        Column {
                            Text(
                                text = "Welcome back",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF49454F),
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = stats.nickname,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = BentoText
                            )
                        }
                    }

                    // Coins Display Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .background(BentoPrimaryContainer, shape = CircleShape)
                            .border(1.dp, Color(0xFFD0BCFF), shape = CircleShape)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Coins",
                            tint = BentoOnPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = String.format("%,d", stats.coins),
                            fontWeight = FontWeight.ExtraBold,
                            color = BentoOnPrimaryContainer,
                            fontSize = 14.sp
                        )
                    }
                }

                // 2. RESUME ONGOING GAME CARD (If exists)
                if (gameState.players.isNotEmpty() && gameState.winningSequence.isEmpty()) {
                    Card(
                        onClick = { onStartGame(gameState.mode) },
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFD8E4) // beautiful accent pink bento style
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFF9A8D4), shape = RoundedCornerShape(24.dp))
                            .testTag("resume_game_button"),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.White, shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                val infiniteTransition = rememberInfiniteTransition(label = "resume_spin")
                                val rotation by infiniteTransition.animateFloat(
                                    initialValue = 0f,
                                    targetValue = 360f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(4000, easing = LinearEasing),
                                        repeatMode = RepeatMode.Restart
                                    ),
                                    label = "rotation"
                                )
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Resume",
                                    tint = Color(0xFFBC1141),
                                    modifier = Modifier
                                        .size(20.dp)
                                        .graphicsLayer(rotationZ = rotation)
                                )
                            }
                            Column {
                                Text(
                                    text = "ACTIVE GAME DETECTED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFBC1141),
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Resume Ongoing Voyage (${gameState.mode.name})",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF21005D)
                                )
                            }
                        }
                    }
                }

                // 3. STATS & PROGRESSION BENTO WIDGET
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BentoBorder, shape = RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "YOUR STANDINGS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF49454F),
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Lvl ${stats.level} Master",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = BentoPrimary
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .background(BentoSecondaryContainer, shape = RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Ranked Pro",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoOnPrimaryContainer
                                )
                            }
                        }

                        // Experience progress
                        val xpNeeded = stats.level * 200
                        val progress = (stats.xp.toFloat() / xpNeeded).coerceIn(0f, 1f)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "XP Progression",
                                    fontSize = 12.sp,
                                    color = Color(0xFF49454F),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${stats.xp} / $xpNeeded",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoText
                                )
                            }
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = BentoPrimary,
                                trackColor = BentoSecondaryContainer
                            )
                        }

                        HorizontalDivider(color = BentoBorder.copy(alpha = 0.5f))

                        // Wins and rates
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val winRate = if (stats.matchesPlayed > 0) (stats.matchesWon * 100 / stats.matchesPlayed) else 0

                            StatsItem(label = "Played", value = "${stats.matchesPlayed}")
                            StatsItem(label = "Wins", value = "${stats.matchesWon}")
                            StatsItem(label = "Win Rate", value = "$winRate%")
                            
                            // Streak block
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Streak",
                                    fontSize = 11.sp,
                                    color = Color(0xFF49454F),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "${stats.winStreak}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (stats.winStreak > 0) Color(0xFF2E7D32) else BentoText
                                    )
                                    if (stats.winStreak > 0) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "On Streak",
                                            tint = Color(0xFFFFC107),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. MATCHMAKING LOBBY OR GAME MODE SELECT
                AnimatedVisibility(visible = matchmaking is GameViewModel.MatchmakingState.Searching) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BentoBorder, shape = RoundedCornerShape(24.dp)),
                        colors = CardDefaults.cardColors(containerColor = BentoPrimaryContainer),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            val infiniteTransition = rememberInfiniteTransition(label = "searching")
                            val scale by infiniteTransition.animateFloat(
                                initialValue = 0.92f,
                                targetValue = 1.08f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = EaseInOutSine),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "scale"
                            )

                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Searching",
                                tint = BentoPrimary,
                                modifier = Modifier
                                    .size(48.dp)
                                    .scale(scale)
                            )

                            Text(
                                text = "Finding Worldwide Match...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = BentoOnPrimaryContainer
                            )
                            Text(
                                text = "Connecting you with Ludo experts across the globe. Grab your lucky dice!",
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                color = BentoOnPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                if (matchmaking is GameViewModel.MatchmakingState.Idle) {
                    
                    // ONLINE MULTIPLAYER SECTION HEADER
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Online Mode",
                            tint = BentoPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "ONLINE MULTIPLAYER / ऑनलाइन मोड",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoPrimary,
                            letterSpacing = 1.sp
                        )
                    }

                    // 5. PRIMARY PLAY CARD: MULTIPLAYER LOBBY
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(BentoPrimary, shape = RoundedCornerShape(28.dp))
                            .clip(RoundedCornerShape(28.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), shape = RoundedCornerShape(28.dp))
                    ) {
                        // Abstract Ludo Shape Decorative Grid in Corner
                        Column(
                            modifier = Modifier
                                .size(120.dp)
                                .align(Alignment.BottomEnd)
                                .offset(x = 18.dp, y = 18.dp)
                                .graphicsLayer(alpha = 0.18f)
                        ) {
                            Row {
                                Box(modifier = Modifier.size(60.dp).padding(3.dp).background(Color(0xFFC62828), RoundedCornerShape(12.dp)))
                                Box(modifier = Modifier.size(60.dp).padding(3.dp).background(Color(0xFF1565C0), RoundedCornerShape(12.dp)))
                            }
                            Row {
                                Box(modifier = Modifier.size(60.dp).padding(3.dp).background(Color(0xFFFBC02D), RoundedCornerShape(12.dp)))
                                Box(modifier = Modifier.size(60.dp).padding(3.dp).background(Color(0xFF2E7D32), RoundedCornerShape(12.dp)))
                            }
                        }

                        // Content
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                // Green pulsing online badge
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val pulseTransition = rememberInfiniteTransition(label = "pulse")
                                    val pulseAlpha by pulseTransition.animateFloat(
                                        initialValue = 0.3f,
                                        targetValue = 1f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(800, easing = EaseInOutSine),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "pulse_alpha"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color.Green.copy(alpha = pulseAlpha), shape = CircleShape)
                                    )
                                    Text(
                                        text = "12,402 Players Online",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFEADDFF).copy(alpha = 0.9f)
                                    )
                                }

                                Text(
                                    text = "MULTIPLAYER\nLOBBY",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    fontStyle = FontStyle.Italic,
                                    color = Color(0xFFEADDFF),
                                    lineHeight = 28.sp,
                                    letterSpacing = (-0.5).sp
                                )
                            }

                            // PLAY NOW button
                            Button(
                                onClick = { viewModel.startOnlineMatchmaking() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFD0BCFF),
                                    contentColor = Color(0xFF21005D)
                                ),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                                modifier = Modifier
                                    .testTag("online_multiplayer_button")
                                    .height(44.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color(0xFF21005D),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "PLAY NOW",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
                    }

                    // OFFLINE MODES SECTION HEADER
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Offline Mode",
                            tint = BentoPinkText,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "OFFLINE PLAY MODES / ऑफलाइन मोड",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoPinkText,
                            letterSpacing = 1.sp
                        )
                    }

                    // 6. TWO-COLUMN BENTO WIDGETS (Practice vs Bots & Local Match)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // AI practice widget
                        BentoSmallActionWidget(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_practice_button"),
                            title = "AI Practice",
                            subtitle = "Practice offline",
                            category = "SOLO GAME",
                            iconColor = BentoPrimary,
                            iconBg = BentoSecondaryContainer,
                            icon = Icons.Default.Build,
                            onClick = { onStartGame(GameMode.AI_PLAY) }
                        )

                        // Local match widget
                        BentoSmallActionWidget(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("local_match_button"),
                            title = "Pass & Play",
                            subtitle = "Local sandbox",
                            category = "LOCAL GAME",
                            iconColor = BentoPinkText,
                            iconBg = BentoPinkContainer,
                            icon = Icons.Default.Face,
                            onClick = { onStartGame(GameMode.LOCAL) }
                        )
                    }

                    // 7. TWO-COLUMN UTILITY WIDGETS (Voice chat & Selected Theme)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Voice Chat widget
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, BentoBorder, shape = RoundedCornerShape(24.dp))
                                .clickable { handleVoiceChatToggle(!stats.voiceChatEnabled) },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(BentoSecondaryContainer, shape = RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star, // Microphone symbol representative
                                            contentDescription = null,
                                            tint = BentoPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    
                                    // Simulated simple toggle indicator
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp, 12.dp)
                                            .background(
                                                color = if (stats.voiceChatEnabled) BentoPrimary else Color(0xFFE0E0E0),
                                                shape = CircleShape
                                            )
                                    )
                                }

                                Column {
                                    Text(
                                        text = "VOICE CHAT",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF49454F),
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = if (stats.voiceChatEnabled) "Always On" else "Disabled",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BentoText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Current Theme widget
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, BentoBorder, shape = RoundedCornerShape(24.dp))
                                .clickable {
                                    coroutineScope.launch {
                                        scrollState.animateScrollTo(scrollState.maxValue)
                                    }
                                },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(BentoPinkContainer, shape = RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Favorite, // Palette symbol representative
                                            contentDescription = null,
                                            tint = BentoPinkText,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color(0xFF49454F),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = "BOARD THEME",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF49454F),
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = when (stats.selectedTheme) {
                                            "neon" -> "Cyber Neon"
                                            "wooden" -> "Zen Forest"
                                            "pastel" -> "Royal Pastel"
                                            else -> "Classic"
                                        },
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BentoText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // 8. THEME SHOP SECTION
                Text(
                    text = "BOARD THEMES & ACCENTS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = BentoPrimary,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(top = 8.dp, bottom = 4.dp)
                )

                val themes = listOf(
                    Triple("classic", "Classic Board", 0),
                    Triple("neon", "Cyber Neon", 300),
                    Triple("wooden", "Zen Forest", 500),
                    Triple("pastel", "Royal Pastel", 800)
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    themes.chunked(2).forEach { rowThemes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowThemes.forEach { (id, label, price) ->
                                val isSelected = stats.selectedTheme == id

                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            if (isSelected) return@clickable
                                            viewModel.selectTheme(
                                                themeName = id,
                                                price = price,
                                                onSuccess = {
                                                    addLogToLocalUI(context, "Theme $label selected successfully!")
                                                },
                                                onError = {
                                                    addLogToLocalUI(context, "Insufficient Coins! Need $price coins.")
                                                }
                                            )
                                        }
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) BentoPrimary else BentoBorder,
                                            shape = RoundedCornerShape(20.dp)
                                        )
                                        .testTag("theme_card_$id"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) Color(0xFFF3EDF7) else Color.White
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Theme circular thumbnail drawing
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .background(
                                                    brush = getThemeThumbnailBrush(id),
                                                    shape = CircleShape
                                                )
                                                .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                        )

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = label,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = BentoText
                                            )
                                            if (isSelected) {
                                                Text(
                                                    text = "Active Theme",
                                                    fontSize = 11.sp,
                                                    color = BentoPrimary,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            } else if (price == 0) {
                                                Text(
                                                    text = "Default (Free)",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF49454F)
                                                )
                                            } else {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ShoppingCart,
                                                        contentDescription = null,
                                                        tint = Color(0xFFFF9800),
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Text(
                                                        text = "$price Coins",
                                                        fontSize = 11.sp,
                                                        color = Color(0xFFFF9800),
                                                        fontWeight = FontWeight.Bold
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
                }
                }
                MainSubScreen.PROFILE -> {
                    ProfileScreen(
                        stats = stats,
                        viewModel = viewModel,
                        onBack = { activeSubScreen = MainSubScreen.DASHBOARD }
                    )
                }
            }
        }
    }
}

@Composable
fun StatsItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF49454F),
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            color = BentoText
        )
    }
}

@Composable
fun BentoSmallActionWidget(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    category: String,
    iconColor: Color,
    iconBg: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .border(1.dp, BentoBorder, shape = RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(iconBg, shape = RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color(0xFF49454F),
                    modifier = Modifier.size(14.dp)
                )
            }

            Column {
                Text(
                    text = category,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF49454F),
                    letterSpacing = 1.sp
                )
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoText
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Color(0xFF49454F),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun getThemeThumbnailBrush(themeId: String): Brush {
    return when (themeId) {
        "neon" -> Brush.sweepGradient(
            colors = listOf(Color(0xFF00FFCC), Color(0xFFFF007F), Color(0xFFFFEA00), Color(0xFF00E5FF), Color(0xFF00FFCC))
        )
        "wooden" -> Brush.sweepGradient(
            colors = listOf(Color(0xFF4CAF50), Color(0xFFD84315), Color(0xFFFFB300), Color(0xFF0277BD), Color(0xFF4CAF50))
        )
        "pastel" -> Brush.sweepGradient(
            colors = listOf(Color(0xFFA5D6A7), Color(0xFFEF9A9A), Color(0xFFFFF59D), Color(0xFF90CAF9), Color(0xFFA5D6A7))
        )
        else -> Brush.sweepGradient(
            colors = listOf(Color(0xFF2E7D32), Color(0xFFC62828), Color(0xFFFBC02D), Color(0xFF1565C0), Color(0xFF2E7D32))
        )
    }
}

private fun addLogToLocalUI(context: Context, text: String) {
    android.widget.Toast.makeText(context, text, android.widget.Toast.LENGTH_SHORT).show()
}

enum class MainSubScreen {
    DASHBOARD,
    PROFILE
}
