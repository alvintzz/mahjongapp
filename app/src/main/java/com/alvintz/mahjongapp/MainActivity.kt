package com.alvintz.mahjongapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.alvintz.mahjongapp.ui.theme.MahjongScorerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = (application as MahjongApplication).repository
        setContent {
            MahjongScorerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MahjongApp(repository)
                }
            }
        }
    }
}
