package com.example.data.database

import androidx.room.TypeConverter
import com.example.data.model.ExamDetails
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class ExamConverters {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    
    private val examDetailsAdapter = moshi.adapter(ExamDetails::class.java)

    @TypeConverter
    fun fromExamDetails(details: ExamDetails?): String? {
        return details?.let { examDetailsAdapter.toJson(it) }
    }

    @TypeConverter
    fun toExamDetails(json: String?): ExamDetails? {
        return json?.let { examDetailsAdapter.fromJson(it) }
    }
}
