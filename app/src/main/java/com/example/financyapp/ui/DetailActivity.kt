package com.example.financyapp.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.financyapp.R
import com.example.financyapp.api.ApiClient
import com.example.financyapp.model.Operation
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Calendar

class DetailActivity : AppCompatActivity() {
    private lateinit var radioGroupType: RadioGroup
    private lateinit var radioButtonIncome: RadioButton
    private lateinit var radioButtonExpense: RadioButton
    private lateinit var editTextAmount: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var editTextDate: EditText
    private lateinit var editTextNote: EditText
    private lateinit var buttonSave: Button
    private lateinit var buttonDelete: Button

    private var operationId: Int? = null
    private var currentOperation: Operation? = null

    companion object {
        private const val KEY_OPERATION_ID = "operation_id"
        private const val KEY_TYPE = "type"
        private const val KEY_AMOUNT = "amount"
        private const val KEY_CATEGORY = "category"
        private const val KEY_DATE = "date"
        private const val KEY_NOTE = "note"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)
        Log.d("DetailActivity", "onCreate")

        applyInsets()
        bindViews()
        setupCategorySpinner()
        setupDateFieldWithPicker()

        operationId = intent.getIntExtra("operation_id", -1)

        if (savedInstanceState != null) {
            restoreState(savedInstanceState)
        } else {
            if (operationId != null && operationId!! > 0) {
                Log.d("DetailActivity", "load operation id=$operationId")
                loadOperation(operationId!!)
            }
        }

