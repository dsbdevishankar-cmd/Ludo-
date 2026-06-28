package com.example.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LudoColor
import com.example.model.LudoToken
import com.example.model.LudoBoardMapper

data class BoardThemeConfig(
    val name: String,
    val background: Color,
    val cellBorder: Color,
    val cellDefault: Color,
    val green: Color,
    val red: Color,
    val yellow: Color,
    val blue: Color,
    val fontColor: Color,
    val cardBg: Color,
    val highlightColor: Color
)

object LudoThemes {
    val Classic = BoardThemeConfig(
        name = "classic",
        background = Color(0xFFFAF8F5),
        cellBorder = Color(0xFFCCCCCC),
        cellDefault = Color(0xFFFFFFFF),
        green = Color(0xFF2E7D32),
        red = Color(0xFFC62828),
        yellow = Color(0xFFFBC02D),
        blue = Color(0xFF1565C0),
        fontColor = Color(0xFF1C1B1F),
        cardBg = Color(0xFFFFFFFF),
        highlightColor = Color(0xFF00E676)
    )

    val CyberNeon = BoardThemeConfig(
        name = "neon",
        background = Color(0xFF0F101A),
        cellBorder = Color(0xFF232840),
        cellDefault = Color(0xFF131625),
        green = Color(0xFF00FFCC),
        red = Color(0xFFFF007F),
        yellow = Color(0xFFFFEA00),
        blue = Color(0xFF00E5FF),
        fontColor = Color(0xFFECEFF1),
        cardBg = Color(0xFF16192B),
        highlightColor = Color(0xFFE040FB)
    )

    val ZenWood = BoardThemeConfig(
        name = "wooden",
        background = Color(0xFF3E2723),
        cellBorder = Color(0xFF271510),
        cellDefault = Color(0xFFD7CCC8),
        green = Color(0xFF4CAF50),
        red = Color(0xFFD84315),
        yellow = Color(0xFFFFB300),
        blue = Color(0xFF0277BD),
        fontColor = Color(0xFFFFECB3),
        cardBg = Color(0xFF4E342E),
        highlightColor = Color(0xFF81C784)
    )

    val RoyalPastel = BoardThemeConfig(
        name = "pastel",
        background = Color(0xFFFBF9F4),
        cellBorder = Color(0xFFEFE8DD),
        cellDefault = Color(0xFFFFFDF9),
        green = Color(0xFFA5D6A7),
        red = Color(0xFFEF9A9A),
        yellow = Color(0xFFFFF59D),
        blue = Color(0xFF90CAF9),
        fontColor = Color(0xFF4E342E),
        cardBg = Color(0xFFFFFFFF),
        highlightColor = Color(0xFFB2DFDB)
    )

    fun getTheme(name: String): BoardThemeConfig {
        return when (name) {
            "neon" -> CyberNeon
            "wooden" -> ZenWood
            "pastel" -> RoyalPastel
            else -> Classic
        }
    }
}

@Composable
fun LudoBoard(
    tokens: List<LudoToken>,
    currentPlayerColor: LudoColor,
    isWaitingForMove: Boolean,
    themeName: String,
    onTokenClick: (LudoColor, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = remember(themeName) { LudoThemes.getTheme(themeName) }

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .shadow(8.dp, shape = MaterialTheme.shapes.medium)
            .background(theme.background)
            .border(2.dp, theme.cellBorder, shape = MaterialTheme.shapes.medium)
    ) {
        val boardWidth = maxWidth
        val cellSize = boardWidth / 15

        // 1. Draw Grid Base (Cells, Safe Areas, Homes)
        LudoBoardBase(theme = theme, cellSize = cellSize)

        // 2. Draw Center Goal Triangles
        CenterGoalTriangles(theme = theme, cellSize = cellSize)

        // 3. Draw Tokens (Grouped by Position to handle overlaps nicely)
        val activeTokens = tokens.filter { it.step <= 57 }
        
        // Group tokens by coordinate
        val groupedTokens = remember(activeTokens) {
            activeTokens.groupBy { token ->
                LudoBoardMapper.getCoordinates(token.color, token.step, token.id)
            }
        }

        groupedTokens.forEach { (coord, tokensInCell) ->
            val cellRow = coord.first
            val cellCol = coord.second

            // Determine local offsets for tokens in this cell to avoid overlapping
            val offsetList = remember(tokensInCell.size) {
                getTokenLocalOffsets(tokensInCell.size, cellSize)
            }

            tokensInCell.forEachIndexed { index, token ->
                val baseLeft = cellSize * cellCol
                val baseTop = cellSize * cellRow
                val localOffset = offsetList.getOrElse(index) { Pair(0.dp, 0.dp) }

                val targetX = baseLeft + localOffset.first
                val targetY = baseTop + localOffset.second

                // Physics spring animations for gorgeous token sliding
                val animX by animateDpAsState(
                    targetValue = targetX,
                    animationSpec = spring(stiffness = 150f, dampingRatio = 0.75f),
                    label = "token_x"
                )
                val animY by animateDpAsState(
                    targetValue = targetY,
                    animationSpec = spring(stiffness = 150f, dampingRatio = 0.75f),
                    label = "token_y"
                )

                val isSelectable = isWaitingForMove && token.color == currentPlayerColor && token.step < 57
                
                LudoTokenView(
                    token = token,
                    theme = theme,
                    cellSize = cellSize,
                    isSelectable = isSelectable,
                    onClick = { onTokenClick(token.color, token.id) },
                    modifier = Modifier
                        .offset(x = animX, y = animY)
                        .size(if (tokensInCell.size > 1) cellSize * 0.45f else cellSize * 0.8f)
                )
            }
        }
    }
}

