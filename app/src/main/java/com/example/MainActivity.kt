package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.data.DatingDatabase
import com.example.data.DatingRepository
import com.example.ui.DatingAppScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize local Room Database & Repository
        val database = DatingDatabase.getDatabase(applicationContext, lifecycleScope)
        val repository = DatingRepository(database.datingDao())

        // Instantiate ViewModel with Custom Factory
        val viewModel: DatingViewModel by viewModels {
            DatingViewModelFactory(repository)
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    DatingAppScreen(viewModel = viewModel)
                }
            }
        }
    }
}
