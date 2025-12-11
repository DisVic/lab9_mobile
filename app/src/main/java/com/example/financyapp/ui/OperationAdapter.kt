package com.example.financyapp.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.financyapp.R
import com.example.financyapp.model.Operation
import java.math.BigDecimal

class OperationAdapter(
    private val operations: MutableList<Operation>,
    private val onItemClick: (Operation) -> Unit
) : RecyclerView.Adapter<OperationAdapter.OperationViewHolder>() {

    class OperationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val typeTextView: TextView = itemView.findViewById(R.id.textViewType)
        val amountTextView: TextView = itemView.findViewById(R.id.textViewAmount)
        val categoryTextView: TextView = itemView.findViewById(R.id.textViewCategory)
        val dateTextView: TextView = itemView.findViewById(R.id.textViewDate)
        val noteTextView: TextView = itemView.findViewById(R.id.textViewNote)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OperationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_operation, parent, false)
        return OperationViewHolder(view)
    }

    override fun onBindViewHolder(holder: OperationViewHolder, position: Int) {
        val operation = operations[position]
        holder.typeTextView.text = if (operation.type == "income") "Доход" else "Расход"
        holder.amountTextView.text =
            "${if (operation.type == "income") "+" else "-"}${formatAmount(operation.amount)}"
        holder.categoryTextView.text = operation.category
        holder.dateTextView.text = operation.date
        holder.noteTextView.text = operation.note

        holder.itemView.setOnClickListener {
            onItemClick(operation)
        }
    }

    override fun getItemCount(): Int = operations.size

    fun updateOperations(newOperations: List<Operation>) {
        operations.clear()
        operations.addAll(newOperations)
        notifyDataSetChanged()
    }

    fun removeOperation(position: Int) {
        operations.removeAt(position)
        notifyItemRemoved(position)
    }

    private fun formatAmount(value: Double): String =
        BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
}