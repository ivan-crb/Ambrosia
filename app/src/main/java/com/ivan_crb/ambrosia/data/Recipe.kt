package com.ivan_crb.ambrosia.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
data class Ingredient(
    val name: String,
    val amount: String,
    val unit: String,
    val category: String = "Other"
)

@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val servings: Int,
    val photoUri: String?, // Internal storage path
    val ingredients: List<Ingredient>,
    val steps: List<String>,
    val tags: List<String> = emptyList()
)

@Entity(tableName = "shopping_list")
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val amount: String,
    val unit: String,
    val isChecked: Boolean = false,
    val quantityAtHome: Double = 0.0,
    val category: String = "Other",
    val weekStart: String? = null, // ISO date of the Monday of that week
    val mealPlanId: Long? = null   // Link to a specific meal plan slot
)

@Entity(tableName = "meal_plans")
data class MealPlan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // ISO format yyyy-MM-dd
    val slot: String, // "Breakfast", "Lunch", "Dinner"
    val recipeId: Long,
    val plannedServings: Int = 0
)

@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey val id: Int = 1,
    val defaultServings: Int = 1,
    val startOfWeek: String = "Monday",
    val visibleMealSlots: String = "Breakfast,Lunch,Dinner",
    val shoppingGrouping: String = "Alphabetical",
    val themeMode: String = "System",
    val language: String = "System"
)

@Entity(tableName = "global_categories")
data class GlobalCategory(
    @PrimaryKey val name: String,
    val category: String
)

@Entity(tableName = "meal_plan_templates")
data class MealPlanTemplate(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String
)

@Entity(tableName = "template_meals")
data class TemplateMeal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val templateId: Long,
    val dayOfWeek: Int, // 1 (Mon) to 7 (Sun)
    val slot: String,
    val recipeId: Long,
    val plannedServings: Int
)
