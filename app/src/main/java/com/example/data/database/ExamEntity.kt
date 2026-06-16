package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.ExamDetails

@Entity(tableName = "exams")
data class ExamEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val sourceFileName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val details: ExamDetails
)
