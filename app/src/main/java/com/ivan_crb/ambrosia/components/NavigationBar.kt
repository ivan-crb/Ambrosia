package com.ivan_crb.ambrosia.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ivan_crb.ambrosia.PlannerScreen
import com.ivan_crb.ambrosia.R
import com.ivan_crb.ambrosia.RecipesScreen
import com.ivan_crb.ambrosia.ShoppingScreen
import com.ivan_crb.ambrosia.StatsScreen
import com.ivan_crb.ambrosia.SettingsScreen
import com.ivan_crb.ambrosia.CreateRecipeScreen
import com.ivan_crb.ambrosia.RecipeDetailScreen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.IconButton
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivan_crb.ambrosia.viewmodel.RecipeViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.ui.graphics.vector.ImageVector

const val CREATE_RECIPE_ROUTE = "create_recipe"
const val VIEW_RECIPE_ROUTE = "view_recipe/{recipeId}"
const val EDIT_RECIPE_ROUTE = "edit_recipe/{recipeId}"

enum class Destination(
    val route: String,
    @StringRes val label: Int,
    val iconVector: ImageVector,
    @StringRes val contentDescription: Int
) {
    Planner("planner", R.string.nav_planner, Icons.Default.DateRange, R.string.nav_planner),
    Shopping("shopping", R.string.nav_shopping, Icons.Default.ShoppingCart, R.string.nav_shopping),
    Recipes("recipes", R.string.nav_recipes, Icons.Default.Description, R.string.nav_recipes),
    Stats("stats", R.string.nav_stats, Icons.Default.BarChart, R.string.nav_stats),
    Settings("settings", R.string.nav_settings, Icons.Default.Settings, R.string.nav_settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyNavigationBar(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = navBackStackEntry?.destination?.route

    val isFullScreen = currentRoute == CREATE_RECIPE_ROUTE || 
                       currentRoute?.startsWith("view_recipe") == true || 
                       currentRoute?.startsWith("edit_recipe") == true

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    val title = when {
                        currentRoute == CREATE_RECIPE_ROUTE -> stringResource(R.string.new_recipe)
                        currentRoute?.startsWith("view_recipe") == true -> stringResource(R.string.recipe_details)
                        currentRoute?.startsWith("edit_recipe") == true -> stringResource(R.string.edit_recipe)
                        else -> {
                            val dest = Destination.entries.find { it.route == currentRoute }
                            if (dest != null) stringResource(dest.label) else "Ambrosia"
                        }
                    }
                    Text(title)
                },
                navigationIcon = {
                    if (isFullScreen) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (!isFullScreen) {
                NavigationBar {
                    Destination.entries.forEach { item ->
                        val isSelected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = item.iconVector,
                                    contentDescription = stringResource(item.contentDescription),
                                )
                            },
                            label = { Text(stringResource(item.label)) },
                            selected = isSelected,
                            onClick = {
                                navController.navigate(item.route) {
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                }
            }
        }
    ) { contentPadding ->
        AppNavHost(navController, Destination.Planner, modifier = Modifier.padding(contentPadding))
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: Destination,
    modifier: Modifier = Modifier
) {
    val recipeViewModel: RecipeViewModel = viewModel()
    
    NavHost(
        navController,
        startDestination = startDestination.route,
        modifier = modifier
    ) {
        Destination.entries.forEach { destination ->
            composable(destination.route) {
                when (destination) {
                    Destination.Planner -> PlannerScreen(viewModel = recipeViewModel)
                    Destination.Recipes -> RecipesScreen(
                        viewModel = recipeViewModel,
                        onAddRecipe = { navController.navigate(CREATE_RECIPE_ROUTE) },
                        onRecipeClick = { id -> navController.navigate("view_recipe/$id") }
                    )
                    Destination.Shopping -> ShoppingScreen(viewModel = recipeViewModel)
                    Destination.Stats -> StatsScreen()
                    Destination.Settings -> SettingsScreen(viewModel = recipeViewModel)
                }
            }
        }
        composable(CREATE_RECIPE_ROUTE) {
            CreateRecipeScreen(
                viewModel = recipeViewModel,
                onSave = { navController.popBackStack() }
            )
        }
        composable(
            route = VIEW_RECIPE_ROUTE,
            arguments = listOf(navArgument("recipeId") { type = NavType.LongType })
        ) { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getLong("recipeId") ?: 0L
            RecipeDetailScreen(
                recipeId = recipeId,
                viewModel = recipeViewModel,
                onEditRecipe = { id: Long -> navController.navigate("edit_recipe/$id") }
            )
        }
        composable(
            route = EDIT_RECIPE_ROUTE,
            arguments = listOf(navArgument("recipeId") { type = NavType.LongType })
        ) { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getLong("recipeId") ?: 0L
            CreateRecipeScreen(
                recipeId = recipeId,
                viewModel = recipeViewModel,
                onSave = { navController.popBackStack() }
            )
        }
    }
}
