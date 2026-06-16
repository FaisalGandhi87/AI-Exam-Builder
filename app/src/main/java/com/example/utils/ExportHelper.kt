package com.example.utils

import com.example.data.model.ExamDetails

object ExportHelper {

    fun generateMarkdownExam(title: String, details: ExamDetails): String {
        val sb = StringBuilder()
        sb.append("# $title\n")
        sb.append("Generated with AI Exam Builder\n\n")

        sb.append("## PART I: QUESTION PAPER\n\n")

        // 1. MCQs
        if (details.mcqs.isNotEmpty()) {
            sb.append("### Section A: Multiple Choice Questions (1 Point each)\n\n")
            details.mcqs.forEach { mcq ->
                sb.append("${mcq.id}. ${mcq.question}\n")
                mcq.options.forEach { opt ->
                    sb.append("   $opt\n")
                }
                sb.append("\n")
            }
        }

        // 2. Fill in the Blanks
        if (details.fillInBlanks.isNotEmpty()) {
            sb.append("### Section B: Fill in the Blanks (1 Point each)\n\n")
            details.fillInBlanks.forEach { fib ->
                sb.append("${fib.id}. ${fib.question}\n")
            }
            sb.append("\n")
        }

        // 3. True or False
        if (details.trueFalse.isNotEmpty()) {
            sb.append("### Section C: True or False Statements (1 Point each)\n\n")
            details.trueFalse.forEach { tf ->
                sb.append("${tf.id}. Statement: ${tf.question}\n")
            }
            sb.append("\n")
        }

        // 4. Short Questions
        if (details.shortQuestions.isNotEmpty()) {
            sb.append("### Section D: Short Answer Questions\n\n")
            details.shortQuestions.forEach { s ->
                sb.append("${s.id}. ${s.question} (${s.points} Points)\n\n")
            }
        }

        // 5. Case Studies
        if (details.caseDocs.isNotEmpty()) {
            sb.append("### Section E: Case Study Analytical Scenarios\n\n")
            details.caseDocs.forEach { case ->
                sb.append("Case Study ${case.id}:\n")
                sb.append("${case.scenario}\n\n")
                sb.append("Guiding Questions:\n")
                case.questions.forEachIndexed { qIdx, question ->
                    sb.append("  ${qIdx + 1}. $question\n")
                }
                sb.append("\n")
            }
        }

        // 6. Numerical Questions
        if (details.numericals.isNotEmpty()) {
            sb.append("### Section F: Numerical & Calculations\n\n")
            details.numericals.forEach { num ->
                sb.append("${num.id}. ${num.question} (${num.points} Points)\n\n")
            }
        }

        sb.append("\n---\n\n")
        sb.append("## PART II: ANSWER KEY\n\n")

        if (details.mcqs.isNotEmpty()) {
            sb.append("### MCQ Answers\n\n")
            details.mcqs.forEach { mcq ->
                sb.append("${mcq.id}. Correct Answer: Option **${mcq.correctAnswer}**\n")
                sb.append("   *Explanation:* ${mcq.explanation}\n\n")
            }
        }

        if (details.fillInBlanks.isNotEmpty()) {
            sb.append("### Fill in the Blank Answers\n\n")
            details.fillInBlanks.forEach { fib ->
                sb.append("${fib.id}. Answer: **${fib.correctAnswer}**\n")
                sb.append("   *Explanation:* ${fib.explanation}\n\n")
            }
        }

        if (details.trueFalse.isNotEmpty()) {
            sb.append("### True/False Answers\n\n")
            details.trueFalse.forEach { tf ->
                val ansStr = if (tf.correctAnswer) "True" else "False"
                sb.append("${tf.id}. Answer: **$ansStr**\n")
                sb.append("   *Explanation:* ${tf.explanation}\n\n")
            }
        }

        if (details.shortQuestions.isNotEmpty()) {
            sb.append("### Short Answer Suggested Guidelines\n\n")
            details.shortQuestions.forEach { s ->
                sb.append("${s.id}. Question: ${s.question}\n")
                sb.append("   Suggested Answer: ${s.suggestedAnswer}\n\n")
            }
        }

        if (details.caseDocs.isNotEmpty()) {
            sb.append("### Case Study Answers & Analysis\n\n")
            details.caseDocs.forEach { case ->
                sb.append("Case Study ${case.id} Assessment Guidelines:\n")
                case.questions.forEachIndexed { idx, q ->
                    sb.append("  Question ${idx + 1}: $q\n")
                    sb.append("  Suggested Response: ${case.answers.getOrNull(idx) ?: "N/A"}\n\n")
                }
            }
        }

        if (details.numericals.isNotEmpty()) {
            sb.append("### Numerical Calculations Answers\n\n")
            details.numericals.forEach { num ->
                sb.append("${num.id}. Question: ${num.question}\n")
                sb.append("   Final Answer: **${num.correctAnswer}**\n")
                sb.append("   Steps to Solve:\n   ${num.stepsToSolve}\n\n")
            }
        }

        sb.append("\n---\n\n")
        sb.append("## PART III: MARKING SCHEME SUMMARY\n\n")
        if (details.markingSchemes.isNotEmpty()) {
            details.markingSchemes.forEach { scheme ->
                sb.append("### Evaluation Criteria - ${scheme.type}\n")
                sb.append("- **Weight per item:** ${scheme.pointsPerQuestion} Points\n")
                sb.append("- **Grading Principles:** ${scheme.criteria}\n\n")
            }
        } else {
            sb.append("Standard academic grading principles apply. Ensure clear alignment between student logic and theoretical concepts.\n")
        }

        return sb.toString()
    }

