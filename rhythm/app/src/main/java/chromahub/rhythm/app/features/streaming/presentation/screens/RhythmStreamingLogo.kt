package chromahub.rhythm.app.features.streaming.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chromahub.rhythm.app.R

/**
 * Reusable composable to display Rhythm logo with name for streaming screens
 */
@Composable
fun RhythmStreamingLogoScreen(
    onSwitchToLocal: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo and Name in same line
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            // Rhythm Logo
            Image(
                painter = painterResource(id = R.drawable.rhythm_splash_logo),
                contentDescription = "Rhythm Logo",
                modifier = Modifier.size(100.dp)
            )
            
            Spacer(modifier = Modifier.width(2.dp))
            
            // Rhythm Name
            Text(
                text = "Rhythm Go",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 48.sp
            )
        }
        
//        Spacer(modifier = Modifier.height(8.dp))
//
//        // Go Branding
//        Text(
//            text = "Go",
//            style = MaterialTheme.typography.titleLarge,
//            fontWeight = FontWeight.SemiBold,
//            color = MaterialTheme.colorScheme.secondary,
//            fontSize = 24.sp,
//            letterSpacing = 2.sp
//        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Coming Soon
        Text(
            text = "Coming Soon",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 18.sp
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Switch to Local Button
        FilledTonalButton(
            onClick = onSwitchToLocal,
            modifier = Modifier
                .width(220.dp)
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Switch to Local",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
