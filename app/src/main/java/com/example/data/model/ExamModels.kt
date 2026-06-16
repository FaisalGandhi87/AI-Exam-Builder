package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MCQQuestion(
    val id: Int,
    val question: String,
    val options: List<String>,
    val correctAnswer: String,
    val explanation: String
)

@JsonClass(generateAdapter = true)
data class FillInBlankQuestion(
    val id: Int,
    val question: String,
    val correctAnswer: String,
    val explanation: String
)

@JsonClass(generateAdapter = true)
data class TrueFalseQuestion(
    val id: Int,
    val question: String,
    val correctAnswer: Boolean,
    val explanation: String
)

@JsonClass(generateAdapter = true)
data class ShortQuestion(
    val id: Int,
    val question: String,
    val suggestedAnswer: String,
    val points: Int
)

@JsonClass(generateAdapter = true)
data class CaseStudyQuestion(
    val id: Int,
    val scenario: String,
    val questions: List<String>,
    val answers: List<String>,
    val markingGuidelines: String
)

@JsonClass(generateAdapter = true)
data class NumericalQuestion(
    val id: Int,
    val question: String,
    val correctAnswer: String,
    val stepsToSolve: String,
    val points: Int
)

@JsonClass(generateAdapter = true)
data class MarkingSchemeInfo(
    val type: String, // e.g., "MCQ", "FillInBlank", "TrueFalse", "ShortQuestion", "CaseStudy", "Numerical"
    val pointsPerQuestion: Int,
    val criteria: String
)

@JsonClass(generateAdapter = true)
data class ExamDetails(
    val mcqs: List<MCQQuestion> = emptyList(),
    val fillInBlanks: List<FillInBlankQuestion> = emptyList(),
    val trueFalse: List<TrueFalseQuestion> = emptyList(),
    val shortQuestions: List<ShortQuestion> = emptyList(),
    val caseDocs: List<CaseStudyQuestion> = emptyList(),
    val numericals: List<NumericalQuestion> = emptyList(),
    val markingSchemes: List<MarkingSchemeInfo> = emptyList()
)