        buttonSave.setOnClickListener { saveOperation() }
        buttonDelete.setOnClickListener { deleteOperation() }
    }

    private fun applyInsets() {
        val root = findViewById<android.view.View>(R.id.root)

        val initialPaddingLeft = root.paddingLeft
        val initialPaddingTop = root.paddingTop
        val initialPaddingRight = root.paddingRight
        val initialPaddingBottom = root.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                initialPaddingLeft + systemBars.left,
                initialPaddingTop + systemBars.top,
                initialPaddingRight + systemBars.right,
                initialPaddingBottom + systemBars.bottom
            )
            insets
        }
    }

    private fun bindViews() {
        radioGroupType = findViewById(R.id.radioGroupType)
        radioButtonIncome = findViewById(R.id.radioButtonIncome)
        radioButtonExpense = findViewById(R.id.radioButtonExpense)
        editTextAmount = findViewById(R.id.editTextAmount)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        editTextDate = findViewById(R.id.editTextDate)
        editTextNote = findViewById(R.id.editTextNote)
        buttonSave = findViewById(R.id.buttonSave)
        buttonDelete = findViewById(R.id.buttonDelete)
    }

    private fun setupCategorySpinner() {
        val categories = arrayOf(
            "Еда", "Транспорт", "Развлечения", "Здоровье", "Одежда",
            "Другое", "Зарплата", "Подарки", "Инвестиции"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter
    }

    private fun setupDateFieldWithPicker() {
        // Запрещаем ручной ввод даты — только календарь
        editTextDate.isFocusable = false
        editTextDate.isClickable = true
        editTextDate.isLongClickable = false

        editTextDate.setOnClickListener {
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        // 1) Пытаемся открыть календарь на дате из поля (если она валидна)
        // 2) Если в поле мусор/пусто — открываем на сегодняшней
        val (year, month, day) = runCatching {
            val parts = editTextDate.text.toString().trim().split("-")
            val y = parts[0].toInt()
            val m = parts[1].toInt() - 1 // DatePickerDialog месяц 0..11
            val d = parts[2].toInt()
            // Дополнительно проверим, что это реальная дата
            LocalDate.of(y, m + 1, d)
            Triple(y, m, d)
        }.getOrElse {
            val c = Calendar.getInstance()
            Triple(
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
            )
        }

        DatePickerDialog(
            this,
            { _, y, m, d ->
                val mm = (m + 1).toString().padStart(2, '0')
                val dd = d.toString().padStart(2, '0')
                editTextDate.setText("$y-$mm-$dd") // строго yyyy-MM-dd
            },
            year,
            month,
            day
        ).show()
    }

    private fun loadOperation(id: Int) {
        Log.d("DetailActivity", "loadOperation id=$id")
        ApiClient.operationApi.getOperation(id).enqueue(object : Callback<Operation> {
            override fun onResponse(call: Call<Operation>, response: Response<Operation>) {
                Log.d(
                    "DetailActivity",
                    "getOperation onResponse code=${response.code()} body=${response.body()} error=${response.errorBody()?.string()}"
                )
                if (response.isSuccessful) {
                    currentOperation = response.body()
                    currentOperation?.let { displayOperation(it) }
                }
            }

            override fun onFailure(call: Call<Operation>, t: Throwable) {
                Log.e("DetailActivity", "getOperation onFailure", t)
            }
        })
    }

    private fun displayOperation(operation: Operation) {
        if (operation.type == "income") {
            radioButtonIncome.isChecked = true
        } else {
            radioButtonExpense.isChecked = true
        }

        editTextAmount.setText(formatAmount(operation.amount))

        val categoryPosition =
            (spinnerCategory.adapter as ArrayAdapter<String>).getPosition(operation.category)
        if (categoryPosition >= 0) spinnerCategory.setSelection(categoryPosition)

        editTextDate.setText(operation.date)
        editTextNote.setText(operation.note)
    }

    private fun saveOperation() {
        Log.d("DetailActivity", "saveOperation")

        val selectedRadioId = radioGroupType.checkedRadioButtonId
        if (selectedRadioId == -1) {
            Toast.makeText(this, "Выберите тип операции", Toast.LENGTH_SHORT).show()
            return
        }

        val type = if (selectedRadioId == R.id.radioButtonIncome) "income" else "expense"
        val amountText = editTextAmount.text.toString().trim()
        val category = spinnerCategory.selectedItem.toString()
        val date = editTextDate.text.toString().trim()
        val note = editTextNote.text.toString().trim()

        if (amountText.isEmpty()) {
            editTextAmount.error = "Введите сумму"
            return
        }

        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            editTextAmount.error = "Введите корректную сумму"
            return
        }

        if (date.isEmpty()) {
            editTextDate.error = "Введите дату"
            return
        }

        // Строгая валидация даты: если строка не парсится, не отправляем на сервер
        if (runCatching { LocalDate.parse(date) }.isFailure) {
            editTextDate.error = "Дата должна быть в формате YYYY-MM-DD"
            return
        }

        val operation = Operation(
            type = type,
            amount = amount,
            category = category,
            date = date,
            note = note
        )

        if (operationId != null && operationId!! > 0) {
            updateOperation(operationId!!, operation)
        } else {
            createOperation(operation)
        }
    }

    private fun createOperation(operation: Operation) {
        Log.d("DetailActivity", "createOperation $operation")
        ApiClient.operationApi.createOperation(operation).enqueue(object : Callback<Operation> {
            override fun onResponse(call: Call<Operation>, response: Response<Operation>) {
                Log.d(
                    "DetailActivity",
                    "createOperation onResponse code=${response.code()} body=${response.body()} error=${response.errorBody()?.string()}"
                )
                if (response.isSuccessful) {
                    finish()
                } else {
                    Toast.makeText(
                        this@DetailActivity,
                        "Ошибка при сохранении",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<Operation>, t: Throwable) {
                Log.e("DetailActivity", "createOperation onFailure", t)
                Toast.makeText(
                    this@DetailActivity,
                    "Ошибка сети: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun updateOperation(id: Int, operation: Operation) {
        Log.d("DetailActivity", "updateOperation id=$id op=$operation")
        ApiClient.operationApi.updateOperation(id, operation).enqueue(object : Callback<Operation> {
            override fun onResponse(call: Call<Operation>, response: Response<Operation>) {
                Log.d(
                    "DetailActivity",
                    "updateOperation onResponse code=${response.code()} body=${response.body()} error=${response.errorBody()?.string()}"
                )
                if (response.isSuccessful) {
                    finish()
                } else {
                    Toast.makeText(
                        this@DetailActivity,
                        "Ошибка при обновлении",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<Operation>, t: Throwable) {
                Log.e("DetailActivity", "updateOperation onFailure", t)
                Toast.makeText(
                    this@DetailActivity,
                    "Ошибка сети: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun deleteOperation() {
        if (operationId == null || operationId!! <= 0) return

        Log.d("DetailActivity", "deleteOperation id=$operationId")
        ApiClient.operationApi.deleteOperation(operationId!!).enqueue(object : Callback<Unit> {
            override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                Log.d(
                    "DetailActivity",
                    "deleteOperation onResponse code=${response.code()} error=${response.errorBody()?.string()}"
                )
                if (response.isSuccessful) {
                    finish()
                } else {
                    Toast.makeText(
                        this@DetailActivity,
                        "Ошибка при удалении",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<Unit>, t: Throwable) {
                Log.e("DetailActivity", "deleteOperation onFailure", t)
                Toast.makeText(
                    this@DetailActivity,
                    "Ошибка сети: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt(KEY_OPERATION_ID, operationId ?: -1)

        val selectedRadioId = radioGroupType.checkedRadioButtonId
        outState.putString(
            KEY_TYPE,
            when (selectedRadioId) {
                R.id.radioButtonIncome -> "income"
                R.id.radioButtonExpense -> "expense"
                else -> "income"
            }
        )

        outState.putString(KEY_AMOUNT, editTextAmount.text.toString())
        outState.putString(KEY_CATEGORY, spinnerCategory.selectedItem.toString())
        outState.putString(KEY_DATE, editTextDate.text.toString())
        outState.putString(KEY_NOTE, editTextNote.text.toString())
    }

    private fun restoreState(savedInstanceState: Bundle) {
        operationId = savedInstanceState.getInt(KEY_OPERATION_ID, -1)

        val type = savedInstanceState.getString(KEY_TYPE, "income")
        if (type == "income") radioButtonIncome.isChecked = true else radioButtonExpense.isChecked = true

        editTextAmount.setText(savedInstanceState.getString(KEY_AMOUNT, ""))

        val category = savedInstanceState.getString(KEY_CATEGORY, "")
        val categoryPosition =
            (spinnerCategory.adapter as ArrayAdapter<String>).getPosition(category)
        if (categoryPosition >= 0) spinnerCategory.setSelection(categoryPosition)

        editTextDate.setText(savedInstanceState.getString(KEY_DATE, ""))
        editTextNote.setText(savedInstanceState.getString(KEY_NOTE, ""))
    }

    private fun formatAmount(value: Double): String =
        BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
}
