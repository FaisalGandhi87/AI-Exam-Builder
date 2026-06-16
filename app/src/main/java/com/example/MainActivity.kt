package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.database.AppDatabase
import com.example.data.repository.ExamRepository
import com.example.ui.screens.ExamAppLayout
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ExamViewModel
import com.example.ui.viewmodel.ExamViewModelFactory

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val database = AppDatabase.getDatabase(this)
    val repository = ExamRepository(database.examDao())
    val factory = ExamViewModelFactory(application, repository)
    val viewModel = ViewModelProvider(this, factory)[ExamViewModel::class.java]

    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
          ExamAppLayout(viewModel = viewModel)
      }
    }
  }
}
