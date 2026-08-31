package com.ivan_crb.ambrosia.viewmodel

import android.app.Application
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ivan_crb.ambrosia.data.AppDatabase
import com.ivan_crb.ambrosia.data.Ingredient
import com.ivan_crb.ambrosia.data.Recipe
import com.ivan_crb.ambrosia.data.ShoppingItem
import com.ivan_crb.ambrosia.data.MealPlan
import com.ivan_crb.ambrosia.data.UserSettings
import com.ivan_crb.ambrosia.data.GlobalCategory
import com.ivan_crb.ambrosia.data.MealPlanTemplate
import com.ivan_crb.ambrosia.data.TemplateMeal
import com.ivan_crb.ambrosia.utils.CategoryUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.UUID
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi

data class AggregatedShoppingItem(
    val name: String,
    val totalRequired: Double,
    val totalAtHome: Double,
    val totalToBuy: Double,
    val unit: String,
    val isChecked: Boolean,
    val category: String,
    val originalIds: List<Long>
)

class RecipeViewModel(application: Application) : AndroidViewModel(application) {
    private val recipeDao = AppDatabase.getDatabase(application).recipeDao()

    private val _currentWeekStart = MutableStateFlow(
        LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    )
    val currentWeekStart: StateFlow<LocalDate> = _currentWeekStart

    val userSettings: StateFlow<UserSettings> = recipeDao.getUserSettings()
        .map { it ?: UserSettings() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettings()
        )

    val allRecipes: StateFlow<List<Recipe>> = recipeDao.getAllRecipes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedTags = MutableStateFlow<Set<String>>(emptySet())
    val selectedTags: StateFlow<Set<String>> = _selectedTags

