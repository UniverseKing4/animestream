package com.animestream.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.animestream.app.ui.details.DetailsScreen
import com.animestream.app.ui.home.HomeScreen
import com.animestream.app.ui.player.PlayerScreen
import com.animestream.app.ui.search.SearchScreen
import com.animestream.app.ui.theme.AnimeStreamTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            AnimeStreamTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    NavHost(navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                onAnimeClick = { id ->
                                    navController.navigate("details/$id")
                                },
                                onSearchClick = {
                                    navController.navigate("search")
                                }
                            )
                        }
                        
                        composable("search") {
                            SearchScreen(
                                onBackClick = { navController.popBackStack() },
                                onAnimeClick = { id ->
                                    navController.navigate("details/$id")
                                }
                            )
                        }
                        
                        composable("details/{animeId}") { backStackEntry ->
                            val animeId = backStackEntry.arguments?.getString("animeId") ?: ""
                            DetailsScreen(
                                animeId = animeId,
                                onBackClick = { navController.popBackStack() },
                                onEpisodeClick = { episodeId ->
                                    navController.navigate("player/$episodeId")
                                }
                            )
                        }
                        
                        composable("player/{episodeId}") { backStackEntry ->
                            val episodeId = backStackEntry.arguments?.getString("episodeId") ?: ""
                            PlayerScreen(
                                episodeId = episodeId,
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