@Composable
fun LudoBoardBase(theme: BoardThemeConfig, cellSize: Dp) {
    Column {
        for (row in 0 until 15) {
            Row {
                for (col in 0 until 15) {
                    val cellColor = getCellColor(row, col, theme)
                    val isBordered = shouldDrawCellBorder(row, col)

                    Box(
                        modifier = Modifier
                            .size(cellSize)
                            .drawBehind {
                                if (isBordered) {
                                    drawRect(
                                        color = theme.cellBorder,
                                        topLeft = Offset.Zero,
                                        size = size,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                                    )
                                }
                            }
                            .background(cellColor)
                    ) {
                        // Draw safe stars
                        if (LudoBoardMapper.safeCells.contains(Pair(row, col)) && !isHomeYard(row, col)) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Safe Zone",
                                tint = if (cellColor == theme.cellDefault) theme.cellBorder.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.8f),
                                modifier = Modifier
                                    .fillMaxSize(0.7f)
                                    .align(Alignment.Center)
                            )
                        }

                        // Draw Yard Circles inside Home Yards
                        if (isYardInnerPoint(row, col)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(cellSize * 0.15f)
                                    .background(Color.White.copy(alpha = 0.85f), shape = CircleShape)
                                    .border(1.5.dp, theme.cellBorder, shape = CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CenterGoalTriangles(theme: BoardThemeConfig, cellSize: Dp) {
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { 1.dp.toPx() }

    Box(
        modifier = Modifier
            .offset(x = cellSize * 6, y = cellSize * 6)
            .size(cellSize * 3)
            .background(Color.Transparent)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Left triangle (Green)
            val greenPath = Path().apply {
                moveTo(0f, 0f)
                lineTo(w / 2f, h / 2f)
                lineTo(0f, h)
                close()
            }
            drawPath(greenPath, theme.green)
            drawPath(greenPath, theme.cellBorder, style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidthPx))

            // Top triangle (Red)
            val redPath = Path().apply {
                moveTo(0f, 0f)
                lineTo(w / 2f, h / 2f)
                lineTo(w, 0f)
                close()
            }
            drawPath(redPath, theme.red)
            drawPath(redPath, theme.cellBorder, style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidthPx))

            // Right triangle (Yellow)
            val yellowPath = Path().apply {
                moveTo(w, 0f)
                lineTo(w / 2f, h / 2f)
                lineTo(w, h)
                close()
            }
            drawPath(yellowPath, theme.yellow)
            drawPath(yellowPath, theme.cellBorder, style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidthPx))

            // Bottom triangle (Blue)
            val bluePath = Path().apply {
                moveTo(0f, h)
                lineTo(w / 2f, h / 2f)
                lineTo(w, h)
                close()
            }
            drawPath(bluePath, theme.blue)
            drawPath(bluePath, theme.cellBorder, style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidthPx))
        }
    }
}