    val filteredRecipes: StateFlow<List<Recipe>> = combine(allRecipes, _searchQuery, _selectedTags) { recipes, query, tags ->
        recipes.filter { recipe ->
            val matchesQuery = recipe.name.contains(query, ignoreCase = true) || 
                             recipe.description.contains(query, ignoreCase = true)
            val matchesTags = tags.isEmpty() || recipe.tags.containsAll(tags)
            matchesQuery && matchesTags
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allRecipeTags: StateFlow<List<String>> = allRecipes.map { recipes ->
        recipes.flatMap { it.tags }.distinct().sorted()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allTemplates: StateFlow<List<MealPlanTemplate>> = recipeDao.getAllTemplates()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val globalCategories: StateFlow<List<GlobalCategory>> = recipeDao.getAllGlobalCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allCategories: StateFlow<List<String>> = globalCategories.map { globals ->
        val standard = listOf("Produce", "Dairy", "Meat", "Pantry", "Frozen", "Bakery", "Other")
        val custom = globals.map { it.category }.distinct().filter { it !in standard }
        standard.dropLast(1) + custom + "Other"
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = listOf("Produce", "Dairy", "Meat", "Pantry", "Frozen", "Bakery", "Other")
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val weeklyMealPlans: StateFlow<List<MealPlan>> = _currentWeekStart
        .flatMapLatest { start ->
            recipeDao.getMealPlansForRange(start.toString(), start.plusDays(6).toString())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val weeklyShoppingItems: StateFlow<List<AggregatedShoppingItem>> = combine(
        _currentWeekStart,
        userSettings
    ) { start, settings ->
        start to settings
    }
    .flatMapLatest { (start, settings) ->
        recipeDao.getShoppingItemsForWeek(start.toString())
            .map { items ->
                items.groupBy { "${it.name.lowercase().trim()}_${it.unit.lowercase().trim()}" }
                    .map { (_, group) ->
                        val first = group.first()
                        val required = group.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
                        val atHome = group.sumOf { it.quantityAtHome }
                        val toBuy = (required - atHome).coerceAtLeast(0.0)
                        
                        AggregatedShoppingItem(
                            name = first.name,
                            totalRequired = required,
                            totalAtHome = atHome,
                            totalToBuy = toBuy,
                            unit = first.unit,
                            isChecked = group.all { it.isChecked } || toBuy <= 0,
                            category = first.category,
                            originalIds = group.map { it.id }
                        )
                    }
                    .let { list ->
                        if (settings.shoppingGrouping == "Category") {
                            list.sortedWith(compareBy({ it.category }, { it.name }))
                        } else {
                            list.sortedBy { it.name }
                        }
                    }
            }
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun nextWeek() {
        _currentWeekStart.value = _currentWeekStart.value.plusWeeks(1)
    }

    fun previousWeek() {
        _currentWeekStart.value = _currentWeekStart.value.minusWeeks(1)
    }

    fun updateDefaultServings(servings: Int) {
        viewModelScope.launch {
            val current = userSettings.value
            recipeDao.updateUserSettings(current.copy(defaultServings = servings))
        }
    }

    fun updateStartOfWeek(day: String) {
        viewModelScope.launch {
            val current = userSettings.value
            recipeDao.updateUserSettings(current.copy(startOfWeek = day))
            val dayOfWeek = if (day == "Sunday") DayOfWeek.SUNDAY else DayOfWeek.MONDAY
            _currentWeekStart.value = LocalDate.now().with(TemporalAdjusters.previousOrSame(dayOfWeek))
        }
    }

    fun updateVisibleMealSlots(slots: String) {
        viewModelScope.launch {
            val current = userSettings.value
            recipeDao.updateUserSettings(current.copy(visibleMealSlots = slots))
        }
    }

    fun updateShoppingGrouping(grouping: String) {
        viewModelScope.launch {
            val current = userSettings.value
            recipeDao.updateUserSettings(current.copy(shoppingGrouping = grouping))
        }
    }

    fun updateThemeMode(mode: String) {
        viewModelScope.launch {
            val current = userSettings.value
            recipeDao.updateUserSettings(current.copy(themeMode = mode))
        }
    }

    fun updateLanguage(language: String) {
        viewModelScope.launch {
            val current = userSettings.value
            recipeDao.updateUserSettings(current.copy(language = language))
            
            val appLocale: LocaleListCompat = when (language) {
                "English" -> LocaleListCompat.forLanguageTags("en")
                "Spanish" -> LocaleListCompat.forLanguageTags("es")
                else -> LocaleListCompat.getEmptyLocaleList()
            }
            AppCompatDelegate.setApplicationLocales(appLocale)
        }
    }

    fun addRecipeToPlan(date: String, slot: String, recipe: Recipe, plannedServings: Int) {
        viewModelScope.launch {
            // Remove existing plan in this slot if any
            val existingPlans = recipeDao.getMealPlansForDate(date).first()
            existingPlans.find { it.slot == slot }?.let { existing ->
                recipeDao.deleteShoppingItemsByMealPlan(existing.id)
                recipeDao.removeMealFromSlot(date, slot)
            }

            recipeDao.insertMealPlan(
                MealPlan(
                    date = date,
                    slot = slot,
                    recipeId = recipe.id,
                    plannedServings = plannedServings
                )
            )
            
            // Get the plan again to find the generated ID
            val allPlans = recipeDao.getMealPlansForDate(date).first()
            val newPlan = allPlans.find { it.slot == slot } ?: return@launch

            // Automatically add ingredients to shopping list for that week
            val weekStart = LocalDate.parse(date).with(TemporalAdjusters.previousOrSame(
                if (userSettings.value.startOfWeek == "Sunday") DayOfWeek.SUNDAY else DayOfWeek.MONDAY
            )).toString()
            
            recipe.ingredients.forEach { ingredient ->
                val scaledAmount = scaleAmount(ingredient.amount, plannedServings, recipe.servings)

                recipeDao.insertShoppingItem(
                    ShoppingItem(
                        name = ingredient.name,
                        amount = scaledAmount,
                        unit = ingredient.unit,
                        category = ingredient.category,
                        weekStart = weekStart,
                        mealPlanId = newPlan.id
                    )
                )
            }
        }
    }

    fun updateMealPlanServings(plan: MealPlan, newServings: Int) {
        viewModelScope.launch {
            recipeDao.updateMealPlanServings(plan.id, newServings)
            val recipe = recipeDao.getAllRecipes().first().find { it.id == plan.recipeId } ?: return@launch
            
            recipeDao.deleteShoppingItemsByMealPlan(plan.id)
            
            val weekStart = LocalDate.parse(plan.date).with(TemporalAdjusters.previousOrSame(
                if (userSettings.value.startOfWeek == "Sunday") DayOfWeek.SUNDAY else DayOfWeek.MONDAY
            )).toString()
            recipe.ingredients.forEach { ingredient ->
                val scaledAmount = scaleAmount(ingredient.amount, newServings, recipe.servings)
                recipeDao.insertShoppingItem(
                    ShoppingItem(
                        name = ingredient.name,
                        amount = scaledAmount,
                        unit = ingredient.unit,
                        category = ingredient.category,
                        weekStart = weekStart,
                        mealPlanId = plan.id
                    )
                )
            }
        }
    }

    private fun scaleAmount(amount: String, planned: Int, base: Int): String {
        return try {
            val originalAmount = amount.toDoubleOrNull()
            if (originalAmount != null && base > 0) {
                val scaled = (originalAmount * planned) / base
                if (scaled == scaled.toInt().toDouble()) {
                    scaled.toInt().toString()
                } else {
                    "%.2f".format(Locale.ENGLISH, scaled)
                }
            } else {
                amount
            }
        } catch (e: Exception) {
            amount
        }
    }

    fun removeRecipeFromPlan(date: String, slot: String) {
        viewModelScope.launch {
            val plans = recipeDao.getMealPlansForDate(date).first()
            plans.find { it.slot == slot }?.let { plan ->
                recipeDao.deleteShoppingItemsByMealPlan(plan.id)
            }
            recipeDao.removeMealFromSlot(date, slot)
        }
    }

    fun addCustomShoppingItem(name: String, amount: String, unit: String, category: String? = null) {
        viewModelScope.launch {
            val finalCategory = category ?: suggestCategory(name)
            recipeDao.insertShoppingItem(
                ShoppingItem(
                    name = name,
                    amount = amount,
                    unit = unit,
                    category = finalCategory,
                    weekStart = _currentWeekStart.value?.toString()
                )
            )
            
            // Learn the category if it was manually provided
            if (category != null) {
                updateIngredientCategory(name, category)
            }
        }
    }

    fun toggleShoppingItem(item: AggregatedShoppingItem) {
        viewModelScope.launch {
            recipeDao.updateShoppingItemsStatus(item.originalIds, !item.isChecked)
        }
    }

    fun updateIngredientCategory(name: String, newCategory: String) {
        viewModelScope.launch {
            val normalizedName = name.lowercase().trim()
            // Update Global Learning
            recipeDao.upsertGlobalCategory(GlobalCategory(normalizedName, newCategory))
            
            // Update current shopping items with this name
            recipeDao.updateShoppingItemsCategoryByName(name, newCategory)

            // Update all recipes that contain this ingredient
            val recipes = recipeDao.getAllRecipes().first()
            recipes.forEach { recipe ->
                var updated = false
                val newIngredients = recipe.ingredients.map { ingredient ->
                    if (ingredient.name.lowercase().trim() == normalizedName) {
                        updated = true
                        ingredient.copy(category = newCategory)
                    } else {
                        ingredient
                    }
                }
                if (updated) {
                    recipeDao.insertRecipe(recipe.copy(ingredients = newIngredients))
                }
            }
        }
    }

    suspend fun suggestCategory(name: String): String {
        val lowerName = name.lowercase().trim()
        val global = recipeDao.getGlobalCategory(lowerName)
        if (global != null) return global.category
        
        return CategoryUtils.suggestCategory(name)
    }

    fun setAtHomeQuantity(item: AggregatedShoppingItem, quantity: Double) {
        viewModelScope.launch {
            val distributedQuantity = quantity / item.originalIds.size
            recipeDao.updateShoppingItemsAtHomeQuantity(item.originalIds, distributedQuantity)
        }
    }

    fun deleteShoppingItem(item: AggregatedShoppingItem) {
        viewModelScope.launch {
            recipeDao.deleteShoppingItems(item.originalIds)
        }
    }

    fun clearCheckedShoppingItems() {
        viewModelScope.launch {
            recipeDao.clearCheckedItems()
        }
    }

    // Search & Filter
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleTagFilter(tag: String) {
        val current = _selectedTags.value
        if (current.contains(tag)) {
            _selectedTags.value = current - tag
        } else {
            _selectedTags.value = current + tag
        }
    }

    // Templates
    fun saveCurrentWeekAsTemplate(name: String) {
        viewModelScope.launch {
            val weekStart = _currentWeekStart.value ?: return@launch
            val plans = weeklyMealPlans.value
            if (plans.isEmpty()) return@launch

            val templateId = recipeDao.insertTemplate(MealPlanTemplate(name = name))
            val templateMeals = plans.map { plan ->
                val date = LocalDate.parse(plan.date)
                val dayOfWeek = date.dayOfWeek.value // 1-7
                TemplateMeal(
                    templateId = templateId,
                    dayOfWeek = dayOfWeek,
                    slot = plan.slot,
                    recipeId = plan.recipeId,
                    plannedServings = plan.plannedServings
                )
            }
            recipeDao.insertTemplateMeals(templateMeals)
        }
    }

    fun applyTemplateToCurrentWeek(templateId: Long) {
        viewModelScope.launch {
            val templateMeals = recipeDao.getTemplateMeals(templateId)
            val weekStart = _currentWeekStart.value ?: return@launch
            val settings = userSettings.value
            val visibleSlots = settings.visibleMealSlots.split(",").toSet()

            // Calculate start date of current week (handling Sunday/Monday start)
            val currentWeekMonday = weekStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

            // Delete current week's plans first
            val currentWeekEnd = weekStart.plusDays(6)
            val existingPlans = recipeDao.getMealPlansForRange(weekStart.toString(), currentWeekEnd.toString()).first()
            existingPlans.forEach { plan ->
                recipeDao.deleteShoppingItemsByMealPlan(plan.id)
                recipeDao.removeMealFromSlot(plan.date, plan.slot)
            }

            // Apply template meals that match visible slots
            templateMeals.forEach { tm ->
                if (visibleSlots.contains(tm.slot)) {
                    val date = currentWeekMonday.plusDays((tm.dayOfWeek - 1).toLong())
                    
                    // Re-calculate based on system start of week if necessary?
                    // Actually dayOfWeek 1-7 is always Mon-Sun in ISO.
                    // If user's week starts on Sunday, it's fine, we just map 1->Mon, 7->Sun.
                    
                    val recipes = allRecipes.value
                    val recipe = recipes.find { it.id == tm.recipeId }
                    if (recipe != null) {
                        addRecipeToPlan(date.toString(), tm.slot, recipe, tm.plannedServings)
                    }
                }
            }
        }
    }

    fun deleteTemplate(templateId: Long) {
        viewModelScope.launch {
            recipeDao.deleteTemplateMeals(templateId)
            recipeDao.deleteTemplate(templateId)
        }
    }

    fun getRecipe(id: Long): Flow<Recipe?> = recipeDao.getRecipeById(id)

    fun getMealPlansForDate(date: String): Flow<List<MealPlan>> = recipeDao.getMealPlansForDate(date)

    fun saveRecipe(
        id: Long = 0,
        name: String,
        description: String,
        servings: Int,
        photoUri: Uri?,
        ingredients: List<Ingredient>,
        steps: List<String>,
        tags: List<String> = emptyList(),
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val internalPhotoPath = photoUri?.let { uri ->
                if (uri.toString().startsWith("file://") && uri.path?.contains(getApplication<Application>().filesDir.path) == true) {
                    uri.path
                } else {
                    saveImageToInternalStorage(uri)
                }
            }
            val recipe = Recipe(
                id = id,
                name = name,
                description = description,
                servings = servings,
                photoUri = internalPhotoPath,
                ingredients = ingredients,
                steps = steps,
                tags = tags
            )
            recipeDao.insertRecipe(recipe)
            onComplete()
        }
    }

    private fun saveImageToInternalStorage(uri: Uri): String? {
        return try {
            val context = getApplication<Application>()
            val inputStream = context.contentResolver.openInputStream(uri)
            val fileName = "recipe_${UUID.randomUUID()}.jpg"
            val file = File(context.filesDir, fileName)
            val outputStream = FileOutputStream(file)
            
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
