package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DiceState
import com.example.model.LudoColor
import com.example.model.GameMode
import com.example.model.Player
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onBackToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.gameState.collectAsState()
    val stats by viewModel.userStats.collectAsState()
    val userVoiceVol by viewModel.userVoiceVolume.collectAsState()
    val context = LocalContext.current

    // Set up mic permission request flow
    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleLocalVoiceChat(true)
            viewModel.startMicPolling(context)
        } else {
            viewModel.toggleLocalVoiceChat(false)
            android.widget.Toast.makeText(context, "Voice chat requires microphone permission", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // Toggle voice chat safely
    fun handleVoiceChatToggle(enabled: Boolean) {
        if (enabled) {
            val hasPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (hasPerm) {
                viewModel.toggleLocalVoiceChat(true)
                viewModel.startMicPolling(context)
            } else {
                micLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
            }
        } else {
            viewModel.toggleLocalVoiceChat(false)
            viewModel.stopMicPolling()
        }
    }

    // Safely pull microphone polling on lifecycle if enabled
    LaunchedEffect(stats.voiceChatEnabled) {
        if (stats.voiceChatEnabled) {
            val hasPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (hasPerm) {
                viewModel.startMicPolling(context)
            }
        } else {
            viewModel.stopMicPolling()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopMicPolling()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = when (state.mode) {
                                GameMode.LOCAL -> "Pass & Play"
                                GameMode.AI_PLAY -> "VS Bots Practice"
                                GameMode.MULTIPLAYER -> "Online Match"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Theme: ${stats.selectedTheme.replaceFirstChar { it.uppercase() }}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackToMenu, modifier = Modifier.testTag("back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit to Menu")
                    }
                },
                actions = {
                    // Voice Chat Action Toggle (Simulating Mic and Waveforms)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = if (stats.voiceChatEnabled) "Voice On" else "Voice Off",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (stats.voiceChatEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Switch(
                            checked = stats.voiceChatEnabled,
                            onCheckedChange = { handleVoiceChatToggle(it) },
                            modifier = Modifier.scale(0.8f).testTag("voice_chat_toggle")
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // 1. PLAYERS GRID WITH DYNAMIC VOICE CHAT PULSATION
            PlayersDashboard(
                players = state.players,
                currentPlayerColor = state.currentPlayer.color,
                localVolume = userVoiceVol,
                voiceChatGlobalEnabled = stats.voiceChatEnabled
            )

            // 2. LUDO BOARD BASE
            LudoBoard(
                tokens = state.tokens,
                currentPlayerColor = state.currentPlayer.color,
                isWaitingForMove = state.diceState == DiceState.ROLLED_WAITING_FOR_MOVE,
                themeName = stats.selectedTheme,
                onTokenClick = { color, tokenId -> viewModel.moveToken(color, tokenId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            )

            // 3. GAME CONSOLE & DICE CONTROLLER
            DiceControlConsole(
                currentPlayer = state.currentPlayer,
                diceValue = state.diceValue,
                diceState = state.diceState,
                onRollClick = { viewModel.rollDice() }
            )

            // 4. QUICK CHAT PANEL (Only in Simulated Online Mode)
            if (state.mode == GameMode.MULTIPLAYER) {
                QuickChatPanel(onPhraseClick = { phrase -> viewModel.sendLocalQuickChat(phrase) })
            }

            // 5. LIVE GAME LOG TERMINAL
            LogsTerminal(logs = state.logs)
        }
    }
}

@Composable
fun PlayersDashboard(
    players: List<Player>,
    currentPlayerColor: LudoColor,
    localVolume: Float,
    voiceChatGlobalEnabled: Boolean
) {
    val avatars = listOf(
        Icons.Default.Face, Icons.Default.Person, Icons.Default.AccountBox, Icons.Default.Star,
        Icons.Default.Build, Icons.Default.Favorite, Icons.Default.ThumbUp, Icons.Default.Warning
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            players.forEach { player ->
                val isCurrent = player.color == currentPlayerColor
                val playerBaseColor = getLudoHexColor(player.color)
                
                val borderThickness by animateDpAsState(
                    targetValue = if (isCurrent) 3.dp else 1.dp,
                    label = "border_thickness"
                )
                val borderColor by animateColorAsState(
                    targetValue = if (isCurrent) playerBaseColor else Color.Transparent,
                    label = "border_color"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Avatar Plate with Voice Ripple Waveforms
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (player.isSpeaking && voiceChatGlobalEnabled) {
                                        playerBaseColor.copy(alpha = 0.15f)
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    }
                                )
                                .border(borderThickness, borderColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            // Audio visualizer bars (if speaking)
                            if (player.isSpeaking && voiceChatGlobalEnabled) {
                                val vol = if (player.color == LudoColor.GREEN) localVolume else player.activeVoiceVolume
                                AudioRippleIndicator(volume = vol, color = playerBaseColor)
                            } else {
                                LudoAvatar(
                                    player = player,
                                    avatarSize = 48.dp,
                                    iconSize = 28.dp
                                )
                            }
                        }

                        // Player details
                        Text(
                            text = player.name,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            color = if (isCurrent) playerBaseColor else MaterialTheme.colorScheme.onSurface
                        )

                        // Color Indicator Badge
                        Box(
                            modifier = Modifier
                                .size(12.dp, 6.dp)
                                .background(playerBaseColor, shape = RoundedCornerShape(2.dp))
                        )
                    }

                    // Floating Speech bubble popup
                    if (player.speechBubble != null) {
                        Surface(
                            modifier = Modifier
                                .offset(y = (-36).dp)
                                .shadow(4.dp, shape = RoundedCornerShape(8.dp)),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            tonalElevation = 4.dp
                        ) {
                            Text(
                                text = player.speechBubble,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AudioRippleIndicator(volume: Float, color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "audio_ripple")
    val height1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h1"
    )
    val height2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h2"
    )
    val height3 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h3"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 6.dp)
    ) {
        val scaledVol = (volume * 1.5f).coerceIn(0.1f, 1f)
        Box(modifier = Modifier.size(3.dp, 24.dp * height1 * scaledVol).background(color, shape = CircleShape))
        Box(modifier = Modifier.size(3.dp, 24.dp * height2 * scaledVol).background(color, shape = CircleShape))
        Box(modifier = Modifier.size(3.dp, 24.dp * height3 * scaledVol).background(color, shape = CircleShape))
    }
}

@Composable
fun DiceControlConsole(
    currentPlayer: Player,
    diceValue: Int,
    diceState: DiceState,
    onRollClick: () -> Unit
) {
    val colorHex = getLudoHexColor(currentPlayer.color)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(10.dp).background(colorHex, shape = CircleShape))
                    Text(
                        text = "${currentPlayer.name}'s Turn",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = colorHex
                    )
                }
                
                Text(
                    text = when (diceState) {
                        DiceState.IDLE -> "Roll the Lucky Die!"
                        DiceState.ROLLING -> "Casting dice..."
                        DiceState.ROLLED_WAITING_FOR_MOVE -> "Choose an active token to advance!"
                        DiceState.NO_MOVE_AVAILABLE -> "No possible moves! Turning over."
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            // Premium 3D Die representation
            val canRoll = diceState == DiceState.IDLE && !currentPlayer.isBot
            
            val scale by animateFloatAsState(
                targetValue = if (diceState == DiceState.ROLLING) 1.15f else 1f,
                animationSpec = spring(dampingRatio = 0.5f, stiffness = 120f),
                label = "dice_scale"
            )

            Box(
                modifier = Modifier
                    .scale(scale)
                    .size(56.dp)
                    .shadow(4.dp, shape = RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = if (canRoll) {
                                listOf(Color.White, Color(0xFFE0E0E0))
                            } else {
                                listOf(colorHex.copy(alpha = 0.9f), colorHex)
                            }
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 2.dp,
                        color = if (canRoll) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable(enabled = canRoll, onClick = onRollClick)
                    .testTag("dice_roll_button"),
                contentAlignment = Alignment.Center
            ) {
                // Render pips based on diceValue (1..6)
                RenderDiceFacePips(value = diceValue, pipColor = if (canRoll) Color.Black else Color.White)
            }
        }
    }
}

@Composable
fun RenderDiceFacePips(value: Int, pipColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        when (value) {
            1 -> {
                Box(modifier = Modifier.size(8.dp).background(pipColor, CircleShape).align(Alignment.Center))
            }
            2 -> {
                Box(modifier = Modifier.size(7.dp).background(pipColor, CircleShape).align(Alignment.TopStart))
                Box(modifier = Modifier.size(7.dp).background(pipColor, CircleShape).align(Alignment.BottomEnd))
            }
            3 -> {
                Box(modifier = Modifier.size(7.dp).background(pipColor, CircleShape).align(Alignment.TopStart))
                Box(modifier = Modifier.size(7.dp).background(pipColor, CircleShape).align(Alignment.Center))
                Box(modifier = Modifier.size(7.dp).background(pipColor, CircleShape).align(Alignment.BottomEnd))
            }
            4 -> {
                Box(modifier = Modifier.size(7.dp).background(pipColor, CircleShape).align(Alignment.TopStart))
                Box(modifier = Modifier.size(7.dp).background(pipColor, CircleShape).align(Alignment.TopEnd))
                Box(modifier = Modifier.size(7.dp).background(pipColor, CircleShape).align(Alignment.BottomStart))
                Box(modifier = Modifier.size(7.dp).background(pipColor, CircleShape).align(Alignment.BottomEnd))
            }
            5 -> {
                Box(modifier = Modifier.size(7.dp).background(pipColor, CircleShape).align(Alignment.TopStart))
                Box(modifier = Modifier.size(7.dp).background(pipColor, CircleShape).align(Alignment.TopEnd))
                Box(modifier = Modifier.size(7.dp).background(pipColor, CircleShape).align(Alignment.Center))
                Box(modifier = Modifier.size(7.dp).background(pipColor, CircleShape).align(Alignment.BottomStart))
                Box(modifier = Modifier.size(7.dp).background(pipColor, CircleShape).align(Alignment.BottomEnd))
            }
            6 -> {
                Box(modifier = Modifier.size(7.dp).background(pipColor, CircleShape).align(Alignment.TopStart))
                Box(modifier = Modifier.size(7.dp).background(pipColor, CircleShape).align(Alignment.TopEnd))
                Box(modifier = Modifier.size(7.dp).background(pipColor, CircleShape).align(Alignment.CenterStart))
                Box(modifier = Modifier.size(7.dp).background(pipColor, CircleShape).align(Alignment.CenterEnd))
                Box(modifier = Modifier.size(7.dp).background(pipColor, CircleShape).align(Alignment.BottomStart))
                Box(modifier = Modifier.size(7.dp).background(pipColor, CircleShape).align(Alignment.BottomEnd))
            }
        }
    }
}

@Composable
fun QuickChatPanel(onPhraseClick: (String) -> Unit) {
    val phrases = listOf("Roll a 6! 🎲", "Oops! 😮", "Good Game! 🤝", "Wow! 🔥", "Hurry up! ⏳")
    
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "QUICK CHAT EMOTES",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.Start)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            phrases.forEach { phrase ->
                Button(
                    onClick = { onPhraseClick(phrase) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp).testTag("quick_chat_$phrase"),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(text = phrase, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun LogsTerminal(logs: List<com.example.model.GameLog>) {
    val listState = rememberLazyListState()

    // Keep scrolled to latest logs automatically
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "LIVE ARENA LOGS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.Start)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = MaterialTheme.shapes.small
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(logs) { log ->
                    val color = if (log.color != null) getLudoHexColor(log.color) else MaterialTheme.colorScheme.onSurfaceVariant
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "•",
                            color = color,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = log.message,
                            fontSize = 11.sp,
                            color = color,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}

fun getLudoHexColor(color: LudoColor): Color {
    return when (color) {
        LudoColor.GREEN -> Color(0xFF2E7D32)
        LudoColor.RED -> Color(0xFFC62828)
        LudoColor.YELLOW -> Color(0xFFFBC02D)
        LudoColor.BLUE -> Color(0xFF1565C0)
    }
}
