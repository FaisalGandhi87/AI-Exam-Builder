package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDao {
    @Query("SELECT * FROM exams ORDER BY timestamp DESC")
    fun getAllExams(): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams WHERE id = :id")
    suspend fun getExamById(id: Int): ExamEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamEntity): Long

    @Delete
    suspend fun deleteExam(exam: ExamEntity)

    @Query("DELETE FROM exams WHERE id = :id")
    suspend fun deleteExamById(id: Int)
}
