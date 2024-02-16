package com.gulali.dein

import android.app.Application
import com.gulali.dein.config.AdapterDb
import com.gulali.dein.config.Permission
import com.gulali.dein.helper.BarcodeScanner
import com.gulali.dein.helper.Helper
import com.gulali.dein.models.constants.Constants
import com.gulali.dein.models.viewmodels.ViewModelBluetoothUpdate
import com.gulali.dein.models.viewmodels.VIewModelHome
import com.gulali.dein.models.viewmodels.ViewModelCategory
import com.gulali.dein.models.viewmodels.ViewModelProductAdd
import com.gulali.dein.models.viewmodels.ViewModelProductDetail
import com.gulali.dein.models.viewmodels.ViewModelProductStock
import com.gulali.dein.models.viewmodels.ViewModelProductUpdate
import com.gulali.dein.models.viewmodels.ViewModelRegistration
import com.gulali.dein.models.viewmodels.ViewModelStockHistory
import com.gulali.dein.models.viewmodels.ViewModelTransaction
import com.gulali.dein.models.viewmodels.ViewModelTransactionDetail
import com.gulali.dein.models.viewmodels.ViewModelUnit
import com.gulali.dein.repositories.RepositoryCart
import com.gulali.dein.repositories.RepositoryCategory
import com.gulali.dein.repositories.RepositoryHistoryStock
import com.gulali.dein.repositories.RepositoryOwner
import com.gulali.dein.repositories.RepositoryProduct
import com.gulali.dein.repositories.RepositoryTransaction
import com.gulali.dein.repositories.RepositoryTransactionItem
import com.gulali.dein.repositories.RepositoryUnit
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

class Main : Application() {

    override fun onCreate() {
        super.onCreate()

        // Start Koin
        startKoin {
            androidContext(this@Main)
            modules(appModule)
        }
    }

    // Koin module
    private val appModule = module {
        single { AdapterDb.initDB(androidContext()) }
        single {
            RepositoryUnit(
                daoUnit= get<AdapterDb>().daoUnit()
            )
        }
        single {
            RepositoryCategory(
                daoCategory= get<AdapterDb>().daoCategory()
            )
        }
        single {
            RepositoryProduct(
                daoProduct= get<AdapterDb>().daoProduct(),
                daoHistoryStock= get<AdapterDb>().daoHistoryStock(),
            )
        }
        single {
            RepositoryHistoryStock(
                daoHistoryStock= get<AdapterDb>().daoHistoryStock()
            )
        }
        single {
            RepositoryTransaction(
                daoTransaction= get<AdapterDb>().daoTransaction(),
                daoHistoryStock= get<AdapterDb>().daoHistoryStock(),
                daoTransactionItem= get<AdapterDb>().daoTransactionItem(),
                daoProduct = get<AdapterDb>().daoProduct()
            )
        }
        single {
            RepositoryTransactionItem(
                daoTransactionItem= get<AdapterDb>().daoTransactionItem()
            )
        }
        single {
            RepositoryCart(
                daoCart= get<AdapterDb>().daoCart()
            )
        }
        single {
            RepositoryOwner(
                daoOwner = get<AdapterDb>().daoOwner()
            )
        }

        single { Helper() }
        single { Constants() }
        single { Permission(androidContext(), get()) }

        single { get<BarcodeScanner>().initBarcodeScanner(androidContext()) }

        viewModel { VIewModelHome() }
        viewModel { ViewModelUnit() }
        viewModel { ViewModelCategory() }
        viewModel { ViewModelProductAdd() }
        viewModel { ViewModelProductDetail() }
        viewModel { ViewModelProductStock() }
        viewModel { ViewModelProductUpdate() }
        viewModel { ViewModelStockHistory() }
        viewModel { ViewModelTransaction() }
        viewModel { ViewModelRegistration() }
        viewModel { ViewModelBluetoothUpdate() }
        viewModel { ViewModelTransactionDetail() }
    }
}