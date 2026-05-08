package com.dartdl.app.ui.page

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dartdl.app.R
import kotlinx.coroutines.delay

@Composable
fun SplashPage(onNavigateToHome: () -> Unit) {
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
        delay(1200)
        onNavigateToHome()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2D1B1B), // Darker reddish-brown at top
                        Color(0xFF1A0F0F), // Near black at bottom
                    )
                )
            )
    ) {
        // Background Patterns (Low Opacity Icons)
        // Top Left - Clapper/Popcorn area
        Icon(
            imageVector = Icons.Default.Movie,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.05f),
            modifier = Modifier
                .size(140.dp)
                .offset(x = (-20).dp, y = 40.dp)
                .rotate(-15f)
        )

        // Top Right - Large Play Icon
        Icon(
            imageVector = Icons.Default.PlayCircle,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.05f),
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-40).dp)
        )

        // Bottom Right - Film Strip effect (using multiple PlayArrows)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 20.dp, y = 20.dp)
                .rotate(-30f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            repeat(3) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.05f),
                    modifier = Modifier.size(80.dp)
                )
            }
        }

        // Main Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .alpha(alpha.value)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_splash_custom),
                contentDescription = null,
                modifier = Modifier.size(160.dp),
                contentScale = ContentScale.Fit
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = stringResource(id = R.string.video_downloader),
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = stringResource(id = R.string.one_click_fast_download),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}
