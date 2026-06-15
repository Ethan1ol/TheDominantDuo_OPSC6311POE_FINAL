package com.example.opsc_ec.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // User
    @Insert
    suspend fun insertUser(user: User): Long

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    // Category
    @Insert
    suspend fun insertCategory(category: Category)

    @Query("SELECT * FROM categories WHERE userId = :userId")
    fun getCategoriesForUser(userId: Int): Flow<List<Category>>

    // Expense
    @Insert
    suspend fun insertExpense(expense: Expense)

    @Query("SELECT * FROM expenses WHERE userId = :userId AND startDate BETWEEN :rangeStart AND :rangeEnd")
    fun getExpensesForPeriod(userId: Int, rangeStart: Long, rangeEnd: Long): Flow<List<Expense>>

    @Query("SELECT categoryId, SUM(actualSpent) as totalAmount FROM expenses WHERE userId = :userId AND startDate BETWEEN :rangeStart AND :rangeEnd GROUP BY categoryId")
    fun getCategoryTotals(userId: Int, rangeStart: Long, rangeEnd: Long): Flow<List<CategoryTotal>>

    @Query("SELECT * FROM expenses WHERE categoryId = :categoryId ORDER BY priority DESC")
    fun getExpensesForCategory(categoryId: Int): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE categoryId = :categoryId AND startDate BETWEEN :rangeStart AND :rangeEnd ORDER BY priority DESC")
    fun getExpensesForCategoryAndPeriod(
        categoryId: Int,
        rangeStart: Long,
        rangeEnd: Long
    ): Flow<List<Expense>>

    @Query("UPDATE expenses SET actualSpent = :amount WHERE id = :expenseId")
    suspend fun updateActualSpent(expenseId: Int, amount: Double)

    // Budget Goal
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateGoal(goal: BudgetGoal)

    @Query("SELECT * FROM budget_goals WHERE userId = :userId AND monthYear = :monthYear LIMIT 1")
    fun getGoalForMonth(userId: Int, monthYear: String): Flow<BudgetGoal?>

    @Query(
        """SELECT
            categories.id, categories.name, categories.userId, categories.priority, categories.isGoal, categories.targetAmount, SUM(expenses.actualSpent) AS totalAmount,SUM(expenses.minGoal) AS minGoal,
SUM(expenses.maxGoal) AS maxGoal
FROM categories
LEFT JOIN expenses
ON categories.id = expenses.categoryId
WHERE categories.userId = :userId
GROUP BY categories.id
ORDER BY categories.priority DESC
"""
    )
    fun getCategoriesWithTotals(
        userId: Int
    ): Flow<List<CategoryWithTotal>>


    @Query(
        """SELECT
            categories.id,categories.name, categories.userId, categories.priority, categories.isGoal, categories.targetAmount, SUM(expenses.actualSpent) AS totalAmount, SUM(expenses.minGoal) AS minGoal, SUM(expenses.maxGoal) AS maxGoal
FROM categories
LEFT JOIN expenses
ON categories.id = expenses.categoryId
AND expenses.startDate BETWEEN :rangeStart AND :rangeEnd
WHERE categories.userId = :userId
GROUP BY categories.id
ORDER BY categories.priority DESC"""
    )
    fun getCategoriesWithTotalsForPeriod(
        userId: Int,
        rangeStart: Long,
        rangeEnd: Long
    ): Flow<List<CategoryWithTotal>>

    @Query(
        """SELECT
            categories.name AS categoryName,SUM(expenses.actualSpent) AS totalSpent,
MIN(expenses.minGoal) AS minGoal,MAX(expenses.maxGoal) AS maxGoal
FROM categories
LEFT JOIN expenses
ON categories.id = expenses.categoryId
WHERE categories.userId = :userId
AND expenses.startDate BETWEEN :startDate AND :endDate
GROUP BY categories.id"""
    )
    fun getCategoryGraph(
        userId: Int,
        startDate: Long,
        endDate: Long
    ): Flow<List<CategoryGraphs>>


    @Query(
        """SELECT categories.*
            FROM categories
INNER JOIN expenses
ON categories.id = expenses.categoryId
WHERE categories.userId = :userId
AND expenses.startDate BETWEEN :startDate AND :endDate
GROUP BY categories.id"""
    )
    fun getCategoriesByDate(
        userId: Int,
        startDate: Long,
        endDate: Long
    ): Flow<List<Category>>
}
data class CategoryWithTotal(
    val id: Int,
    val name: String,
    val userId: Int,
    val priority: Int,
    val isGoal: Boolean,
    val targetAmount: Double,
    val totalAmount: Double?,
    val minGoal: Double?,
    val maxGoal: Double?
)

data class CategoryTotal(
    val categoryId: Int,
    val totalAmount: Double
)
