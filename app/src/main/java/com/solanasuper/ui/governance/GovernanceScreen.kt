package com.solanasuper.ui.governance

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solanasuper.ui.components.MpcLoadingOverlay

@Composable
fun GovernanceScreen(
    viewModel: GovernanceViewModel
) {
    val state by viewModel.state.collectAsState()

    if (state.mpcState != com.solanasuper.ui.state.ArciumComputationState.IDLE) {
        MpcLoadingOverlay(state.mpcState)
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Democracy", 
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White.copy(alpha = 0.5f),
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) + 
                        slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessLow))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Proposal #1", 
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF14F195),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Universal Basic Income", 
                        style = MaterialTheme.typography.displayMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Should the network distribute 10 SOL monthly to verified humans?", 
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 18.sp,
                        lineHeight = 28.sp
                    )
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp), 
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { viewModel.initiateVote("YES") },
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF14F195))
                        ) {
                            Text("VOTE YES", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        
                        Button(
                            onClick = { viewModel.initiateVote("NO") },
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.1f)
                            )
                        ) {
                            Text("VOTE NO", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            AnimatedVisibility(visible = state.voteStatus.isNotEmpty() && state.voteStatus != "Active Proposal: Implement UBI") {
                 Text(
                    state.voteStatus, 
                    style = MaterialTheme.typography.bodyMedium, 
                    color = if (state.voteStatus.contains("Error") || state.voteStatus.contains("Failed")) Color(0xFFCF6679) else Color(0xFF14F195),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
