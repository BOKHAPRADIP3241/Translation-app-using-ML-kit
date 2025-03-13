package com.example.translationapp

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var sourceLanguageSpinner: Spinner
    private lateinit var targetLanguageSpinner: Spinner
    private lateinit var swapButton: ImageButton
    private lateinit var inputText: EditText
    private lateinit var translatedText: TextView
    private lateinit var translator: Translator

    private val SPEECH_REQUEST_CODE = 100

    private val languageMap = mapOf(
        "Spanish" to TranslateLanguage.SPANISH,
        "English" to TranslateLanguage.ENGLISH,
        "French" to TranslateLanguage.FRENCH,
        "German" to TranslateLanguage.GERMAN,
        "Hindi" to TranslateLanguage.HINDI
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
// Initialize views
        sourceLanguageSpinner = findViewById(R.id.sourceLanguageSpinner)
        targetLanguageSpinner = findViewById(R.id.targetLanguageSpinner)
        swapButton = findViewById(R.id.swapButton)
        inputText = findViewById(R.id.inputText)
        translatedText = findViewById(R.id.translatedText)

        // Set default languages
        sourceLanguageSpinner.setSelection(0) // Spanish
        targetLanguageSpinner.setSelection(1) // English

        // Initialize translator
        setupTranslator()

        findViewById<ImageButton>(R.id.micButton).setOnClickListener {
            startSpeechToText()
        }

        // Swap button functionality
        swapButton.setOnClickListener {
            val sourcePos = sourceLanguageSpinner.selectedItemPosition
            val targetPos = targetLanguageSpinner.selectedItemPosition
            sourceLanguageSpinner.setSelection(targetPos)
            targetLanguageSpinner.setSelection(sourcePos)
            setupTranslator() // Reinitialize translator with new language pair
        }

        // Listen for language changes
        sourceLanguageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                setupTranslator()
                translateText()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        targetLanguageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                setupTranslator()
                translateText()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Listen for text input changes
        inputText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                translateText()
            }
        })

    }

    private fun startSpeechToText() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE)
        } catch (e: Exception) {
            translatedText.text = "Speech recognition error: ${e.message}"
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            val results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                inputText.setText(results[0])
            }
        }
    }

    private fun setupTranslator() {
        val sourceLang = languageMap[sourceLanguageSpinner.selectedItem.toString()]
        val targetLang = languageMap[targetLanguageSpinner.selectedItem.toString()]
        if (sourceLang != null && targetLang != null) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLang)
                .setTargetLanguage(targetLang)
                .build()
            translator = Translation.getClient(options)

            // Download the model if needed
            translator.downloadModelIfNeeded()
                .addOnSuccessListener {
                    // Model downloaded successfully
                }
                .addOnFailureListener { exception ->
                    translatedText.text = "Error downloading model: ${exception.message}"
                }
        }
    }

    private fun translateText() {
        val text = inputText.text.toString().trim()
        if (text.isEmpty()) {
            translatedText.text = "Translation"
            return
        }

        translator.translate(text)
            .addOnSuccessListener { translated ->
                translatedText.text = translated
            }
            .addOnFailureListener { exception ->
                translatedText.text = "Translation error: ${exception.message}"
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        translator.close()
    }
}