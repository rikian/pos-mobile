package com.gulali.dein.service

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.gulali.dein.R
import com.gulali.dein.databinding.StockHistoryBinding
import com.gulali.dein.helper.Helper
import com.gulali.dein.models.constants.Constants
import com.gulali.dein.models.viewmodels.ViewModelStockHistory
import com.gulali.dein.repositories.RepositoryHistoryStock
import org.koin.android.ext.android.inject

class StockHistory : AppCompatActivity() {
    private lateinit var binding: StockHistoryBinding
    private lateinit var tableLayout: TableLayout
    private lateinit var inflater: LayoutInflater

    // DI
    private val helper: Helper by inject()
    private val constant: Constants by inject()

    // repo
    private val repositoryHistoryStock: RepositoryHistoryStock by inject()

    // view model
    private val viewModelStockHistory: ViewModelStockHistory by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StockHistoryBinding.inflate(layoutInflater).also {
            binding = it
            setContentView(binding.root)
            tableLayout = binding.tbLayout
            inflater = LayoutInflater.from(this@StockHistory)
        }

        viewModelStockHistory.productID = intent.getIntExtra(constant.productID(), 0)
        if (viewModelStockHistory.productID == 0) return finish()
        viewModelStockHistory.stockHistory =
            repositoryHistoryStock.getHistoryStockById(viewModelStockHistory.productID).toMutableList()

        for (h in viewModelStockHistory.stockHistory) {
            val dateTime = helper.formatSpecificDate(helper.unixTimestampToDate(h.date.created))

            // Inflate the stock_history_item.xml layout
            val tableRow: TableRow = inflater.inflate(R.layout.stock_history_item, binding.root, false) as TableRow

            // Access TextViews in the inflated layout and set their values
            val date: TextView = tableRow.findViewById(R.id.date)
            val time: TextView = tableRow.findViewById(R.id.time)
            val trID: TextView = tableRow.findViewById(R.id.transactionID)
            val stockIn: TextView = tableRow.findViewById(R.id.stockIn)
            val sPurchase: TextView = tableRow.findViewById(R.id.s_purchase)
            val stockOut: TextView = tableRow.findViewById(R.id.stockOut)
            val remainStock: TextView = tableRow.findViewById(R.id.remain_stock)

            date.text = dateTime.date
            time.text = dateTime.time
            trID.text = if (h.transactionID == "") "-" else h.transactionID
            sPurchase.text = if (h.purchase == 0) "-" else "Rp ${helper.intToRupiah(h.purchase)}"
            stockIn.text = h.inStock.toString()
            stockOut.text = h.outStock.toString()
            remainStock.text = h.currentStock.toString()

            tableRow.setOnClickListener {
                if (h.transactionID == "") {
                    helper.generateTOA(this@StockHistory, h.info, true)
                } else {
                    helper.generateTOA(this@StockHistory, "move to transaction display", true)
                }
            }

            // Add the inflated layout to the TableLayout
            tableLayout.addView(tableRow)
        }
    }
}