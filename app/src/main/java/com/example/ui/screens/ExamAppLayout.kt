package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.ExamEntity
import com.example.data.model.*
import com.example.ui.viewmodel.ExamViewModel
import com.example.utils.ExportHelper
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Screen {
    HISTORY,
    CREATOR,
    VIEWER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamAppLayout(
    viewModel: ExamViewModel,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf(Screen.HISTORY) }
    val context = LocalContext.current

    val savedExams by viewModel.savedExams.collectAsStateWithLifecycle()
    val activeDetails by viewModel.activeDetails.collectAsStateWithLifecycle()
    val activeTitle by viewModel.activeExamTitle.collectAsStateWithLifecycle()
    val selectedFileName by viewModel.selectedFileName.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val generatingCategory by viewModel.generatingCategory.collectAsStateWithLifecycle()
    val errorMsg by viewModel.errorMsg.collectAsStateWithLifecycle()
    val isTranslating by viewModel.isTranslating.collectAsStateWithLifecycle()

    // Dialog for pasting references
    var showPasteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "AI Exam Builder",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Instant Academic Assessment Suite",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    if (currentScreen != Screen.HISTORY) {
                        IconButton(onClick = {
                            if (currentScreen == Screen.VIEWER) {
                                currentScreen = Screen.CREATOR
                            } else {
                                currentScreen = Screen.HISTORY
                            }
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Go Back"
                            )
                        }
                    }
                },
                actions = {
                    if (currentScreen == Screen.HISTORY) {
                        IconButton(onClick = {
                            viewModel.clearActiveWorkspace()
                            currentScreen = Screen.CREATOR
                        }) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "New Exam")
                        }
                    } else if (currentScreen == Screen.CREATOR) {
                        IconButton(onClick = {
                            viewModel.clearActiveWorkspace()
                            Toast.makeText(context, "Workspace Reset", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset Workspace")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        bottomBar = {
            // Adaptive notch-safe padding under top/bottom scaffold containers
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.HISTORY -> {
                    HistoryScreen(
                        savedExams = savedExams,
                        onSelectExam = { exam ->
                            viewModel.loadSavedExamIntoWorkspace(exam)
                            currentScreen = Screen.VIEWER
                        },
                        onDeleteExam = { id ->
                            viewModel.deleteSavedExam(id)
                            Toast.makeText(context, "Exam Deleted", Toast.LENGTH_SHORT).show()
                        },
                        onComposeNew = {
                            viewModel.clearActiveWorkspace()
                            currentScreen = Screen.CREATOR
                        }
                    )
                }
                Screen.CREATOR -> {
                    CreatorScreen(
                        viewModel = viewModel,
                        activeTitle = activeTitle,
                        selectedFileName = selectedFileName,
                        activeDetails = activeDetails,
                        onOpenPasteDialog = { showPasteDialog = true },
                        onViewFullExam = {
                            if (activeDetails.mcqs.isEmpty() &&
                                activeDetails.fillInBlanks.isEmpty() &&
                                activeDetails.trueFalse.isEmpty() &&
                                activeDetails.shortQuestions.isEmpty() &&
                                activeDetails.caseDocs.isEmpty() &&
                                activeDetails.numericals.isEmpty()
                            ) {
                                Toast.makeText(context, "Please generate at least one type of question first!", Toast.LENGTH_LONG).show()
                            } else {
                                currentScreen = Screen.VIEWER
                            }
                        }
                    )
                }
                Screen.VIEWER -> {
                    ViewerScreen(
                        title = activeTitle,
                        details = activeDetails,
                        onSaveToHistory = {
                            viewModel.saveActiveExamToHistory()
                            Toast.makeText(context, "Exam successfully saved to history!", Toast.LENGTH_LONG).show()
                            currentScreen = Screen.HISTORY
                        },
                        onTranslate = { lang ->
                            viewModel.translateActiveExam(lang)
                        },
                        isTranslating = isTranslating
                    )
                }
            }

            // Global Loading Indicator Overlays
            if (isGenerating) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                strokeWidth = 5.dp,
                                modifier = Modifier
                                    .size(70.dp)
                                    .padding(8.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "AI Compiler Generating...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Creating unified ${generatingCategory ?: "questions"} matching the context guidelines. This may take up to 45 seconds.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (isTranslating) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                strokeWidth = 5.dp,
                                modifier = Modifier
                                    .size(70.dp)
                                    .padding(8.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Translating Exam...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Translating entire assessment content structure (questions, options, answers, guidelines) via Gemini AI. This maintains physical scale and layout format in the targeted language.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Error Toast Handler
            errorMsg?.let { error ->
                Snackbar(
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("OK", color = MaterialTheme.colorScheme.inversePrimary)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Text(text = error, maxLines = 4, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }

    // Dialog for Pasting study text
    if (showPasteDialog) {
        var pastedTitle by remember { mutableStateOf("") }
        var pastedContent by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showPasteDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxSize()
                ) {
                    Text(
                        text = "Paste Syllabus Content",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = pastedTitle,
                        onValueChange = { pastedTitle = it },
                        label = { Text("Material Topic / Name") },
                        placeholder = { Text("e.g., Cellular Mitochondria Bioscope") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = pastedContent,
                        onValueChange = { pastedContent = it },
                        label = { Text("Reference text contents") },
                        placeholder = { Text("Paste direct syllabus paragraphs, textbook definitions or article notes...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        maxLines = 150
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showPasteDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (pastedContent.isNotBlank()) {
                                    viewModel.handleTextMaterialPasted(pastedTitle, pastedContent)
                                    showPasteDialog = false
                                } else {
                                    Toast.makeText(context, "Please paste some study contents first!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text("Save Material")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(
    savedExams: List<ExamEntity>,
    onSelectExam: (ExamEntity) -> Unit,
    onDeleteExam: (Int) -> Unit,
    onComposeNew: () -> Unit
) {
    if (savedExams.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        RoundedCornerShape(50.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = "",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(50.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Welcome to AI Exam Builder",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Instantly synthesize 70 MCQs, 70 Fill in the Blanks, 30 True/False, 30 Short Questions, 25 Case Studies, and Numerical Equations from any study material PDF or text document.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onComposeNew,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Design New Exam Paper")
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Your Saved Assessments",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Text(
                text = "Review and export completed papers",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(savedExams) { exam ->
                    val totalCount = with(exam.details) {
                        mcqs.size + fillInBlanks.size + trueFalse.size + shortQuestions.size + caseDocs.size + numericals.size
                    }
                    val formattedDate = SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault())
                        .format(Date(exam.timestamp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectExam(exam) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = exam.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text("$totalCount Questions") }
                                    )
                                    Text(
                                        text = exam.sourceFileName,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Created Model: $formattedDate",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { onDeleteExam(exam.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onComposeNew,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Design New Exam Paper")
            }
        }
    }
}

@Composable
fun CreatorScreen(
    viewModel: ExamViewModel,
    activeTitle: String,
    selectedFileName: String?,
    activeDetails: ExamDetails,
    onOpenPasteDialog: () -> Unit,
    onViewFullExam: () -> Unit
) {
    val context = LocalContext.current

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.handleFilePicked(context, it) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Step 1: Syllabus Upload Card
        item {
            Text(
                text = "1. Load Syllabus Material",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (selectedFileName == null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No study source loaded",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Please attach a reference textbook PDF or paste raw syllabus context below to generate accurate questions.",
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { fileLauncher.launch("application/pdf") },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Select PDF")
                            }

                            OutlinedButton(onClick = onOpenPasteDialog) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = "")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Paste Text")
                            }
                        }
                    }
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Active Reference Material",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                text = selectedFileName,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { viewModel.clearActiveWorkspace() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove Material",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        // Step 2: Exam Meta Config (Title)
        item {
            Text(
                text = "2. Customize Exam Meta",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = activeTitle,
                onValueChange = { viewModel.updateActiveExamTitle(it) },
                label = { Text("Exam Paper Title") },
                placeholder = { Text("e.g. Calculus II Comprehensive Midterm") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Step 3: Question Type Generation Widgets
        item {
            Text(
                text = "3. Generate Assessment Sections",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Compile questions incrementally using Gemini AI",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // List of question modules
        item {
            QuestionGenerationCard(
                title = "Multiple Choice Questions (MCQs)",
                icon = "⁝≣",
                info = "Generate up to 70 questions with 4 choices, answers, and detailed explanations.",
                currentCount = activeDetails.mcqs.size,
                maxAllowed = 70,
                defaultSteps = listOf(10, 30, 70),
                isEnabled = selectedFileName != null,
                onGenerate = { count -> viewModel.triggerQuestionGeneration("MCQ", count) }
            )
        }

        item {
            QuestionGenerationCard(
                title = "Fill in the Blanks",
                icon = "✎_ ",
                info = "Generate up to 70 fill-in-the-blank items complete with solutions and details.",
                currentCount = activeDetails.fillInBlanks.size,
                maxAllowed = 70,
                defaultSteps = listOf(10, 30, 70),
                isEnabled = selectedFileName != null,
                onGenerate = { count -> viewModel.triggerQuestionGeneration("FillInBlank", count) }
            )
        }

        item {
            QuestionGenerationCard(
                title = "True / False Statements",
                icon = "✔✘",
                info = "Generate up to 30 True/False factual checks with conceptual logic explanations.",
                currentCount = activeDetails.trueFalse.size,
                maxAllowed = 30,
                defaultSteps = listOf(10, 20, 30),
                isEnabled = selectedFileName != null,
                onGenerate = { count -> viewModel.triggerQuestionGeneration("TrueFalse", count) }
            )
        }

        item {
            QuestionGenerationCard(
                title = "Short Answer Questions",
                icon = "✉✎",
                info = "Generate up to 30 short theoretical subjective questions with score weights and expected answer structures.",
                currentCount = activeDetails.shortQuestions.size,
                maxAllowed = 30,
                defaultSteps = listOf(10, 20, 30),
                isEnabled = selectedFileName != null,
                onGenerate = { count -> viewModel.triggerQuestionGeneration("Short", count) }
            )
        }

        item {
            QuestionGenerationCard(
                title = "Analytical Case Studies",
                icon = "📖⁝",
                info = "Generate up to 25 detailed scenario-based deep analyses, with granular sub-questions and marking guidelines.",
                currentCount = activeDetails.caseDocs.size,
                maxAllowed = 25,
                defaultSteps = listOf(5, 15, 25),
                isEnabled = selectedFileName != null,
                onGenerate = { count -> viewModel.triggerQuestionGeneration("CaseStudy", count) }
            )
        }

        item {
            QuestionGenerationCard(
                title = "Numerical Calculation Problems",
                icon = "＋÷",
                info = "Generate application-based mathematics and computation formulas with full step-by-step solutions.",
                currentCount = activeDetails.numericals.size,
                maxAllowed = 20,
                defaultSteps = listOf(5, 10, 20),
                isEnabled = selectedFileName != null,
                onGenerate = { count -> viewModel.triggerQuestionGeneration("Numerical", count) }
            )
        }

        // Section Summary and Full Review Card
        item {
            Spacer(modifier = Modifier.height(10.dp))
            val totalCreated = with(activeDetails) {
                mcqs.size + fillInBlanks.size + trueFalse.size + shortQuestions.size + caseDocs.size + numericals.size
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Exam Blueprint Status",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Cumulative Items: $totalCreated Questions created so far of target specs.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onViewFullExam,
                        enabled = totalCreated > 0,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Review Full Paper, Key & Marking Scheme")
                    }
                }
            }
        }
    }
}

@Composable
fun QuestionGenerationCard(
    title: String,
    icon: String,
    info: String,
    currentCount: Int,
    maxAllowed: Int,
    defaultSteps: List<Int>,
    isEnabled: Boolean,
    onGenerate: (Int) -> Unit
) {
    var selectedCount by remember { mutableStateOf(defaultSteps.first()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (currentCount > 0) {
                MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (currentCount > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = icon,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (currentCount > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                    Text(text = info, fontSize = 11.sp, color = Color.Gray, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "Amount:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    defaultSteps.forEach { step ->
                        FilterChip(
                            selected = selectedCount == step,
                            onClick = { selectedCount = step },
                            label = { Text("$step", fontSize = 11.sp) }
                        )
                    }
                }

                Button(
                    onClick = { onGenerate(selectedCount) },
                    enabled = isEnabled,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(
                        imageVector = if (currentCount > 0) Icons.Default.Check else Icons.Default.PlayArrow,
                        contentDescription = "Run compiler",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (currentCount > 0) "Add More ($selectedCount)" else "Generate ($selectedCount)",
                        fontSize = 11.sp
                    )
                }
            }

            if (currentCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { currentCount.toFloat() / maxAllowed.coerceAtLeast(1).toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Progress: $currentCount / $maxAllowed created",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (currentCount >= maxAllowed) {
                        Text(text = "Target Met!", fontSize = 11.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ViewerScreen(
    title: String,
    details: ExamDetails,
    onSaveToHistory: () -> Unit,
    onTranslate: (String) -> Unit,
    isTranslating: Boolean
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Question Paper", "Answer Key", "Marking Scheme")

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab row selector
        TabRow(selectedTabIndex = selectedTab) {
            tabTitles.forEachIndexed { index, text ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(text = text, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
            }
        }

        // Translation control row
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Translate:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val langs = listOf("English", "Urdu", "Arabic")
                    langs.forEach { lang ->
                        val isSelected = title.endsWith("($lang)")
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary 
                                    else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                )
                                .clickable(enabled = !isTranslating) { onTranslate(lang) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = lang,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                                        else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }

        // Floating Action Buttons container for export functions
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // LazyColumn scrolling item views
            when (selectedTab) {
                0 -> QuestionsPaperView(details = details)
                1 -> AnswerKeyView(details = details)
                2 -> MarkingSchemeView(details = details)
            }

            // Floated floating action layouts
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Save to historical folder
                FloatingActionButton(
                    onClick = onSaveToHistory,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(50.dp)
                ) {
                    Icon(imageVector = Icons.Default.Done, contentDescription = "Save Exam")
                }

                // Export to Word
                FloatingActionButton(
                    onClick = {
                        val htmlContent = ExportHelper.generateHtmlExam(title, details)
                        shareWordDoc(context, htmlContent, title)
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(50.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Export Word")
                }

                // Print as PDF system trigger
                FloatingActionButton(
                    onClick = {
                        val htmlContent = ExportHelper.generateHtmlExam(title, details)
                        printHtml(context, htmlContent, title)
                    },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.size(50.dp)
                ) {
                    Icon(imageVector = Icons.Default.Build, contentDescription = "PDF / Print")
                }
            }
        }
    }
}

@Composable
fun QuestionsPaperView(details: ExamDetails) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // A. MCQs List
        if (details.mcqs.isNotEmpty()) {
            item {
                SectionHeader("Section A: MCQ Assessments", "Answer every multiple choice by picking the single correct variant.")
            }
            items(details.mcqs) { mcq ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "${mcq.id}. ${mcq.question}", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        mcq.options.forEach { opt ->
                            Row(
                                modifier = Modifier
                                    .padding(vertical = 4.dp, horizontal = 8.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .border(1.5.dp, Color.Gray, RoundedCornerShape(5.dp))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = opt, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // B. Fill blanks
        if (details.fillInBlanks.isNotEmpty()) {
            item {
                SectionHeader("Section B: Fill in the Blanks", "Provide complete single-word or short answers in the blanks.")
            }
            items(details.fillInBlanks) { fib ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${fib.id}. ${fib.question}",
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // C. True or False
        if (details.trueFalse.isNotEmpty()) {
            item {
                SectionHeader("Section C: True or False Statements", "Write True or False beside each theoretical fact statement.")
            }
            items(details.trueFalse) { tf ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${tf.id}. Statement: ${tf.question}",
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // D. Short questions
        if (details.shortQuestions.isNotEmpty()) {
            item {
                SectionHeader("Section D: Short Answer Questions", "Draft targeted and precise explanation paragraphs.")
            }
            items(details.shortQuestions) { s ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${s.id}. ${s.question}",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        SuggestionChip(onClick = {}, label = { Text("${s.points} pts") })
                    }
                }
            }
        }

        // E. Case study
        if (details.caseDocs.isNotEmpty()) {
            item {
                SectionHeader("Section E: Analytical Case Studies", "Deconstruct scenario parameters and solve guiding inquiries.")
            }
            items(details.caseDocs) { case ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = "Case Study Scenario ${case.id}:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = case.scenario,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = "Guiding Analysis Inquiries:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        case.questions.forEachIndexed { index, q ->
                            Text(
                                text = "  ${index + 1}. $q",
                                fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // F. Numerical
        if (details.numericals.isNotEmpty()) {
            item {
                SectionHeader("Section F: Numerical & Calculations", "Present formulas, substitute variables and calculate correct answers.")
            }
            items(details.numericals) { num ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${num.id}. ${num.question}",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        SuggestionChip(onClick = {}, label = { Text("${num.points} pts") })
                    }
                }
            }
        }
    }
}

@Composable
fun AnswerKeyView(details: ExamDetails) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (details.mcqs.isNotEmpty()) {
            item { SectionHeader("MCQ Solutions & Explanations", "Correct choices matching academic study references.") }
            items(details.mcqs) { mcq ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFDCFCE7), RoundedCornerShape(8.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "${mcq.id}. ${mcq.question}", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Solution Answer: Option ${mcq.correctAnswer}", fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                        Text(text = "Logic explanation: ${mcq.explanation}", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }

        if (details.fillInBlanks.isNotEmpty()) {
            item { SectionHeader("Fill In Blank Answer Index", "Solutions for targeted word blanks.") }
            items(details.fillInBlanks) { fib ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFDCFCE7), RoundedCornerShape(8.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "${fib.id}. Blank Statement: ${fib.question}", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Key Answer: ${fib.correctAnswer}", fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                        Text(text = "Explanation: ${fib.explanation}", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }

        if (details.trueFalse.isNotEmpty()) {
            item { SectionHeader("True/False Truth Explanations", "Correct evaluation facts.") }
            items(details.trueFalse) { tf ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFDCFCE7), RoundedCornerShape(8.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "${tf.id}. Statement: ${tf.question}", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Truth Value: ${if (tf.correctAnswer) "True" else "False"}", fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                        Text(text = "Theoretical verification: ${tf.explanation}", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }

        if (details.shortQuestions.isNotEmpty()) {
            item { SectionHeader("Short Answers Expectation Guidelines", "Suggested answers for assessment scoring.") }
            items(details.shortQuestions) { s ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7).copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "${s.id}. Question: ${s.question}", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Suggested Key Structural Points:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(text = s.suggestedAnswer, fontSize = 13.sp)
                    }
                }
            }
        }

        if (details.caseDocs.isNotEmpty()) {
            item { SectionHeader("Case Study Analysis Keys", "Solution structures with contextual reference answers.") }
            items(details.caseDocs) { case ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7).copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "Case Study ${case.id} Answers:", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        case.questions.forEachIndexed { index, q ->
                            Text(text = "Q${index + 1}: $q", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Text(
                                text = "Expected key response points: ${case.answers.getOrNull(index) ?: "N/A"}",
                                fontSize = 13.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        if (details.numericals.isNotEmpty()) {
            item { SectionHeader("Numerical Derivation & Formula Steps", "Step-by-step substitution calculations.") }
            items(details.numericals) { num ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF5FF)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFF3E8FF), RoundedCornerShape(8.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "${num.id}. Question: ${num.question}", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Correct Answer Value: ${num.correctAnswer}", fontWeight = FontWeight.Bold, color = Color(0xFF7E22CE))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Derivation Pathway:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(
                            text = num.stepsToSolve,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .background(Color.White)
                                .padding(8.dp)
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MarkingSchemeView(details: ExamDetails) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (details.markingSchemes.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Marking schemes automatically compiled relative to custom generated section counts. Please compile questions first.",
                        modifier = Modifier.padding(20.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            item {
                SectionHeader("Assessor Marking Guidelines", "Detailed principles and criteria guidelines for scoring.")
            }
            items(details.markingSchemes) { scheme ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Assessment Style: ${scheme.type}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            SuggestionChip(
                                onClick = {},
                                label = { Text("${scheme.pointsPerQuestion} pts / item") }
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Grading Strategy Expectation:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Text(
                            text = scheme.criteria,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = subtitle,
            fontSize = 11.sp,
            color = Color.Gray
        )
    }
}

// Native printer integration helper
private fun printHtml(context: Context, html: String, jobName: String) {
    try {
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val printAdapter = webView.createPrintDocumentAdapter(jobName)
                printManager.print(
                    jobName,
                    printAdapter,
                    PrintAttributes.Builder().build()
                )
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    } catch (e: Exception) {
        Log.e("PrintPrinter", "Page finished print error", e)
        Toast.makeText(context, "System Print Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}

// Share doc document path
private fun shareWordDoc(context: Context, htmlContent: String, title: String) {
    try {
        val cleanTitle = title.replace("\\s+".toRegex(), "_")
        val file = File(context.cacheDir, "$cleanTitle.doc")
        val fos = FileOutputStream(file)
        fos.write(htmlContent.toByteArray(Charsets.UTF_8))
        fos.close()

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/msword"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Word Document"))
    } catch (e: Exception) {
        Log.e("ExportWord", "Saving Word Doc file failed", e)
        Toast.makeText(context, "Export Word failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}
