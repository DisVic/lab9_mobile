package com.example.financyapp.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.financyapp.api.ApiClient
import com.example.financyapp.model.Operation
import com.example.financyapp.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.time.LocalDate
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.math.BigDecimal

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var balanceTextView: TextView
    private lateinit var adapter: OperationAdapter
    private val operations = mutableListOf<Operation>()

    companion object {
        private const val CHANNEL_ID = "financy_channel"
        private const val KEY_OPERATIONS = "operations"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        createNotificationChannel()

        recyclerView = findViewById(R.id.recyclerViewOperations)
        balanceTextView = findViewById(R.id.textViewBalance)
        val fabAdd = findViewById<FloatingActionButton>(R.id.fabAdd)

        adapter = OperationAdapter(operations) { operation ->
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra("operation_id", operation.id)
            startActivity(intent)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        fabAdd.setOnClickListener {
            val intent = Intent(this, AddOperationActivity::class.java)
            startActivity(intent)
        }

        if (savedInstanceState != null) {
            val savedOperations = savedInstanceState.getParcelableArrayList<Operation>(KEY_OPERATIONS)
            if (savedOperations != null) {
                val sorted = sortOperations(savedOperations)
                operations.clear()
                operations.addAll(sorted)
                adapter.updateOperations(sorted)
                updateBalance()
            }
        } else {
            loadOperations()
        }
    }

    override fun onResume() {
        super.onResume()
        loadOperations()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Financy Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Уведомления о финансовых операциях"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun loadOperations() {
        Log.d("MainActivity", "loadOperations request")
        ApiClient.operationApi.getOperations().enqueue(object : Callback<List<Operation>> {
            override fun onResponse(
                call: Call<List<Operation>>,
                response: Response<List<Operation>>
            ) {
                Log.d("MainActivity", "loadOperations onResponse code=${response.code()} bodySize=${response.body()?.size} error=${response.errorBody()?.string()}")
                if (response.isSuccessful) {
                    val loadedOperations = sortOperations(response.body() ?: emptyList())
                    operations.clear()
                    operations.addAll(loadedOperations)
                    adapter.updateOperations(loadedOperations)
                    updateBalance()
                }
            }

            override fun onFailure(call: Call<List<Operation>>, t: Throwable) {
                Log.e("MainActivity", "loadOperations onFailure", t)
            }
        })
    }

    private fun updateBalance() {
        var balance = BigDecimal.ZERO
        operations.forEach { operation ->
            val amount = BigDecimal.valueOf(operation.amount)
            balance = if (operation.type == "income") balance.add(amount) else balance.subtract(amount)
        }
        balanceTextView.text = formatAmount(balance)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList(KEY_OPERATIONS, ArrayList(operations))
    }

    private fun formatAmount(value: BigDecimal): String =
        value.stripTrailingZeros().toPlainString()

    private fun sortOperations(list: List<Operation>): List<Operation> =
        list.sortedWith(
            compareByDescending<Operation> { LocalDate.parse(it.date) }
                .thenByDescending { it.id ?: 0 }
        )
}
