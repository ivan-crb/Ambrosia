package com.ivan_crb.ambrosia.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes ORDER BY name ASC")
    fun getAllRecipes(): Flow<List<Recipe>>

    @Query("SELECT * FROM recipes WHERE id = :id")
    fun getRecipeById(id: Long): Flow<Recipe?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: Recipe)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun deleteRecipe(id: Long)

    // Shopping List
    @Query("SELECT * FROM shopping_list WHERE weekStart = :weekStart OR weekStart IS NULL ORDER BY isChecked ASC, name ASC")
    fun getShoppingItemsForWeek(weekStart: String): Flow<List<ShoppingItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingItem(item: ShoppingItem)

    @Query("DELETE FROM shopping_list WHERE id = :id")
    suspend fun deleteShoppingItem(id: Long)

    @Query("UPDATE shopping_list SET isChecked = :isChecked WHERE id IN (:ids)")
    suspend fun updateShoppingItemsStatus(ids: List<Long>, isChecked: Boolean)

    @Query("UPDATE shopping_list SET quantityAtHome = :quantity WHERE id IN (:ids)")
    suspend fun updateShoppingItemsAtHomeQuantity(ids: List<Long>, quantity: Double)

    @Query("DELETE FROM shopping_list WHERE id IN (:ids)")
    suspend fun deleteShoppingItems(ids: List<Long>)

    @Query("DELETE FROM shopping_list WHERE isChecked = 1")
    suspend fun clearCheckedItems()

    @Query("UPDATE shopping_list SET category = :category WHERE name = :name")
    suspend fun updateShoppingItemsCategoryByName(name: String, category: String)

    // Meal Plan
    @Query("SELECT * FROM meal_plans WHERE date = :date")
    fun getMealPlansForDate(date: String): Flow<List<MealPlan>>

    @Query("SELECT * FROM meal_plans WHERE date BETWEEN :startDate AND :endDate")
    fun getMealPlansForRange(startDate: String, endDate: String): Flow<List<MealPlan>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealPlan(mealPlan: MealPlan)

    @Query("DELETE FROM meal_plans WHERE date = :date AND slot = :slot")
    suspend fun removeMealFromSlot(date: String, slot: String)

    @Query("SELECT * FROM meal_plans WHERE id = :id")
    suspend fun getMealPlanById(id: Long): MealPlan?

    @Query("UPDATE meal_plans SET plannedServings = :servings WHERE id = :id")
    suspend fun updateMealPlanServings(id: Long, servings: Int)

    @Query("DELETE FROM shopping_list WHERE mealPlanId = :mealPlanId")
    suspend fun deleteShoppingItemsByMealPlan(mealPlanId: Long)

    // User Settings
    @Query("SELECT * FROM user_settings WHERE id = 1")
    fun getUserSettings(): Flow<UserSettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateUserSettings(settings: UserSettings)

    // Global Categories
    @Query("SELECT * FROM global_categories WHERE name = :name")
    suspend fun getGlobalCategory(name: String): GlobalCategory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGlobalCategory(globalCategory: GlobalCategory)

    @Query("SELECT * FROM global_categories")
    fun getAllGlobalCategories(): Flow<List<GlobalCategory>>

    // Templates
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: MealPlanTemplate): Long

    @Query("SELECT * FROM meal_plan_templates ORDER BY name ASC")
    fun getAllTemplates(): Flow<List<MealPlanTemplate>>

    @Query("SELECT * FROM meal_plan_templates WHERE id = :id")
    suspend fun getTemplateById(id: Long): MealPlanTemplate?

    @Query("DELETE FROM meal_plan_templates WHERE id = :id")
    suspend fun deleteTemplate(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplateMeals(meals: List<TemplateMeal>)

    @Query("SELECT * FROM template_meals WHERE templateId = :templateId")
    suspend fun getTemplateMeals(templateId: Long): List<TemplateMeal>

    @Query("DELETE FROM template_meals WHERE templateId = :templateId")
    suspend fun deleteTemplateMeals(templateId: Long)
}