@Composable
fun LudoTokenView(
    token: LudoToken,
    theme: BoardThemeConfig,
    cellSize: Dp,
    isSelectable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val baseColor = when (token.color) {
        LudoColor.GREEN -> theme.green
        LudoColor.RED -> theme.red
        LudoColor.YELLOW -> theme.yellow
        LudoColor.BLUE -> theme.blue
    }

    val shadowElevation = if (isSelectable) 6.dp else 2.dp

    Box(
        modifier = modifier
            .shadow(shadowElevation, shape = CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(baseColor.copy(alpha = 0.9f), baseColor),
                    radius = 100f
                ),
                shape = CircleShape
            )
            .border(
                width = if (isSelectable) 2.5.dp else 1.dp,
                color = if (isSelectable) theme.highlightColor else Color.White.copy(alpha = 0.8f),
                shape = CircleShape
            )
            .clip(CircleShape)
            .clickable(
                enabled = isSelectable,
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = androidx.compose.material3.ripple(bounded = true, color = Color.White)
            )
            .testTag("token_${token.color.name.lowercase()}_${token.id}")
    ) {
        // Core stylish cap / inner circle
        Box(
            modifier = Modifier
                .fillMaxSize(0.5f)
                .background(Color.White.copy(alpha = 0.75f), shape = CircleShape)
                .align(Alignment.Center)
        ) {
            Text(
                text = "${token.id + 1}",
                color = baseColor,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

// HELPER FUNCTIONS FOR RENDERING THE 15x15 LUDO GRID
private fun getCellColor(row: Int, col: Int, theme: BoardThemeConfig): Color {
    // Center goals
    if (row in 6..8 && col in 6..8) {
        return Color.Transparent
    }

    // Home Yards
    if (row in 0..5 && col in 0..5) return theme.green
    if (row in 0..5 && col in 9..14) return theme.red
    if (row in 9..14 && col in 9..14) return theme.yellow
    if (row in 9..14 && col in 0..5) return theme.blue

    // Home corridors
    if (row == 7 && col in 1..5) return theme.green
    if (col == 7 && row in 1..5) return theme.red
    if (row == 7 && col in 9..13) return theme.yellow
    if (col == 7 && row in 9..13) return theme.blue

    // Starting positions
    if (row == 6 && col == 1) return theme.green
    if (row == 1 && col == 8) return theme.red
    if (row == 8 && col == 13) return theme.yellow
    if (row == 13 && col == 6) return theme.blue

    // Default cell
    return theme.cellDefault
}

private fun shouldDrawCellBorder(row: Int, col: Int): Boolean {
    // Don't draw individual cell borders inside home yards (keeps them clean solid)
    if (row in 0..5 && col in 0..5) return false
    if (row in 0..5 && col in 9..14) return false
    if (row in 9..14 && col in 9..14) return false
    if (row in 9..14 && col in 0..5) return false

    // Don't draw inside center Goal area
    if (row in 6..8 && col in 6..8) return false

    return true
}

private fun isHomeYard(row: Int, col: Int): Boolean {
    return (row in 0..5 && col in 0..5) ||
            (row in 0..5 && col in 9..14) ||
            (row in 9..14 && col in 9..14) ||
            (row in 9..14 && col in 0..5)
}

private fun isYardInnerPoint(row: Int, col: Int): Boolean {
    val points = setOf(
        Pair(2, 2), Pair(2, 3), Pair(3, 2), Pair(3, 3), // Green
        Pair(2, 11), Pair(2, 12), Pair(3, 11), Pair(3, 12), // Red
        Pair(11, 11), Pair(11, 12), Pair(12, 11), Pair(12, 12), // Yellow
        Pair(11, 2), Pair(11, 3), Pair(12, 2), Pair(12, 3) // Blue
    )
    return points.contains(Pair(row, col))
}

// TOKEN CLUSTERING LOGIC (To avoid direct stacking overlaps)
private fun getTokenLocalOffsets(count: Int, cellSize: Dp): List<Pair<Dp, Dp>> {
    val centerMargin = cellSize * 0.1f
    return when (count) {
        1 -> listOf(Pair(centerMargin, centerMargin))
        2 -> listOf(
            Pair(cellSize * 0.05f, cellSize * 0.25f),
            Pair(cellSize * 0.45f, cellSize * 0.25f)
        )
        3 -> listOf(
            Pair(cellSize * 0.25f, cellSize * 0.05f),
            Pair(cellSize * 0.05f, cellSize * 0.45f),
            Pair(cellSize * 0.45f, cellSize * 0.45f)
        )
        else -> listOf(
            Pair(cellSize * 0.05f, cellSize * 0.05f),
            Pair(cellSize * 0.45f, cellSize * 0.05f),
            Pair(cellSize * 0.05f, cellSize * 0.45f),
            Pair(cellSize * 0.45f, cellSize * 0.45f)
        )
    }
}
