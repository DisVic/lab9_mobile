package com.example.financyapp.ui

import android.app.NotificationManager
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.financyapp.api.ApiClient
import com.example.financyapp.model.Operation
import com.example.financyapp.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class AddOperationActivity : AppCompatActivity() {
    private lateinit var radioGroupType: RadioGroup
    private lateinit var editTextAmount: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var editTextDate: EditText
    private lateinit var editTextNote: EditText
    private lateinit var buttonSave: Button

    companion object {
        private const val CHANNEL_ID = "financy_channel"
        private const val NOTIFICATION_ID = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_operation)

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

        radioGroupType = findViewById(R.id.radioGroupType)
        editTextAmount = findViewById(R.id.editTextAmount)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        editTextDate = findViewById(R.id.editTextDate)
        editTextNote = findViewById(R.id.editTextNote)
        buttonSave = findViewById(R.id.buttonSave)

        val categories = arrayOf("Еда", "Транспорт", "Развлечения", "Здоровье", "Одежда", "Другое", "Зарплата", "Подарки", "Инвестиции")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        editTextDate.setText(dateFormat.format(Date()))

        buttonSave.setOnClickListener {
            saveOperation()
        }
    }

    private fun saveOperation() {
        Log.d("AddOperation", "start saveOperation")
        val selectedRadioId = radioGroupType.checkedRadioButtonId
        val type = if (selectedRadioId == R.id.radioButtonIncome) "income" else "expense"
        val amountText = editTextAmount.text.toString()
        val category = spinnerCategory.selectedItem.toString()
        val date = editTextDate.text.toString()
        val note = editTextNote.text.toString()

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

        val operation = Operation(
            type = type,
            amount = amount,
            category = category,
            date = date,
            note = note
        )

        Log.d("AddOperation", "sending createOperation: $operation")
        ApiClient.operationApi.createOperation(operation).enqueue(object : Callback<Operation> {
            override fun onResponse(
                call: Call<Operation>,
                response: Response<Operation>
            ) {
                Log.d("AddOperation", "createOperation onResponse code=${response.code()} body=${response.body()} error=${response.errorBody()?.string()}")
                if (response.isSuccessful) {
                    showNotification("Операция успешно добавлена")
                    finish()
                } else {
                    Toast.makeText(this@AddOperationActivity, "Ошибка при сохранении", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Operation>, t: Throwable) {
                Log.e("AddOperation", "createOperation onFailure", t)
                Toast.makeText(this@AddOperationActivity, "Ошибка сети: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showNotification(message: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Финансовый трекер")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}

