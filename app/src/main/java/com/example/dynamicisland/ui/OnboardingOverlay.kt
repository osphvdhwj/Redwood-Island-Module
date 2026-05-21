package com.example.dynamicisland.ui

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun OnboardingOverlay(
    onDismiss: () -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    
    val steps = listOf(
        OnboardingStep(
            title = "Tap to Expand",
            description = "Quickly tap the island to expand notifications or media controls.",
            icon = Icons.Default.TouchApp
        ),
        OnboardingStep(
            title = "Swipe Up to Dismiss",
            description = "Flick the island upwards to quickly dismiss an alert and return to the ring.",
            icon = Icons.Default.ArrowUpward
        ),
        OnboardingStep(
            title = "Pull Down for More",
            description = "Drag the island downwards for an elastic stretch to reveal maximum details.",
            icon = Icons.Default.ArrowDownward
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { /* Block clicks behind overlay */ },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1E1E1E))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // Icon Animation
            val infiniteTransition = rememberInfiniteTransition(label = "icon_anim")
            val iconOffset by infiniteTransition.animateFloat(
                initialValue = -10f,
                targetValue = 10f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "icon_offset"
            )

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> -width } + fadeOut())
                },
                label = "onboarding_step"
            ) { stepIndex ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = steps[stepIndex].icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(64.dp)
                            .offset(y = if (stepIndex > 0) iconOffset.dp else 0.dp) // Only bounce the arrows
                            .padding(bottom = 16.dp)
                    )
                    
                    Text(
                        text = steps[stepIndex].title,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = steps[stepIndex].description,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Progress Dots
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(steps.size) { index ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (currentStep == index) Color.White 
                                    else Color.White.copy(alpha = 0.3f)
                                )
                        )
                    }
                }

                // Next / Got It Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White)
                        .clickable {
                            if (currentStep < steps.size - 1) {
                                currentStep++
                            } else {
                                onDismiss()
                            }
                        }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = if (currentStep < steps.size - 1) "Next" else "Got It",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

private data class OnboardingStep(
    val title: String,
    val description: String,
    val icon: ImageVector
)