// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 MAIGUS

package com.maigus.aynehacalculator

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

class MainActivity : AppCompatActivity() {

    // --- État interne du calcul (toujours en chiffres arabes classiques en interne) ---
    private var currentInput: String = "0"
    private var firstOperand: BigDecimal? = null
    private var pendingOperator: Char? = null
    private var expressionLabel: String = ""
    private var resultLabel: String = ""
    private var startFreshNumber: Boolean = true
    private var isError: Boolean = false

    private lateinit var tvDisplay: TextView
    private lateinit var tvExpression: TextView
    private lateinit var tvResult: TextView
    private lateinit var svDisplay: HorizontalScrollView
    private lateinit var svExpression: HorizontalScrollView
    private lateinit var svResult: HorizontalScrollView

    // Correspondance chiffre arabe -> glyphe AYNEHA (U+10FF7C à U+10FF85, plan 16).
    // Ces points de code dépassent le plan de base : chaque glyphe occupe 2 unités UTF-16
    // (paire de substitution). On les manipule donc comme des String, jamais comme des Char.
    private val digitGlyph: Array<String> = Array(10) { glyph(0x10FF7C + it) }
    private val aynehaComma: String = glyph(0x10FFDC)

    private fun glyph(codePoint: Int): String = String(Character.toChars(codePoint))

    /** Inverse une chaîne en gardant chaque paire de substitution intacte
     *  (String.reversed() de Kotlin inverse les unités UTF-16 et casserait les glyphes). */
    private fun String.reversedByCodePoints(): String = StringBuilder(this).reverse().toString()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvDisplay = findViewById(R.id.tv_display)
        tvExpression = findViewById(R.id.tv_expression)
        tvResult = findViewById(R.id.tv_result)
        svDisplay = findViewById(R.id.sv_display)
        svExpression = findViewById(R.id.sv_expression)
        svResult = findViewById(R.id.sv_result)

        setupDigitButton(R.id.btn_0, "0")
        setupDigitButton(R.id.btn_1, "1")
        setupDigitButton(R.id.btn_2, "2")
        setupDigitButton(R.id.btn_3, "3")
        setupDigitButton(R.id.btn_4, "4")
        setupDigitButton(R.id.btn_5, "5")
        setupDigitButton(R.id.btn_6, "6")
        setupDigitButton(R.id.btn_7, "7")
        setupDigitButton(R.id.btn_8, "8")
        setupDigitButton(R.id.btn_9, "9")

        findViewById<Button>(R.id.btn_dot).setOnClickListener { onDot() }
        findViewById<Button>(R.id.btn_back).setOnClickListener { onBackspace() }
        findViewById<Button>(R.id.btn_ac).setOnClickListener { onAllClear() }
        findViewById<Button>(R.id.btn_percent).setOnClickListener { onPercent() }

        findViewById<Button>(R.id.btn_add).setOnClickListener { onOperator('+') }
        findViewById<Button>(R.id.btn_sub).setOnClickListener { onOperator('−') }
        findViewById<Button>(R.id.btn_mul).setOnClickListener { onOperator('×') }
        findViewById<Button>(R.id.btn_div).setOnClickListener { onOperator('÷') }

        findViewById<Button>(R.id.btn_eq).setOnClickListener { onEquals() }

        findViewById<TextView>(R.id.btn_info).setOnClickListener {
            startActivity(Intent(this, PrivacyActivity::class.java))
        }

