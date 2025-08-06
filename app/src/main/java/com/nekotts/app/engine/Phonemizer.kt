package com.nekotts.app.engine

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Normalizer
import java.util.*
import android.content.Context

class Phonemizer(private val context: Context) {
    
    companion object {
        private const val TAG = "Phonemizer"
        private const val MAX_PHONEME_LENGTH = 400
    }
    
    // Default vocabulary mapping phonemes to tokens
    private val defaultVocab = mapOf(
        // Vowels
        "a" to 1, "ɑ" to 2, "ɒ" to 3, "æ" to 4, "ʌ" to 5, "ɔ" to 6, "o" to 7, "ə" to 8, "ɚ" to 9, "ɛ" to 10,
        "e" to 11, "ɪ" to 12, "i" to 13, "ɨ" to 14, "ɵ" to 15, "u" to 16, "ʊ" to 17, "y" to 18, "ø" to 19, "œ" to 20,
        "ɶ" to 21, "ɑ̃" to 22, "æ̃" to 23, "ɛ̃" to 24, "ɔ̃" to 25, "œ̃" to 26,
        
        // Consonants
        "b" to 30, "d" to 31, "f" to 32, "g" to 33, "h" to 34, "j" to 35, "k" to 36, "l" to 37, "m" to 38, "n" to 39,
        "p" to 40, "ɹ" to 41, "r" to 42, "s" to 43, "ʃ" to 44, "t" to 45, "v" to 46, "w" to 47, "x" to 48, "z" to 49,
        "ʒ" to 50, "ð" to 51, "θ" to 52, "ʔ" to 53, "ŋ" to 54, "ʍ" to 55, "ɾ" to 56, "ɽ" to 57, "ɸ" to 58, "β" to 59,
        "ɓ" to 60, "ɗ" to 61, "ʄ" to 62, "ɠ" to 63, "ʛ" to 64, "ɴ" to 65, "ɳ" to 66, "ɲ" to 67, "ɻ" to 68, "ʀ" to 69,
        "ʁ" to 70, "ɫ" to 71, "ɬ" to 72, "ɮ" to 73, "ʎ" to 74, "ʝ" to 75, "ɟ" to 76, "c" to 77, "ɖ" to 78, "ʈ" to 79,
        "q" to 80, "ɢ" to 81, "ʑ" to 82, "ʐ" to 83, "ɕ" to 84, "ç" to 85, "ʝ̊" to 86, "χ" to 87, "ʁ̝" to 88, "ħ" to 89,
        "ʕ" to 90, "ʡ" to 91, "ʢ" to 92,
        
        // Diphthongs
        "aɪ" to 100, "aʊ" to 101, "eɪ" to 102, "oʊ" to 103, "ɔɪ" to 104, "ɪə" to 105, "eə" to 106, "ʊə" to 107,
        
        // Stress markers
        "ˈ" to 150, "ˌ" to 151, "ː" to 152, "̆" to 153, "̯" to 154, "˞" to 155,
        
        // Special characters
        " " to 200, "." to 201, "," to 202, "?" to 203, "!" to 204, ":" to 205, ";" to 206, "-" to 207, "'" to 208,
        "\"" to 209, "(" to 210, ")" to 211, "[" to 212, "]" to 213, "{" to 214, "}" to 215,
        
        // Silence and padding
        "<pad>" to 0, "<unk>" to 216, "<s>" to 217, "</s>" to 218, "<sil>" to 219
    )
    
