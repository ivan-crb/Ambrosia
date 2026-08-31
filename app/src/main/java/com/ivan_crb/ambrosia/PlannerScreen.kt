package com.ivan_crb.ambrosia

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.ivan_crb.ambrosia.data.Ingredient
import com.ivan_crb.ambrosia.data.Recipe
import com.ivan_crb.ambrosia.data.MealPlan
import com.ivan_crb.ambrosia.viewmodel.AggregatedShoppingItem
import com.ivan_crb.ambrosia.viewmodel.RecipeViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

fun formatDouble(value: Double): String {
    return if (value == value.toInt().toDouble()) {
        value.toInt().toString()
    } else {
        "%.2f".format(Locale.ENGLISH, value)
    }
}

@Composable
fun WeeklyNavigationHeader(viewModel: RecipeViewModel) {
    val weekStart by viewModel.currentWeekStart.collectAsState()
    val templates by viewModel.allTemplates.collectAsState()
    
    val weekEnd = weekStart.plusDays(6)
    val formatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())

    var showSaveDialog by remember { mutableStateOf(false) }
    var showApplyDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.previousWeek() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.previous_week))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${weekStart.format(formatter)} - ${weekEnd.format(formatter)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { viewModel.nextWeek() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.next_week))
            }
        }

        Row {
            IconButton(onClick = { showSaveDialog = true }) {
                Icon(Icons.Default.Save, contentDescription = stringResource(R.string.save_template))
            }
            IconButton(onClick = { showApplyDialog = true }) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = stringResource(R.string.apply_template))
            }
        }
    }

    if (showSaveDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text(stringResource(R.string.save_template)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.template_name)) },
                    placeholder = { Text(stringResource(R.string.enter_template_name)) },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotBlank()) {
                        viewModel.saveCurrentWeekAsTemplate(name)
                        showSaveDialog = false
                    }
                }) {
                    Text(stringResource(R.string.add))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showApplyDialog) {
        AlertDialog(
            onDismissRequest = { showApplyDialog = false },
            title = { Text(stringResource(R.string.select_template)) },
            text = {
                if (templates.isEmpty()) {
                    Text(stringResource(R.string.no_templates))
                } else {
                    LazyColumn {
                        items(templates) { template ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.applyTemplateToCurrentWeek(template.id)
                                        showApplyDialog = false
                                    }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(template.name, style = MaterialTheme.typography.bodyLarge)
                                IconButton(onClick = { viewModel.deleteTemplate(template.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showApplyDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun PlannerScreen(viewModel: RecipeViewModel) {
    val weekStart by viewModel.currentWeekStart.collectAsState()
    val mealPlans by viewModel.weeklyMealPlans.collectAsState()
    val recipes by viewModel.allRecipes.collectAsState()
    val settings by viewModel.userSettings.collectAsState()

    val visibleSlots = remember(settings.visibleMealSlots) { 
        settings.visibleMealSlots.split(",").filter { it.isNotBlank() }
    }

    var showRecipePicker by remember { mutableStateOf<Pair<LocalDate, String>?>(null) }
    var selectedRecipeForServings by remember { mutableStateOf<Pair<Pair<LocalDate, String>, Recipe>?>(null) }
    var editingMealPlanForServings by remember { mutableStateOf<MealPlan?>(null) }

    Scaffold(
        topBar = { WeeklyNavigationHeader(viewModel) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            items((0..6).map { weekStart.plusDays(it.toLong()) }) { date ->
                val dayPlans = mealPlans.filter { it.date == date.toString() }
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = date.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault()),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        visibleSlots.forEach { slot ->
                            val plan = dayPlans.find { it.slot == slot }
                            val recipe = recipes.find { it.id == plan?.recipeId }
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        if (plan == null) {
                                            showRecipePicker = date to slot
                                        } else {
                                            editingMealPlanForServings = plan
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$slot:",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.width(70.dp),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (recipe != null) "${recipe.name} (${plan?.plannedServings} serv.)" else stringResource(R.string.assign_recipe),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (recipe != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.weight(1f)
                                )
                                if (recipe != null) {
                                    IconButton(
                                        onClick = { viewModel.removeRecipeFromPlan(date.toString(), slot) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.remove),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRecipePicker != null) {
        AlertDialog(
            onDismissRequest = { showRecipePicker = null },
            title = { Text(stringResource(R.string.choose_recipe, showRecipePicker?.second ?: "")) },
            text = {
                LazyColumn {
                    items(recipes) { recipe ->
                        Text(
                            text = recipe.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedRecipeForServings = showRecipePicker!! to recipe
                                    showRecipePicker = null
                                }
                                .padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRecipePicker = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (selectedRecipeForServings != null) {
        val (location, recipe) = selectedRecipeForServings!!
        var servings by remember { mutableStateOf(settings.defaultServings.toString()) }

        AlertDialog(
            onDismissRequest = { selectedRecipeForServings = null },
            title = { Text(stringResource(R.string.how_many_servings)) },
            text = {
                OutlinedTextField(
                    value = servings,
                    onValueChange = { servings = it },
                    label = { Text(stringResource(R.string.number_of_people)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val numServings = servings.toIntOrNull() ?: settings.defaultServings
                        viewModel.addRecipeToPlan(location.first.toString(), location.second, recipe, numServings)
                        selectedRecipeForServings = null
                    }
                ) {
                    Text(stringResource(R.string.assign))
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedRecipeForServings = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (editingMealPlanForServings != null) {
        val plan = editingMealPlanForServings!!
        var servings by remember { mutableStateOf(plan.plannedServings.toString()) }

        AlertDialog(
            onDismissRequest = { editingMealPlanForServings = null },
            title = { Text(stringResource(R.string.update_servings)) },
            text = {
                OutlinedTextField(
                    value = servings,
                    onValueChange = { servings = it },
                    label = { Text(stringResource(R.string.number_of_people)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val numServings = servings.toIntOrNull() ?: plan.plannedServings
                        viewModel.updateMealPlanServings(plan, numServings)
                        editingMealPlanForServings = null
                    }
                ) {
                    Text(stringResource(R.string.update))
                }
            },
            dismissButton = {
                TextButton(onClick = { editingMealPlanForServings = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun ShoppingScreen(viewModel: RecipeViewModel) {
    val items by viewModel.weeklyShoppingItems.collectAsState()
    val settings by viewModel.userSettings.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    val scope = rememberCoroutineScope()
    var showAddItemDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { WeeklyNavigationHeader(viewModel) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddItemDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_item))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Grouping selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = stringResource(R.string.group_by), style = MaterialTheme.typography.labelLarge)
                listOf("Alphabetical", "Category").forEach { mode ->
                    val label = if (mode == "Alphabetical") stringResource(R.string.alphabetical) else stringResource(R.string.category)
                    FilterChip(
                        selected = settings.shoppingGrouping == mode,
                        onClick = { viewModel.updateShoppingGrouping(mode) },
                        label = { Text(label) }
                    )
                }
            }
            HorizontalDivider()

            if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.shopping_list_empty))
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    val groupedItems = if (settings.shoppingGrouping == "Category") {
                        items.groupBy { it.category }
                    } else {
                        mapOf("" to items)
                    }

                    groupedItems.forEach { (category, categoryItems) ->
                        if (category.isNotEmpty()) {
                            item {
                                val standardCategories = listOf("Produce", "Dairy", "Meat", "Pantry", "Frozen", "Bakery")
                                val categoryText = if (category in standardCategories) {
                                    val categoryRes = when(category) {
                                        "Produce" -> R.string.category_produce
                                        "Dairy" -> R.string.category_dairy
                                        "Meat" -> R.string.category_meat
                                        "Pantry" -> R.string.category_pantry
                                        "Frozen" -> R.string.category_frozen
                                        "Bakery" -> R.string.category_bakery
                                        else -> R.string.category_other
                                    }
                                    stringResource(categoryRes)
                                } else {
                                    category
                                }
                                Text(
                                    text = categoryText,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        items(categoryItems) { item ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.toggleShoppingItem(item) },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = item.isChecked,
                                        onCheckedChange = { viewModel.toggleShoppingItem(item) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            textDecoration = if (item.isChecked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = stringResource(R.string.to_buy, formatDouble(item.totalToBuy), item.unit),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                textDecoration = if (item.isChecked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                                            )
                                            if (item.totalRequired != item.totalToBuy) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = stringResource(R.string.total_required, formatDouble(item.totalRequired)),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }
                                    }
                                    
                                    // Subtle inline inventory
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Category click to change
                                        var showCategorySelector by remember { mutableStateOf(false) }
                                        val standardCategories = listOf("Produce", "Dairy", "Meat", "Pantry", "Frozen", "Bakery")
                                        val categoryText = if (item.category in standardCategories) {
                                            val categoryRes = when(item.category) {
                                                "Produce" -> R.string.category_produce
                                                "Dairy" -> R.string.category_dairy
                                                "Meat" -> R.string.category_meat
                                                "Pantry" -> R.string.category_pantry
                                                "Frozen" -> R.string.category_frozen
                                                "Bakery" -> R.string.category_bakery
                                                else -> R.string.category_other
                                            }
                                            stringResource(categoryRes)
                                        } else {
                                            item.category
                                        }
                                        
                                        Surface(
                                            onClick = { showCategorySelector = true },
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        ) {
                                            Text(
                                                text = categoryText,
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }

                                        if (showCategorySelector) {
                                            var showCustomCategoryInput by remember { mutableStateOf(false) }
                                            var customCategoryName by remember { mutableStateOf("") }

                                            if (!showCustomCategoryInput) {
                                                AlertDialog(
                                                    onDismissRequest = { showCategorySelector = false },
                                                    title = { Text(stringResource(R.string.category)) },
                                                    text = {
                                                        LazyColumn {
                                                            items(categories) { cat ->
                                                                val standardCategories = listOf("Produce", "Dairy", "Meat", "Pantry", "Frozen", "Bakery")
                                                                val label = if (cat in standardCategories) {
                                                                    val catRes = when (cat) {
                                                                        "Produce" -> R.string.category_produce
                                                                        "Dairy" -> R.string.category_dairy
                                                                        "Meat" -> R.string.category_meat
                                                                        "Pantry" -> R.string.category_pantry
                                                                        "Frozen" -> R.string.category_frozen
                                                                        "Bakery" -> R.string.category_bakery
                                                                        else -> R.string.category_other
                                                                    }
                                                                    stringResource(catRes)
                                                                } else if (cat == "Other") {
                                                                    stringResource(R.string.category_other)
                                                                } else {
                                                                    cat
                                                                }
                                                                
                                                                Text(
                                                                    text = label,
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .clickable {
                                                                            viewModel.updateIngredientCategory(item.name, cat)
                                                                            showCategorySelector = false
                                                                        }
                                                                        .padding(16.dp)
                                                                )
                                                            }
                                                            item {
                                                                Text(
                                                                    text = stringResource(R.string.category_custom),
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .clickable {
                                                                            showCustomCategoryInput = true
                                                                        }
                                                                        .padding(16.dp),
                                                                    color = MaterialTheme.colorScheme.primary,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                        }
                                                    },
                                                    confirmButton = {}
                                                )
                                            } else {
                                                AlertDialog(
                                                    onDismissRequest = { showCategorySelector = false },
                                                    title = { Text(stringResource(R.string.new_category)) },
                                                    text = {
                                                        OutlinedTextField(
                                                            value = customCategoryName,
                                                            onValueChange = { customCategoryName = it },
                                                            label = { Text(stringResource(R.string.category_name)) },
                                                            singleLine = true
                                                        )
                                                    },
                                                    confirmButton = {
                                                        Button(onClick = {
                                                            if (customCategoryName.isNotBlank()) {
                                                                viewModel.updateIngredientCategory(item.name, customCategoryName)
                                                                showCategorySelector = false
                                                            }
                                                        }) {
                                                            Text(stringResource(R.string.add))
                                                        }
                                                    },
                                                    dismissButton = {
                                                        TextButton(onClick = { showCustomCategoryInput = false }) {
                                                            Text(stringResource(R.string.back))
                                                        }
                                                    }
                                                )
                                            }
                                        }

                                        var haveAmount by remember(item.totalAtHome) { mutableStateOf(formatDouble(item.totalAtHome)) }
                                        Text(text = stringResource(R.string.have), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Box(modifier = Modifier.width(45.dp)) {
                                            OutlinedTextField(
                                                value = haveAmount,
                                                onValueChange = { newValue: String ->
                                                    haveAmount = newValue
                                                    newValue.toDoubleOrNull()?.let { qty ->
                                                        viewModel.setAtHomeQuantity(item, qty)
                                                    }
                                                },
                                                textStyle = MaterialTheme.typography.labelSmall,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                maxLines = 1,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                    
                                    IconButton(onClick = { viewModel.deleteShoppingItem(item) }) {
                                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                    item {
                        if (items.any { it.isChecked }) {
                            TextButton(
                                onClick = { viewModel.clearCheckedShoppingItems() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(stringResource(R.string.clear_checked))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddItemDialog) {
        var name by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("") }
        var unit by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Other") }
        var showCustomCategoryDialog by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddItemDialog = false },
            title = { Text(stringResource(R.string.add_shopping_item)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { newName ->
                            name = newName
                            scope.launch {
                                val suggested = viewModel.suggestCategory(newName)
                                if (category == "Other") {
                                    category = suggested
                                }
                            }
                        },
                        label = { Text(stringResource(R.string.item_name)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            label = { Text(stringResource(R.string.amount)) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text(stringResource(R.string.unit)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(R.string.category) + ":", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy((-12).dp)
                    ) {
                        val standardCategories = listOf("Produce", "Dairy", "Meat", "Pantry", "Frozen", "Bakery", "Other")
                        val displayedCategories = (categories + category.trim()).distinct()
                        displayedCategories.forEach { cat ->
                            val label = if (cat in standardCategories) {
                                val catRes = when (cat) {
                                    "Produce" -> R.string.category_produce
                                    "Dairy" -> R.string.category_dairy
                                    "Meat" -> R.string.category_meat
                                    "Pantry" -> R.string.category_pantry
                                    "Frozen" -> R.string.category_frozen
                                    "Bakery" -> R.string.category_bakery
                                    else -> R.string.category_other
                                }
                                stringResource(catRes)
                            } else {
                                cat
                            }
                            FilterChip(
                                selected = category.trim() == cat.trim(),
                                onClick = { category = cat },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                        
                        FilterChip(
                            selected = false,
                            onClick = { showCustomCategoryDialog = true },
                            label = { Text(stringResource(R.string.category_custom), style = MaterialTheme.typography.labelSmall) }
                        )
                    }

                    if (showCustomCategoryDialog) {
                        var customCatName by remember { mutableStateOf("") }
                        AlertDialog(
                            onDismissRequest = { showCustomCategoryDialog = false },
                            title = { Text(stringResource(R.string.new_category)) },
                            text = {
                                OutlinedTextField(
                                    value = customCatName,
                                    onValueChange = { customCatName = it },
                                    label = { Text(stringResource(R.string.category_name)) },
                                    singleLine = true
                                )
                            },
                            confirmButton = {
                                Button(onClick = {
                                    if (customCatName.isNotBlank()) {
                                        category = customCatName
                                        showCustomCategoryDialog = false
                                    }
                                }) {
                                    Text(stringResource(R.string.add))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showCustomCategoryDialog = false }) {
                                    Text(stringResource(R.string.cancel))
                                }
                            }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.addCustomShoppingItem(name, amount, unit, category)
                            showAddItemDialog = false
                        }
                    }
                ) {
                    Text(stringResource(R.string.add))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddItemDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun SettingsScreen(viewModel: RecipeViewModel) {
    val settings by viewModel.userSettings.collectAsState()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(text = stringResource(R.string.preferences), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Default Servings
        item {
            var defaultServings by remember(settings.defaultServings) { mutableStateOf(settings.defaultServings.toString()) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(R.string.default_servings), style = MaterialTheme.typography.bodyLarge)
                    Text(text = stringResource(R.string.default_servings_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
                OutlinedTextField(
                    value = defaultServings,
                    onValueChange = { 
                        defaultServings = it
                        it.toIntOrNull()?.let { qty -> viewModel.updateDefaultServings(qty) }
                    },
                    modifier = Modifier.width(80.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Start of Week
        item {
            Text(text = stringResource(R.string.start_of_week), style = MaterialTheme.typography.bodyLarge)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Monday", "Sunday").forEach { day ->
                    FilterChip(
                        selected = settings.startOfWeek == day,
                        onClick = { viewModel.updateStartOfWeek(day) },
                        label = { Text(day) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Meal Slots
        item {
            var showAddSlotDialog by remember { mutableStateOf(false) }
            
            Text(text = stringResource(R.string.visible_meal_slots), style = MaterialTheme.typography.bodyLarge)
            val currentSlots = settings.visibleMealSlots.split(",").filter { it.isNotBlank() }
            
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                currentSlots.forEach { slot ->
                    FilterChip(
                        selected = true,
                        onClick = {
                            val newSlots = currentSlots - slot
                            viewModel.updateVisibleMealSlots(newSlots.joinToString(","))
                        },
                        label = { Text(slot) },
                        trailingIcon = { Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
                
                OutlinedButton(
                    onClick = { showAddSlotDialog = true },
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.add_slot), style = MaterialTheme.typography.labelLarge)
                }
            }
            
            if (showAddSlotDialog) {
                var newSlotName by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { showAddSlotDialog = false },
                    title = { Text(stringResource(R.string.add_new_slot)) },
                    text = {
                        OutlinedTextField(
                            value = newSlotName,
                            onValueChange = { newSlotName = it },
                            label = { Text(stringResource(R.string.slot_name_hint)) },
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (newSlotName.isNotBlank()) {
                                val newSlots = currentSlots + newSlotName
                                viewModel.updateVisibleMealSlots(newSlots.joinToString(","))
                                showAddSlotDialog = false
                            }
                        }) {
                            Text(stringResource(R.string.add))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddSlotDialog = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Language
        item {
            Text(text = stringResource(R.string.language_label), style = MaterialTheme.typography.bodyLarge)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("System", "English", "Spanish").forEach { lang ->
                    val label = when(lang) {
                        "English" -> stringResource(R.string.language_english)
                        "Spanish" -> stringResource(R.string.language_spanish)
                        else -> stringResource(R.string.language_system)
                    }
                    FilterChip(
                        selected = settings.language == lang,
                        onClick = { viewModel.updateLanguage(lang) },
                        label = { Text(label) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Theme Mode
        item {
            Text(text = stringResource(R.string.theme_mode), style = MaterialTheme.typography.bodyLarge)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("System", "Light", "Dark").forEach { mode ->
                    FilterChip(
                        selected = settings.themeMode == mode,
                        onClick = { viewModel.updateThemeMode(mode) },
                        label = { Text(mode) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(text = stringResource(R.string.app_version, "1.0.0"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun RecipesScreen(
    viewModel: RecipeViewModel,
    onAddRecipe: () -> Unit,
    onRecipeClick: (Long) -> Unit
) {
    val recipes by viewModel.filteredRecipes.collectAsState()
    val allRecipes by viewModel.allRecipes.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTags by viewModel.selectedTags.collectAsState()
    val allTags by viewModel.allRecipeTags.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddRecipe) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_recipe))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text(stringResource(R.string.search_recipes)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Tag Filters
            if (allTags.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allTags) { tag ->
                        FilterChip(
                            selected = selectedTags.contains(tag),
                            onClick = { viewModel.toggleTagFilter(tag) },
                            label = { Text(tag) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (recipes.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val message = if (allRecipes.isEmpty()) {
                        stringResource(R.string.no_recipes)
                    } else {
                        stringResource(R.string.no_recipes_found)
                    }
                    Text(text = message)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    items(recipes) { recipe ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable { onRecipeClick(recipe.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (recipe.photoUri != null) {
                                    AsyncImage(
                                        model = recipe.photoUri,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                }
                                Column {
                                    Text(text = recipe.name, style = MaterialTheme.typography.titleMedium)
                                    Text(text = stringResource(R.string.servings_count, recipe.servings), style = MaterialTheme.typography.bodySmall)
                                    
                                    if (recipe.tags.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        @OptIn(ExperimentalLayoutApi::class)
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            recipe.tags.forEach { tag ->
                                                Surface(
                                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = tag,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@Composable
fun RecipeDetailScreen(
    recipeId: Long,
    viewModel: RecipeViewModel,
    onEditRecipe: (Long) -> Unit
) {
    val recipe by viewModel.getRecipe(recipeId).collectAsState(initial = null)

    if (recipe == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(onClick = { onEditRecipe(recipeId) }) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_recipe))
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                recipe?.photoUri?.let { uri ->
                    item {
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = recipe?.name ?: "", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(text = stringResource(R.string.serves_count, recipe?.servings ?: 0), style = MaterialTheme.typography.bodyLarge)
                        
                        if (recipe?.tags?.isNotEmpty() == true) {
                            Spacer(modifier = Modifier.height(8.dp))
                            @OptIn(ExperimentalLayoutApi::class)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                recipe?.tags?.forEach { tag ->
                                    SuggestionChip(
                                        onClick = { },
                                        label = { Text(tag) }
                                    )
                                }
                            }
                        }

                        if (recipe?.description?.isNotBlank() == true) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = stringResource(R.string.description_label), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(text = recipe?.description ?: "", style = MaterialTheme.typography.bodyMedium)
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(text = stringResource(R.string.ingredients_label), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        recipe?.ingredients?.forEach { ingredient ->
                            Text(text = "• ${ingredient.amount} ${ingredient.unit} ${ingredient.name}", style = MaterialTheme.typography.bodyMedium)
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(text = stringResource(R.string.steps_label), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        recipe?.steps?.forEachIndexed { index, step ->
                            Text(text = "${index + 1}. $step", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateRecipeScreen(
    recipeId: Long = 0,
    viewModel: RecipeViewModel,
    onSave: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var servings by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    
    val categories by viewModel.allCategories.collectAsState()
    val ingredients = remember { mutableStateListOf<Ingredient>() }
    val steps = remember { mutableStateListOf<String>() }
    val tags = remember { mutableStateListOf<String>() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Load existing data if editing
    LaunchedEffect(recipeId) {
        if (recipeId != 0L) {
            viewModel.getRecipe(recipeId).collect { recipe ->
                recipe?.let {
                    name = it.name
                    description = it.description
                    servings = it.servings.toString()
                    photoUri = it.photoUri?.let { path -> Uri.fromFile(File(path)) }
                    ingredients.clear()
                    ingredients.addAll(it.ingredients)
                    steps.clear()
                    steps.addAll(it.steps)
                    tags.clear()
                    tags.addAll(it.tags)
                }
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) photoUri = uri }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success -> if (success) { /* photoUri is already set */ } }
    )

    fun takePhoto() {
        val fileName = "temp_camera_${System.currentTimeMillis()}.jpg"
        val file = File(context.externalCacheDir, fileName)
        val uri = FileProvider.getUriForFile(
            context,
            "com.ivan_crb.ambrosia.fileprovider",
            file
        )
        photoUri = uri
        cameraLauncher.launch(uri)
    }

    // Validation
    val isNameValid = name.isNotBlank()
    val isServingsValid = servings.isNotBlank() && servings.toIntOrNull() != null
    val isIngredientsValid = ingredients.isNotEmpty() && ingredients.all { it.name.isNotBlank() && it.amount.isNotBlank() }
    val isStepsValid = steps.isNotEmpty() && steps.all { it.isNotBlank() }
    
    val canSave = isNameValid && isServingsValid && isIngredientsValid && isStepsValid

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(text = stringResource(R.string.basic_info), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.recipe_name_req)) },
                modifier = Modifier.fillMaxWidth(),
                isError = name.isBlank()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.description_opt)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = servings,
                onValueChange = { servings = it },
                label = { Text(stringResource(R.string.servings_req)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = servings.isNotBlank() && servings.toIntOrNull() == null
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Tags Editor
            Text(text = stringResource(R.string.tags), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            var newTag by remember { mutableStateOf("") }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newTag,
                    onValueChange = { newTag = it },
                    label = { Text(stringResource(R.string.add_tag)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                IconButton(onClick = {
                    if (newTag.isNotBlank() && !tags.contains(newTag)) {
                        tags.add(newTag.trim())
                        newTag = ""
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }
            if (tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tags.forEach { tag ->
                        InputChip(
                            selected = true,
                            onClick = { tags.remove(tag) },
                            label = { Text(tag) },
                            trailingIcon = { Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(modifier = Modifier.fillMaxWidth()) {
                if (photoUri != null) {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = "Preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { takePhoto() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.camera))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.gallery))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(R.string.ingredients_label), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { ingredients.add(Ingredient("", "", "", "Other")) }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }
        }

        itemsIndexed(ingredients) { index, ingredient ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = ingredient.name,
                        onValueChange = { newName ->
                            ingredients[index] = ingredient.copy(name = newName)
                            // Auto-categorize
                            scope.launch {
                                val suggested = viewModel.suggestCategory(newName)
                                if (ingredients[index].category == "Other") {
                                    ingredients[index] = ingredients[index].copy(category = suggested)
                                }
                            }
                        },
                        label = { Text(stringResource(R.string.ingredient)) },
                        modifier = Modifier.weight(2f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    OutlinedTextField(
                        value = ingredient.amount,
                        onValueChange = { ingredients[index] = ingredient.copy(amount = it) },
                        label = { Text(stringResource(R.string.qty)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    OutlinedTextField(
                        value = ingredient.unit,
                        onValueChange = { ingredients[index] = ingredient.copy(unit = it) },
                        label = { Text(stringResource(R.string.unit)) },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { ingredients.removeAt(index) }) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                    }
                }
                
                // Category row for ingredient
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp)) {
                    Text(text = stringResource(R.string.category) + ":", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.width(8.dp))
                    var showCustomDialog by remember { mutableStateOf(false) }
                    
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy((-12).dp)
                    ) {
                        val standardCategories = listOf("Produce", "Dairy", "Meat", "Pantry", "Frozen", "Bakery", "Other")
                        val displayedCategories = (categories + ingredient.category.trim()).distinct()
                        displayedCategories.forEach { cat ->
                            val label = if (cat in standardCategories) {
                                val catRes = when (cat) {
                                    "Produce" -> R.string.category_produce
                                    "Dairy" -> R.string.category_dairy
                                    "Meat" -> R.string.category_meat
                                    "Pantry" -> R.string.category_pantry
                                    "Frozen" -> R.string.category_frozen
                                    "Bakery" -> R.string.category_bakery
                                    else -> R.string.category_other
                                }
                                stringResource(catRes)
                            } else {
                                cat
                            }
                            FilterChip(
                                selected = ingredient.category.trim() == cat.trim(),
                                onClick = { 
                                    ingredients[index] = ingredient.copy(category = cat)
                                    viewModel.updateIngredientCategory(ingredient.name, cat)
                                },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                        
                        FilterChip(
                            selected = false,
                            onClick = { showCustomDialog = true },
                            label = { Text(stringResource(R.string.category_custom), style = MaterialTheme.typography.labelSmall) }
                        )
                    }

                    if (showCustomDialog) {
                        var customCat by remember { mutableStateOf("") }
                        AlertDialog(
                            onDismissRequest = { showCustomDialog = false },
                            title = { Text(stringResource(R.string.new_category)) },
                            text = {
                                OutlinedTextField(
                                    value = customCat,
                                    onValueChange = { customCat = it },
                                    label = { Text(stringResource(R.string.category_name)) },
                                    singleLine = true
                                )
                            },
                            confirmButton = {
                                Button(onClick = {
                                    if (customCat.isNotBlank()) {
                                        ingredients[index] = ingredient.copy(category = customCat)
                                        viewModel.updateIngredientCategory(ingredient.name, customCat)
                                        showCustomDialog = false
                                    }
                                }) {
                                    Text(stringResource(R.string.add))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showCustomDialog = false }) {
                                    Text(stringResource(R.string.cancel))
                                }
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(R.string.steps_label), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { steps.add("") }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }
        }

        itemsIndexed(steps) { index, step ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = step,
                    onValueChange = { steps[index] = it },
                    label = { Text(stringResource(R.string.step_n, index + 1)) },
                    modifier = Modifier.weight(1f),
                    minLines = 2
                )
                IconButton(onClick = { steps.removeAt(index) }) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    viewModel.saveRecipe(
                        id = recipeId,
                        name = name,
                        description = description,
                        servings = servings.toInt(),
                        photoUri = photoUri,
                        ingredients = ingredients,
                        steps = steps,
                        tags = tags,
                        onComplete = onSave
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = canSave
            ) {
                Text(if (recipeId == 0L) stringResource(R.string.save_recipe) else stringResource(R.string.update_recipe))
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun StatsScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = stringResource(R.string.soon))
    }
}
