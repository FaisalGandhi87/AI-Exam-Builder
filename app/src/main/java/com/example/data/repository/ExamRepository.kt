package com.example.data.repository

import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.database.ExamDao
import com.example.data.database.ExamEntity
import com.example.data.model.*
import com.example.data.network.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ExamRepository(private val examDao: ExamDao) {

    val allExams: Flow<List<ExamEntity>> = examDao.getAllExams()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    suspend fun getExamById(id: Int): ExamEntity? {
        return examDao.getExamById(id)
    }

    suspend fun insertExam(exam: ExamEntity): Long {
        return examDao.insertExam(exam)
    }

    suspend fun deleteExamById(id: Int) {
        examDao.deleteExamById(id)
    }

    /**
     * Generates custom questions from a PDF (base64) using Gemini 3.5 Flash.
     */
    suspend fun generateQuestions(
        pdfBase64: String?,
        rawText: String?,
        questionType: String, // "MCQ", "FillInBlank", "TrueFalse", "Short", "CaseStudy", "Numerical"
        count: Int,
        currentDetails: ExamDetails
    ): ExamDetails = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw Exception("Gemini API key is not configured in Secrets. Please add GEMINI_API_KEY in the Secrets panel.")
        }

        val prompt = buildPromptForType(questionType, count)
        val parts = mutableListOf<Part>()
        
        // Add PDF or text content
        if (!pdfBase64.isNullOrEmpty()) {
            parts.add(Part(inlineData = InlineData(mimeType = "application/pdf", data = pdfBase64)))
        } else if (!rawText.isNullOrEmpty()) {
            parts.add(Part(text = "Reference study material:\n$rawText\n\n"))
        } else {
            throw Exception("No study material (PDF or text) supplied.")
        }

        parts.add(Part(text = prompt))

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = parts)),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.4f
            ),
            systemInstruction = Content(
                parts = listOf(Part(text = "You are an expert academic assessment designer. You read textbook/source material and generate high-quality questions matching requested schemas exactly. Do not include any greeting or conversational filler in your response - reply with pure, parseable JSON only."))
            )
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val rawTextResponse = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("Empty response received from Gemini.")
            
            val cleanJson = sanitizeJson(rawTextResponse)
            Log.d("ExamRepository", "Raw Clean Json: $cleanJson")

            // Parse response based on type
            return@withContext mergeGeneratedContent(questionType, cleanJson, currentDetails, count)
        } catch (e: Exception) {
            Log.e("ExamRepository", "Error during code generation", e)
            throw e
        }
    }

    private fun buildPromptForType(type: String, count: Int): String {
        return when (type) {
            "MCQ" -> """
                Generate exactly $count Multiple Choice Questions (MCQs) based strictly on the content provided.
                Each MCQ must have a clear academic question, 4 options labeled exactly as 'A) ...', 'B) ...', 'C) ...', 'D) ...', a single correctAnswer ('A', 'B', 'C', or 'D'), and a concise theoretical explanation.
                
                Respond strictly with JSON matching this schema:
                {
                  "mcqs": [
                    {
                      "id": 1,
                      "question": "Question text...",
                      "options": ["A) Opt1", "B) Opt2", "C) Opt3", "D) Opt4"],
                      "correctAnswer": "A",
                      "explanation": "Explanation text..."
                    }
                  ]
                }
            """.trimIndent()

            "FillInBlank" -> """
                Generate exactly $count Fill-in-the-Blank questions based strictly on the provided content.
                Include a blank line (represented as '___') inside the question statement, and provide the precise correctAnswer and an explanation.
                
                Respond strictly with JSON matching this schema:
                {
                  "fillInBlanks": [
                    {
                      "id": 1,
                      "question": "Word ___ is very essential.",
                      "correctAnswer": "Word",
                      "explanation": "Why..."
                    }
                  ]
                }
            """.trimIndent()

            "TrueFalse" -> """
                Generate exactly $count True or False statements based on the provided material.
                Each statement must be a clear fact with a clear correctAnswer (true or false) and a concise explanation of the scientific/literary truth.
                
                Respond strictly with JSON matching this schema:
                {
                  "trueFalse": [
                    {
                      "id": 1,
                      "question": "The sky is blue.",
                      "correctAnswer": true,
                      "explanation": "Why..."
                    }
                  ]
                }
            """.trimIndent()

            "Short" -> """
                Generate exactly $count academically sound Short Answer Questions with estimated score points and realistic suggested model answers.
                
                Respond strictly with JSON matching this schema:
                {
                  "shortQuestions": [
                    {
                      "id": 1,
                      "question": "What is ...?",
                      "suggestedAnswer": "A complete suggested answer text...",
                      "points": 5
                    }
                  ]
                }
            """.trimIndent()

            "CaseStudy" -> """
                Generate exactly $count detailed Case Studies and analytical narratives based on the provided material.
                Each case study must include a comprehensive scenario narrative, followed by 2 to 3 guiding/analytical questions, recommended correct/suggested responses for each question, and a marking guidelines statement.
                
                Respond strictly with JSON matching this schema:
                {
                  "caseDocs": [
                    {
                      "id": 1,
                      "scenario": "Scenario context...",
                      "questions": ["Q1...", "Q2..."],
                      "answers": ["Answer 1...", "Answer 2..."],
                      "markingGuidelines": "Give points for identifying..."
                    }
                  ]
                }
            """.trimIndent()

            "Numerical" -> """
                Generate exactly $count Numerical or calculation problems based on key numbers, metrics, or physical equations in the provided text.
                Provide the question, final correctAnswer value, and step-by-step calculations with estimated points.
                
                Respond strictly with JSON matching this schema:
                {
                  "numericals": [
                    {
                      "id": 1,
                      "question": "Calculate ...",
                      "correctAnswer": "Answer...",
                      "stepsToSolve": "Formula, step 1, step 2...",
                      "points": 10
                    }
                  ]
                }
            """.trimIndent()

            else -> throw IllegalArgumentException("Unknown question type: $type")
        }
    }

    private fun mergeGeneratedContent(
        type: String,
        json: String,
        currentDetails: ExamDetails,
        count: Int
    ): ExamDetails {
        val adapter = moshi.adapter(ExamDetails::class.java).failOnUnknown()
        val parsed = try {
            adapter.fromJson(json)
        } catch (e: Exception) {
            // Re-try with forgiving adapter if structured mismatch inside marking
            moshi.adapter(ExamDetails::class.java).fromJson(json)
        } ?: throw Exception("Failed to parse JSON schema matching ExamDetails.")

        // Build default marking scheme info for this batch
        val pointsPerType = when (type) {
            "MCQ" -> 1
            "FillInBlank" -> 1
            "TrueFalse" -> 1
            "Short" -> 5
            "CaseStudy" -> 15
            "Numerical" -> 10
            else -> 1
        }
        val defaultScheme = MarkingSchemeInfo(
            type = type,
            pointsPerQuestion = pointsPerType,
            criteria = "1 point per correct answer"
        )

        return when (type) {
            "MCQ" -> {
                val updatedMcqs = currentDetails.mcqs.toMutableList()
                parsed.mcqs.forEachIndexed { index, m ->
                    updatedMcqs.add(m.copy(id = updatedMcqs.size + 1))
                }
                currentDetails.copy(
                    mcqs = updatedMcqs,
                    markingSchemes = updateOrAddMarkingScheme(currentDetails.markingSchemes, defaultScheme)
                )
            }
            "FillInBlank" -> {
                val updatedBlanks = currentDetails.fillInBlanks.toMutableList()
                parsed.fillInBlanks.forEachIndexed { index, f ->
                    updatedBlanks.add(f.copy(id = updatedBlanks.size + 1))
                }
                currentDetails.copy(
                    fillInBlanks = updatedBlanks,
                    markingSchemes = updateOrAddMarkingScheme(currentDetails.markingSchemes, defaultScheme)
                )
            }
            "TrueFalse" -> {
                val updatedTf = currentDetails.trueFalse.toMutableList()
                parsed.trueFalse.forEachIndexed { index, t ->
                    updatedTf.add(t.copy(id = updatedTf.size + 1))
                }
                currentDetails.copy(
                    trueFalse = updatedTf,
                    markingSchemes = updateOrAddMarkingScheme(currentDetails.markingSchemes, defaultScheme)
                )
            }
            "Short" -> {
                val updatedShort = currentDetails.shortQuestions.toMutableList()
                parsed.shortQuestions.forEachIndexed { index, s ->
                    updatedShort.add(s.copy(id = updatedShort.size + 1))
                }
                currentDetails.copy(
                    shortQuestions = updatedShort,
                    markingSchemes = updateOrAddMarkingScheme(currentDetails.markingSchemes, defaultScheme.copy(criteria = "Assess clarity, relevance, and precise terminology usage."))
                )
            }
            "CaseStudy" -> {
                val updatedCase = currentDetails.caseDocs.toMutableList()
                parsed.caseDocs.forEachIndexed { index, c ->
                    updatedCase.add(c.copy(id = updatedCase.size + 1))
                }
                currentDetails.copy(
                    caseDocs = updatedCase,
                    markingSchemes = updateOrAddMarkingScheme(currentDetails.markingSchemes, defaultScheme.copy(criteria = "Granular points: Part (a) 5 points, Part (b) 10 points. Evaluate contextual reasoning and evidence extraction."))
                )
            }
            "Numerical" -> {
                val updatedNumerical = currentDetails.numericals.toMutableList()
                parsed.numericals.forEach { n ->
                    updatedNumerical.add(n.copy(id = updatedNumerical.size + 1))
                }
                currentDetails.copy(
                    numericals = updatedNumerical,
                    markingSchemes = updateOrAddMarkingScheme(currentDetails.markingSchemes, defaultScheme.copy(criteria = "Correct method: 5 points. Correct computational arithmetic: 5 points."))
                )
            }
            else -> currentDetails
        }
    }

    private fun updateOrAddMarkingScheme(
        currentSchemes: List<MarkingSchemeInfo>,
        newScheme: MarkingSchemeInfo
    ): List<MarkingSchemeInfo> {
        val list = currentSchemes.toMutableList()
        val index = list.indexOfFirst { it.type == newScheme.type }
        if (index >= 0) {
            list[index] = newScheme
        } else {
            list.add(newScheme)
        }
        return list
    }

    private fun sanitizeJson(html: String): String {
        var raw = html.trim()
        if (raw.startsWith("```json")) {
            raw = raw.substringAfter("```json")
        } else if (raw.startsWith("```")) {
            raw = raw.substringAfter("```")
        }
        if (raw.endsWith("```")) {
            raw = raw.substringBeforeLast("```")
        }
        return raw.trim()
    }
}
