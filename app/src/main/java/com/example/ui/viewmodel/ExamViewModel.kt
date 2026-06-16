package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Base64
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.ExamEntity
import com.example.data.model.ExamDetails
import com.example.data.repository.ExamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream

class ExamViewModel(
    application: Application,
    private val repository: ExamRepository
) : AndroidViewModel(application) {

    // List of previously saved exams
    val savedExams: StateFlow<List<ExamEntity>> = repository.allExams
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current composing Exam details
    private val _activeDetails = MutableStateFlow(ExamDetails())
    val activeDetails: StateFlow<ExamDetails> = _activeDetails.asStateFlow()

    private val _activeExamTitle = MutableStateFlow("New AI Unified Exam")
    val activeExamTitle: StateFlow<String> = _activeExamTitle.asStateFlow()

    // Uploaded material details
    private val _selectedFileName = MutableStateFlow<String?>(null)
    val selectedFileName: StateFlow<String?> = _selectedFileName.asStateFlow()

    private val _selectedFileBase64 = MutableStateFlow<String?>(null)
    val selectedFileBase64: StateFlow<String?> = _selectedFileBase64.asStateFlow()

    private val _selectedFileText = MutableStateFlow<String?>(null)
    val selectedFileText: StateFlow<String?> = _selectedFileText.asStateFlow()

    // Loading & state management
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _generatingCategory = MutableStateFlow<String?>(null)
    val generatingCategory: StateFlow<String?> = _generatingCategory.asStateFlow()

    private val _errorMsg = MutableStateFlow<String?>(null)
    val errorMsg: StateFlow<String?> = _errorMsg.asStateFlow()

    fun updateActiveExamTitle(title: String) {
        _activeExamTitle.value = title
    }

    fun clearActiveWorkspace() {
        _activeDetails.value = ExamDetails()
        _activeExamTitle.value = "New AI Unified Exam"
        _selectedFileName.value = null
        _selectedFileBase64.value = null
        _selectedFileText.value = null
        _errorMsg.value = null
    }

    fun loadSavedExamIntoWorkspace(exam: ExamEntity) {
        _activeExamTitle.value = exam.title
        _activeDetails.value = exam.details
        _selectedFileName.value = exam.sourceFileName
    }

    // Handles picking and reading text / PDF files
    fun handleFilePicked(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                _errorMsg.value = null
                val cr = context.contentResolver
                var name: String? = null
                
                // Get filename
                cr.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex >= 0) {
                        name = cursor.getString(nameIndex)
                    }
                }
                
                if (name == null) {
                    name = uri.lastPathSegment ?: "uploaded_document"
                }
                _selectedFileName.value = name

                // Read bytes
                val inputStream: InputStream? = cr.openInputStream(uri)
                val bytes = inputStream?.readBytes() ?: throw Exception("Unable to read file content.")
                inputStream.close()

                if (name!!.endsWith(".pdf", ignoreCase = true)) {
                    val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    _selectedFileBase64.value = encoded
                    _selectedFileText.value = null
                } else {
                    // Treat as text material
                    val text = String(bytes, Charsets.UTF_8)
                    _selectedFileText.value = text
                    _selectedFileBase64.value = null
                }
            } catch (e: Exception) {
                _errorMsg.value = "Failed to open document: ${e.localizedMessage}"
            }
        }
    }

    fun handleTextMaterialPasted(title: String, body: String) {
        _selectedFileName.value = title.ifBlank { "Pasted Text Material" }
        _selectedFileText.value = body
        _selectedFileBase64.value = null
    }

    /**
     * Incrementally builds exam questions calling Gemini 3.5 Flash
     */
    fun triggerQuestionGeneration(
        type: String, // "MCQ", "FillInBlank", "TrueFalse", "Short", "CaseStudy", "Numerical"
        count: Int
    ) {
        viewModelScope.launch {
            _isGenerating.value = true
            _generatingCategory.value = type
            _errorMsg.value = null

            try {
                val updatedDetails = repository.generateQuestions(
                    pdfBase64 = _selectedFileBase64.value,
                    rawText = _selectedFileText.value,
                    questionType = type,
                    count = count,
                    currentDetails = _activeDetails.value
                )
                _activeDetails.value = updatedDetails
            } catch (e: Exception) {
                _errorMsg.value = e.localizedMessage ?: "Network error during AI question compilation."
            } finally {
                _isGenerating.value = false
                _generatingCategory.value = null
            }
        }
    }

    fun deleteSavedExam(examId: Int) {
        viewModelScope.launch {
            repository.deleteExamById(examId)
        }
    }

    fun saveActiveExamToHistory() {
        viewModelScope.launch {
            try {
                val entity = ExamEntity(
                    title = _activeExamTitle.value.ifBlank { "AI Unified Exam" },
                    sourceFileName = _selectedFileName.value ?: "Virtual Syllabus Material",
                    details = _activeDetails.value
                )
                repository.insertExam(entity)
            } catch (e: Exception) {
                _errorMsg.value = "Failed to save exam to history: ${e.localizedMessage}"
            }
        }
    }

    private val _isTranslating = MutableStateFlow(false)
    val isTranslating: StateFlow<Boolean> = _isTranslating.asStateFlow()

    fun translateActiveExam(targetLanguage: String) {
        viewModelScope.launch {
            _isTranslating.value = true
            _errorMsg.value = null
            try {
                val translated = repository.translateExam(_activeDetails.value, targetLanguage)
                _activeDetails.value = translated
                
                // Add suffix indicating the translation language
                val suffix = " ($targetLanguage)"
                val currentTitle = _activeExamTitle.value
                val languages = listOf("English", "Urdu", "Arabic")
                var cleanedTitle = currentTitle
                
                // Remove existing language suffixes if any
                for (lang in languages) {
                    val langSuffix = " ($lang)"
                    if (cleanedTitle.endsWith(langSuffix)) {
                        cleanedTitle = cleanedTitle.substringBeforeLast(langSuffix)
                    }
                }
                _activeExamTitle.value = "$cleanedTitle$suffix"
            } catch (e: Exception) {
                _errorMsg.value = e.localizedMessage ?: "Error during AI Translation: Check your internet connection."
            } finally {
                _isTranslating.value = false
            }
        }
    }

    fun clearError() {
        _errorMsg.value = null
    }
}

class ExamViewModelFactory(
    private val application: Application,
    private val repository: ExamRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExamViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExamViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