    // Simple G2P mappings for common English patterns
    private val englishG2PMappings = mapOf(
        // Vowel patterns
        "a" to "æ", "e" to "ɛ", "i" to "ɪ", "o" to "ɒ", "u" to "ʌ", "y" to "aɪ",
        "ai" to "eɪ", "ay" to "eɪ", "ee" to "iː", "ea" to "iː", "ie" to "iː", "oo" to "uː", "ou" to "aʊ", "ow" to "aʊ",
        "oa" to "oʊ", "oe" to "oʊ", "au" to "ɔː", "aw" to "ɔː", "oy" to "ɔɪ", "oi" to "ɔɪ",
        "ar" to "ɑːɹ", "or" to "ɔːɹ", "er" to "ɚ", "ir" to "ɚ", "ur" to "ɚ", "ear" to "ɪəɹ", "air" to "eəɹ",
        
        // Consonant patterns
        "b" to "b", "c" to "k", "d" to "d", "f" to "f", "g" to "g", "h" to "h", "j" to "dʒ", "k" to "k",
        "l" to "l", "m" to "m", "n" to "n", "p" to "p", "q" to "kw", "r" to "ɹ", "s" to "s", "t" to "t",
        "v" to "v", "w" to "w", "x" to "ks", "z" to "z",
        
        // Digraphs
        "ch" to "tʃ", "sh" to "ʃ", "th" to "θ", "wh" to "w", "ph" to "f", "gh" to "g", "ck" to "k",
        "ng" to "ŋ", "nk" to "ŋk"
    )
    
    // Language-specific phoneme mappings
    private val languagePhonemes = mapOf(
        "en" to englishG2PMappings,
        "en-us" to englishG2PMappings,
        "en-gb" to englishG2PMappings.plus(mapOf(
            "a" to "ɑ", "o" to "ɔ", "au" to "ɔː", "aw" to "ɔː"
        )),
        
        // Basic Spanish mappings
        "es" to mapOf(
            "a" to "a", "e" to "e", "i" to "i", "o" to "o", "u" to "u",
            "b" to "b", "c" to "k", "d" to "d", "f" to "f", "g" to "g", "h" to "", "j" to "x",
            "k" to "k", "l" to "l", "m" to "m", "n" to "n", "p" to "p", "r" to "r", "s" to "s",
            "t" to "t", "v" to "b", "w" to "w", "x" to "ks", "y" to "ʝ", "z" to "θ",
            "ch" to "tʃ", "ll" to "ʎ", "ñ" to "ɲ", "rr" to "rr"
        ),
        
        // Basic French mappings
        "fr" to mapOf(
            "a" to "a", "e" to "ə", "i" to "i", "o" to "o", "u" to "y", "é" to "e", "è" to "ɛ",
            "ê" to "ɛ", "ô" to "o", "ù" to "y", "à" to "a", "ç" to "s", "î" to "i", "û" to "y",
            "b" to "b", "c" to "k", "d" to "d", "f" to "f", "g" to "g", "h" to "", "j" to "ʒ",
            "k" to "k", "l" to "l", "m" to "m", "n" to "n", "p" to "p", "r" to "ʁ", "s" to "s",
            "t" to "t", "v" to "v", "w" to "w", "x" to "ks", "y" to "i", "z" to "z",
            "ch" to "ʃ", "gn" to "ɲ", "qu" to "k", "th" to "t"
        ),
        
        // Basic German mappings
        "de" to mapOf(
            "a" to "a", "e" to "e", "i" to "ɪ", "o" to "o", "u" to "ʊ", "ä" to "ɛ", "ö" to "ø", "ü" to "y",
            "b" to "b", "c" to "k", "d" to "d", "f" to "f", "g" to "g", "h" to "h", "j" to "j",
            "k" to "k", "l" to "l", "m" to "m", "n" to "n", "p" to "p", "r" to "ʁ", "s" to "s",
            "t" to "t", "v" to "f", "w" to "v", "x" to "ks", "y" to "y", "z" to "ts",
            "ch" to "x", "sch" to "ʃ", "th" to "t", "ph" to "f", "qu" to "kv", "ß" to "s"
        )
    )
    
    fun phonemize(text: String, language: String = "en-us"): String {
        return try {
            Log.d(TAG, "Phonemizing text: '$text' for language: $language")
            
            // Normalize and clean text
            val cleanText = normalizeText(text)
            if (cleanText.isEmpty()) {
                Log.w(TAG, "Text is empty after normalization")
                return ""
            }
            
            // Apply language-specific phonemization
            val phonemes = when (language.lowercase()) {
                "en", "en-us", "en-gb" -> phonemizeEnglish(cleanText, language)
                "es" -> phonemizeSpanish(cleanText)
                "fr" -> phonemizeFrench(cleanText)
                "de" -> phonemizeGerman(cleanText)
                else -> {
                    Log.w(TAG, "Language $language not supported, using English")
                    phonemizeEnglish(cleanText, "en-us")
                }
            }
            
            // Filter valid phonemes and limit length
            val filteredPhonemes = phonemes
                .take(MAX_PHONEME_LENGTH)
            
            Log.d(TAG, "Phonemization result: '$filteredPhonemes'")
            filteredPhonemes
            
        } catch (e: Exception) {
            Log.e(TAG, "Phonemization failed for text: '$text'", e)
            // Fallback to simple character mapping
            normalizeText(text).take(50)
        }
    }
    
