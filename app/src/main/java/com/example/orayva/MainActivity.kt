package com.example.orayva

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.orayva.ui.Blue
import com.example.orayva.ui.ConvertTheme
import com.example.orayva.ui.DurPop
import com.example.orayva.ui.DurSheet
import com.example.orayva.ui.Hairline
import com.example.orayva.ui.HomeScreen
import com.example.orayva.ui.Ink
import com.example.orayva.ui.LocalToast
import com.example.orayva.ui.Mist
import com.example.orayva.ui.OrayvaToastLayer
import com.example.orayva.ui.PickerScreen
import com.example.orayva.ui.QueueScreen
import com.example.orayva.ui.Snow
import com.example.orayva.ui.SplashScreen
import com.example.orayva.ui.StudioScreen
import com.example.orayva.ui.Surface
import com.example.orayva.ui.ToastHost
import com.example.orayva.ui.VaultScreen
import com.example.orayva.ui.orayvaDrawer
import com.example.orayva.ui.orayvaOut
import com.example.orayva.ui.pressable

private data class Tab(val route: String, val label: String)

private val Tabs = listOf(
    Tab("home", "Home"),
    Tab("queue", "Queue"),
    Tab("vault", "Vault"),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ConvertTheme { App() } }
    }
}

@Composable
private fun App() {
    val nav = rememberNavController()
    val toast = remember { ToastHost() }
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "splash"
    val showBar = route in setOf("home", "queue", "vault")
    CompositionLocalProvider(LocalToast provides toast) {
        Box(Modifier.fillMaxSize().background(Ink)) {
            Scaffold(
                containerColor = Ink,
                bottomBar = {
                    if (showBar) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(Surface)
                                .border(width = 1.dp, color = Hairline)
                                .navigationBarsPadding()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Tabs.forEach { t ->
                                val selected = route == t.route
                                Column(
                                    Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .pressable {
                                            nav.navigate(t.route) {
                                                popUpTo("home") { inclusive = false }
                                                launchSingleTop = true
                                            }
                                        },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Box(
                                        Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (selected) Blue else Hairline),
                                    )
                                    Text(
                                        t.label,
                                        color = if (selected) Snow else Mist,
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                        modifier = Modifier.padding(top = 6.dp),
                                    )
                                }
                            }
                        }
                    }
                },
            ) { pad ->
                NavHost(
                    nav,
                    startDestination = "splash",
                    modifier = Modifier.padding(pad),
                    enterTransition = {
                        fadeIn(orayvaOut(DurPop)) + slideInHorizontally(orayvaOut(DurSheet)) { it / 18 }
                    },
                    exitTransition = {
                        fadeOut(orayvaOut(DurPop))
                    },
                    popEnterTransition = {
                        fadeIn(orayvaOut(DurPop))
                    },
                    popExitTransition = {
                        fadeOut(orayvaOut(DurPop)) + slideOutHorizontally(orayvaDrawer(DurSheet)) { it / 18 }
                    },
                ) {
                    composable("splash") {
                        SplashScreen { nav.navigate("home") { popUpTo("splash") { inclusive = true } } }
                    }
                    composable("home") { HomeScreen(onOpenType = { nav.navigate("studio/$it") }) }
                    composable(
                        "studio/{typeId}",
                        arguments = listOf(navArgument("typeId") { type = NavType.StringType }),
                    ) { e ->
                        StudioScreen(
                            typeId = e.arguments!!.getString("typeId")!!,
                            onPick = { nav.navigate("picker/${e.arguments!!.getString("typeId")!!}") },
                            onDone = { nav.navigate("queue") { popUpTo("home") } },
                            nav = nav,
                        )
                    }
                    composable(
                        "picker/{typeId}",
                        arguments = listOf(navArgument("typeId") { type = NavType.StringType }),
                    ) { e ->
                        PickerScreen(typeId = e.arguments!!.getString("typeId")!!, nav = nav)
                    }
                    composable("queue") { QueueScreen() }
                    composable("vault") { VaultScreen() }
                }
            }
            OrayvaToastLayer(toast)
        }
    }
}