        refreshDisplay()
    }

    private fun setupDigitButton(id: Int, digit: String) {
        findViewById<Button>(id).setOnClickListener { onDigit(digit) }
    }

    // --- Gestion des saisies ---

    private fun onDigit(digit: String) {
        if (isError) onAllClear()
        resultLabel = ""

        currentInput = if (startFreshNumber) {
            startFreshNumber = false
            if (digit == "0") "0" else digit
        } else {
            if (currentInput == "0") digit else currentInput + digit
        }
        refreshDisplay()
    }

    private fun onDot() {
        if (isError) onAllClear()
        resultLabel = ""

        if (startFreshNumber) {
            currentInput = "0."
            startFreshNumber = false
        } else if (!currentInput.contains(".")) {
            currentInput += "."
        }
        refreshDisplay()
    }

    private fun onBackspace() {
        if (isError) {
            onAllClear()
            return
        }
        resultLabel = ""
        currentInput = if (currentInput.length <= 1 || (currentInput.length == 2 && currentInput.startsWith("-"))) {
            "0"
        } else {
            currentInput.dropLast(1)
        }
        if (currentInput.isEmpty()) currentInput = "0"
        refreshDisplay()
    }

    private fun onAllClear() {
        currentInput = "0"
        firstOperand = null
        pendingOperator = null
        expressionLabel = ""
        resultLabel = ""
        startFreshNumber = true
        isError = false
        refreshDisplay()
    }

    private fun onPercent() {
        if (isError) return
        resultLabel = ""
        val value = currentInput.toBigDecimalOrNull() ?: return
        currentInput = trimDecimal(value.divide(BigDecimal(100), MathContext.DECIMAL64))
        refreshDisplay()
    }

    private fun onOperator(op: Char) {
        if (isError) return
        resultLabel = ""

        val value = currentInput.toBigDecimalOrNull() ?: return

        if (pendingOperator != null && !startFreshNumber) {
            val result = compute(firstOperand ?: BigDecimal.ZERO, value, pendingOperator!!)
            if (result == null) {
                showError()
                return
            }
            firstOperand = result
            currentInput = trimDecimal(result)
        } else {
            firstOperand = value
        }

        pendingOperator = op
        expressionLabel = "${trimDecimal(firstOperand!!)} $op"
        startFreshNumber = true
        refreshDisplay()
    }

    private fun onEquals() {
        if (isError) return
        val op = pendingOperator ?: return
        val second = currentInput.toBigDecimalOrNull() ?: return
        val first = firstOperand ?: return

        val result = compute(first, second, op)
        if (result == null) {
            showError()
            return
        }

        expressionLabel = "${trimDecimal(first)} $op ${trimDecimal(second)} ="
        resultLabel = trimDecimal(result)
        currentInput = trimDecimal(result)
        firstOperand = null
        pendingOperator = null
        startFreshNumber = true
        refreshDisplay()
    }

    private fun compute(a: BigDecimal, b: BigDecimal, op: Char): BigDecimal? {
        return try {
            when (op) {
                '+' -> a.add(b)
                '−' -> a.subtract(b)
                '×' -> a.multiply(b)
                '÷' -> {
                    if (b.compareTo(BigDecimal.ZERO) == 0) null
                    else a.divide(b, 12, RoundingMode.HALF_UP)
                }
                else -> null
            }
        } catch (e: ArithmeticException) {
            null
        }
    }

    private fun showError() {
        isError = true
        currentInput = "0"
        firstOperand = null
        pendingOperator = null
        startFreshNumber = true
        tvDisplay.text = "Erreur"
        tvExpression.text = toAyneha(expressionLabel).reversedByCodePoints()
        expressionLabel = ""
        resultLabel = ""
        tvResult.text = ""
        svDisplay.post { svDisplay.scrollTo(0, 0) }
        svExpression.post { svExpression.scrollTo(0, 0) }
    }

    private fun trimDecimal(value: BigDecimal): String {
        var v = value.stripTrailingZeros()
        if (v.scale() < 0) v = v.setScale(0)
        return v.toPlainString()
    }

    // --- Affichage ---

    private fun refreshDisplay() {
        // RTL : le chiffre le plus récent se place toujours à gauche du précédent —
        // on inverse la chaîne affichée (la valeur interne, elle, reste correcte).
        tvDisplay.text = toAyneha(currentInput).reversedByCodePoints()
        tvExpression.text = toAyneha(expressionLabel).reversedByCodePoints()
        tvResult.text = toAyneha(resultLabel).reversedByCodePoints()

        // Recentre automatiquement sur les chiffres les plus récents (bord gauche du
        // texte, puisque la chaîne est inversée) à chaque frappe. L'utilisateur peut
        // ensuite glisser le doigt à tout moment pour revoir le début de la saisie.
        svDisplay.post { svDisplay.scrollTo(0, 0) }
        svExpression.post { svExpression.scrollTo(0, 0) }
        svResult.post { svResult.scrollTo(0, 0) }
    }

    /** Convertit une chaîne de chiffres arabes classiques (avec éventuels '-' et '.')
     *  en chaîne composée des glyphes numériques AYNEHA. */
    private fun toAyneha(input: String): String {
        val sb = StringBuilder()
        for (c in input) {
            when {
                c in '0'..'9' -> sb.append(digitGlyph[c - '0'])
                c == '.' -> sb.append(aynehaComma)
                c == '-' -> sb.append('-')
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    private fun String.toBigDecimalOrNull(): BigDecimal? = try {
        BigDecimal(this)
    } catch (e: NumberFormatException) {
        null
    }
}