    fun generateHtmlExam(title: String, details: ExamDetails): String {
        val sb = StringBuilder()
        sb.append("""
            <!DOCTYPE html>
            <html>
            <head>
            <meta charset="utf-8">
            <title>$title</title>
            <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; line-height: 1.6; color: #333; margin: 40px; }
                h1 { color: #1e3a8a; text-align: center; border-bottom: 2px solid #1e3a8a; padding-bottom: 10px; margin-bottom: 30px; }
                h2 { color: #2563eb; border-bottom: 1px solid #e5e7eb; padding-bottom: 5px; margin-top: 40px; }
                h3 { color: #1e40af; background-color: #f3f4f6; padding: 6px 12px; border-radius: 4px; }
                .question { margin-bottom: 20px; padding: 10px; border-left: 3px solid #60a5fa; background: #fafafa; }
                .options { margin-left: 20px; list-style-type: none; padding-left: 0; }
                .options li { margin: 5px 0; }
                .explanation { font-style: italic; color: #4b5563; margin-top: 8px; font-size: 0.9em; background: #f0fdf4; padding: 8px; border-radius: 4px; border: 1px dashed #86efac; }
                .answer-section { margin-top: 15px; background: #fffbeb; padding: 10px; border: 1px solid #fef3c7; border-radius: 4px; }
                .bold { font-weight: bold; }
                .page-break { page-break-before: always; }
                .badge { display: inline-block; background-color: #3b82f6; color: white; padding: 2px 8px; border-radius: 12px; font-size: 0.8em; font-weight: bold; }
            </style>
            </head>
            <body>
                <h1>$title</h1>
                <p style="text-align: center; font-size: 0.9em; color: #666;">Generated via AI Exam Builder</p>
                
                <h2>PART I: QUESTION PAPER</h2>
                <hr>
        """.trimIndent())

        // 1. MCQs
        if (details.mcqs.isNotEmpty()) {
            sb.append("<h3>Section A: Multiple Choice Questions (1 Point each)</h3>")
            details.mcqs.forEach { mcq ->
                sb.append("<div class='question'>")
                sb.append("<p class='bold'>${mcq.id}. ${mcq.question}</p>")
                sb.append("<ul class='options'>")
                mcq.options.forEach { opt ->
                    sb.append("<li>$opt</li>")
                }
                sb.append("</ul>")
                sb.append("</div>")
            }
        }

        // 2. Fill in the Blanks
        if (details.fillInBlanks.isNotEmpty()) {
            sb.append("<h3>Section B: Fill in the Blanks (1 Point each)</h3>")
            details.fillInBlanks.forEach { fib ->
                sb.append("<div class='question'>")
                sb.append("<p>${fib.id}. ${fib.question}</p>")
                sb.append("</div>")
            }
        }

        // 3. True or False
        if (details.trueFalse.isNotEmpty()) {
            sb.append("<h3>Section C: True or False Statements (1 Point each)</h3>")
            details.trueFalse.forEach { tf ->
                sb.append("<div class='question'>")
                sb.append("<p>${tf.id}. Statement: ${tf.question}</p>")
                sb.append("</div>")
            }
        }

        // 4. Short Questions
        if (details.shortQuestions.isNotEmpty()) {
            sb.append("<h3>Section D: Short Answer Questions</h3>")
            details.shortQuestions.forEach { s ->
                sb.append("<div class='question'>")
                sb.append("<p class='bold'>${s.id}. ${s.question} <span class='badge'>${s.points} pts</span></p>")
                sb.append("</div>")
            }
        }

        // 5. Case Studies
        if (details.caseDocs.isNotEmpty()) {
            sb.append("<h3>Section E: Case Study Analytical Scenarios</h3>")
            details.caseDocs.forEach { case ->
                sb.append("<div class='question' style='border-left-color: #f59e0b;'>")
                sb.append("<p class='bold'>Case Study ${case.id}:</p>")
                sb.append("<p style='background: white; padding: 10px; border: 1px solid #e5e7eb; border-radius: 4px;'>${case.scenario}</p>")
                sb.append("<p class='bold'>Analytical Questions:</p>")
                sb.append("<ol>")
                case.questions.forEach { q ->
                    sb.append("<li>$q</li>")
                }
                sb.append("</ol>")
                sb.append("</div>")
            }
        }

        // 6. Numerical Questions
        if (details.numericals.isNotEmpty()) {
            sb.append("<h3>Section F: Numerical & Calculations</h3>")
            details.numericals.forEach { num ->
                sb.append("<div class='question' style='border-left-color: #ec4899;'>")
                sb.append("<p class='bold'>${num.id}. ${num.question} <span class='badge'>${num.points} pts</span></p>")
                sb.append("</div>")
            }
        }

        // Answer Key Page Break
        sb.append("<div class='page-break'></div>")
        sb.append("""
            <h2>PART II: ANSWER KEY</h2>
            <hr>
        """.trimIndent())

        if (details.mcqs.isNotEmpty()) {
            sb.append("<h3>MCQ Answers</h3>")
            details.mcqs.forEach { mcq ->
                sb.append("<div class='answer-section'>")
                sb.append("<p class='bold'>${mcq.id}. Correct Answer: Option ${mcq.correctAnswer}</p>")
                sb.append("<div class='explanation'><strong>Explanation:</strong> ${mcq.explanation}</div>")
                sb.append("</div>")
            }
        }

        if (details.fillInBlanks.isNotEmpty()) {
            sb.append("<h3>Fill in the Blank Answers</h3>")
            details.fillInBlanks.forEach { fib ->
                sb.append("<div class='answer-section'>")
                sb.append("<p class='bold'>${fib.id}. Correct Answer: ${fib.correctAnswer}</p>")
                sb.append("<div class='explanation'><strong>Explanation:</strong> ${fib.explanation}</div>")
                sb.append("</div>")
            }
        }

        if (details.trueFalse.isNotEmpty()) {
            sb.append("<h3>True/False Answers</h3>")
            details.trueFalse.forEach { tf ->
                val ansStr = if (tf.correctAnswer) "True" else "False"
                sb.append("<div class='answer-section'>")
                sb.append("<p class='bold'>${tf.id}. Correct Answer: $ansStr</p>")
                sb.append("<div class='explanation'><strong>Explanation:</strong> ${tf.explanation}</div>")
                sb.append("</div>")
            }
        }

        if (details.shortQuestions.isNotEmpty()) {
            sb.append("<h3>Short Answer Expected Key Guidelines</h3>")
            details.shortQuestions.forEach { s ->
                sb.append("<div class='answer-section'>")
                sb.append("<p class='bold'>${s.id}. Question: ${s.question}</p>")
                sb.append("<p><strong>Suggested Answer Structure:</strong> ${s.suggestedAnswer}</p>")
                sb.append("</div>")
            }
        }

        if (details.caseDocs.isNotEmpty()) {
            sb.append("<h3>Case Studies Guidelines & Solutions</h3>")
            details.caseDocs.forEach { case ->
                sb.append("<div class='answer-section' style='border-left: 3px solid #f59e0b;'>")
                sb.append("<p class='bold'>Case Study ${case.id} Solution Keys:</p>")
                case.questions.forEachIndexed { qIdx, question ->
                    sb.append("<p><strong>Q${qIdx + 1}:</strong> $question</p>")
                    sb.append("<p><strong>Expected Answer:</strong> ${case.answers.getOrNull(qIdx) ?: "N/A"}</p>")
                }
                sb.append("<div class='explanation'><strong>Evaluation Guidelines:</strong> ${case.markingGuidelines}</div>")
                sb.append("</div>")
            }
        }

        if (details.numericals.isNotEmpty()) {
            sb.append("<h3>Numerical Step-by-Step Solutions</h3>")
            details.numericals.forEach { num ->
                sb.append("<div class='answer-section' style='border-left: 3px solid #ec4899;'>")
                sb.append("<p class='bold'>${num.id}. Question: ${num.question}</p>")
                sb.append("<p><strong>Final Correct Value:</strong> <span style='color: #db2777; font-weight: bold;'>${num.correctAnswer}</span></p>")
                sb.append("<div class='explanation' style='background-color: #fdf2f8; border-color: #fbcfe8;'><strong>Derivation & Steps:</strong><br>${num.stepsToSolve.replace("\n", "<br>")}</div>")
                sb.append("</div>")
            }
        }

        // Marking Scheme Page Break
        sb.append("<div class='page-break'></div>")
        sb.append("""
            <h2>PART III: MARKING SCHEME & SCORING PRINCIPLES</h2>
            <hr>
        """.trimIndent())

        if (details.markingSchemes.isNotEmpty()) {
            details.markingSchemes.forEach { scheme ->
                sb.append("<div style='background: #f8fafc; padding: 15px; border-radius: 6px; margin-bottom: 20px; border: 1px solid #cbd5e1;'>")
                sb.append("<p class='bold' style='color:#0f172a; margin-top:0;'>Assessment Type: ${scheme.type}</p>")
                sb.append("<p><strong>Marks Allotted:</strong> ${scheme.pointsPerQuestion} Points per item</p>")
                sb.append("<p><strong>Scoring Strategy:</strong> ${scheme.criteria}</p>")
                sb.append("</div>")
            }
        } else {
            sb.append("<p>Standard marking guidelines apply relative to individual question weights.</p>")
        }

        sb.append("""
            </body>
            </html>
        """.trimIndent())

        return sb.toString()
    }
}