    private fun normalizeText(text: String): String {
        return Normalizer.normalize(text, Normalizer.Form.NFKD)
            .lowercase(Locale.ENGLISH)
            .replace(Regex("[^\\p{L}\\p{N}\\s.,?!:;'-]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
    
    private fun phonemizeEnglish(text: String, variant: String = "en-us"): String {
        val mappings = languagePhonemes[variant] ?: englishG2PMappings
        val result = StringBuilder()
        val words = text.split(Regex("\\s+"))
        
        for ((wordIndex, word) in words.withIndex()) {
            if (wordIndex > 0) result.append(" ")
            result.append(convertWordToPhonemes(word, mappings))
        }
        
        return result.toString()
    }
    
    private fun phonemizeSpanish(text: String): String {
        val mappings = languagePhonemes["es"] ?: emptyMap()
        return convertTextToPhonemes(text, mappings)
    }
    
    private fun phonemizeFrench(text: String): String {
        val mappings = languagePhonemes["fr"] ?: emptyMap()
        return convertTextToPhonemes(text, mappings)
    }
    
    private fun phonemizeGerman(text: String): String {
        val mappings = languagePhonemes["de"] ?: emptyMap()
        return convertTextToPhonemes(text, mappings)
    }
    
    private fun convertTextToPhonemes(text: String, mappings: Map<String, String>): String {
        val result = StringBuilder()
        val words = text.split(Regex("\\s+"))
        
        for ((wordIndex, word) in words.withIndex()) {
            if (wordIndex > 0) result.append(" ")
            result.append(convertWordToPhonemes(word, mappings))
        }
        
        return result.toString()
    }
    
    private fun convertWordToPhonemes(word: String, mappings: Map<String, String>): String {
        val result = StringBuilder()
        var i = 0
        
        while (i < word.length) {
            var matched = false
            
            // Try longer patterns first
            for (length in 3 downTo 1) {
                if (i + length <= word.length) {
                    val substring = word.substring(i, i + length)
                    mappings[substring]?.let { phoneme ->
                        if (phoneme.isNotEmpty()) {
                            result.append(phoneme)
                        }
                        i += length
                        matched = true
                        return@let
                    }
                }
            }
            
            if (!matched) {
                // Fallback: use character directly if it's valid
                val char = word[i].toString()
                if (defaultVocab.containsKey(char)) {
                    result.append(char)
                }
                i++
            }
        }
        
        return result.toString()
    }
    
    fun tokenize(phonemes: String): List<Int> {
        if (phonemes.length > MAX_PHONEME_LENGTH) {
            Log.w(TAG, "Phonemes too long (${phonemes.length} > $MAX_PHONEME_LENGTH), truncating")
        }
        
        val tokens = mutableListOf<Int>()
        var i = 0
        
        while (i < phonemes.length && i < MAX_PHONEME_LENGTH) {
            var matched = false
            
            // Try longer phonemes first (e.g., "aɪ" before "a")
            for (length in 3 downTo 1) {
                if (i + length <= phonemes.length) {
                    val substring = phonemes.substring(i, i + length)
                    defaultVocab[substring]?.let { tokenId ->
                        tokens.add(tokenId)
                        i += length
                        matched = true
                        return@let
                    }
                }
            }
            
            if (!matched) {
                // Unknown character, use UNK token
                tokens.add(defaultVocab["<unk>"] ?: 216)
                i++
            }
        }
        
        return tokens
    }
    
    suspend fun phonemizeAsync(text: String, language: String = "en-us"): String = withContext(Dispatchers.IO) {
        phonemize(text, language)
    }
    
    suspend fun tokenizeAsync(phonemes: String): List<Int> = withContext(Dispatchers.IO) {
        tokenize(phonemes)
    }
    
    fun getSupportedLanguages(): List<String> = languagePhonemes.keys.toList()
    
    fun getVocabularySize(): Int = defaultVocab.size
    
    fun getPhonemeForToken(tokenId: Int): String? {
        return defaultVocab.entries.find { it.value == tokenId }?.key
    }
    
    fun getTokenForPhoneme(phoneme: String): Int? = defaultVocab[phoneme]
    
    fun isValidPhoneme(phoneme: String): Boolean = defaultVocab.containsKey(phoneme)
    
    fun preprocessTextForTTS(text: String): String {
        return text
            // Handle common abbreviations
            .replace(Regex("\\bMr\\."), "Mister")
            .replace(Regex("\\bMrs\\."), "Misses")
            .replace(Regex("\\bDr\\."), "Doctor")
            .replace(Regex("\\bProf\\."), "Professor")
            .replace(Regex("\\bSt\\."), "Saint")
            .replace(Regex("\\bAve\\."), "Avenue")
            .replace(Regex("\\bRd\\."), "Road")
            .replace(Regex("\\bBlvd\\."), "Boulevard")
            
            // Handle numbers (basic)
            .replace(Regex("\\b(\\d+)\\b")) { matchResult ->
                val number = matchResult.value.toIntOrNull()
                if (number != null && number < 100) {
                    convertNumberToWords(number)
                } else {
                    matchResult.value
                }
            }
            
            // Handle common symbols
            .replace("&", "and")
            .replace("@", "at")
            .replace("%", "percent")
            .replace("$", "dollars")
            .replace("€", "euros")
            .replace("£", "pounds")
            
            // Normalize whitespace and punctuation
            .replace(Regex("[\\r\\n]+"), ". ")
            .replace(Regex("[\\t\\f]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
    
    private fun convertNumberToWords(number: Int): String {
        return when (number) {
            0 -> "zero"
            1 -> "one"
            2 -> "two"
            3 -> "three"
            4 -> "four"
            5 -> "five"
            6 -> "six"
            7 -> "seven"
            8 -> "eight"
            9 -> "nine"
            10 -> "ten"
            11 -> "eleven"
            12 -> "twelve"
            13 -> "thirteen"
            14 -> "fourteen"
            15 -> "fifteen"
            16 -> "sixteen"
            17 -> "seventeen"
            18 -> "eighteen"
            19 -> "nineteen"
            20 -> "twenty"
            30 -> "thirty"
            40 -> "forty"
            50 -> "fifty"
            60 -> "sixty"
            70 -> "seventy"
            80 -> "eighty"
            90 -> "ninety"
            else -> {
                when {
                    number < 30 -> "twenty ${convertNumberToWords(number - 20)}"
                    number < 40 -> "thirty ${convertNumberToWords(number - 30)}"
                    number < 50 -> "forty ${convertNumberToWords(number - 40)}"
                    number < 60 -> "fifty ${convertNumberToWords(number - 50)}"
                    number < 70 -> "sixty ${convertNumberToWords(number - 60)}"
                    number < 80 -> "seventy ${convertNumberToWords(number - 70)}"
                    number < 90 -> "eighty ${convertNumberToWords(number - 80)}"
                    number < 100 -> "ninety ${convertNumberToWords(number - 90)}"
                    else -> number.toString()
                }
            }
        }
    }
    
    fun addCustomPhoneme(phoneme: String, tokenId: Int): Boolean {
        return if (!defaultVocab.containsKey(phoneme) && !defaultVocab.containsValue(tokenId)) {
            // In a real implementation, this would update the vocabulary
            Log.d(TAG, "Would add custom phoneme: $phoneme -> $tokenId")
            true
        } else {
            false
        }
    }
    
    fun getPhonemizationInfo(): Map<String, Any> {
        return mapOf(
            "vocabularySize" to defaultVocab.size,
            "maxPhonemeLength" to MAX_PHONEME_LENGTH,
            "supportedLanguages" to getSupportedLanguages(),
            "defaultLanguage" to "en-us"
        )
    }
}