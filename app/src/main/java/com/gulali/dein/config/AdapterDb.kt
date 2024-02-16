package com.gulali.dein.config

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.gulali.dein.contracts.dao.DaoCart
import com.gulali.dein.contracts.dao.DaoCategory
import com.gulali.dein.contracts.dao.DaoHistoryStock
import com.gulali.dein.contracts.dao.DaoOwner
import com.gulali.dein.contracts.dao.DaoProduct
import com.gulali.dein.contracts.dao.DaoTransaction
import com.gulali.dein.contracts.dao.DaoTransactionItem
import com.gulali.dein.contracts.dao.DaoUnit
import com.gulali.dein.models.entities.EntityCart
import com.gulali.dein.models.entities.EntityCategory
import com.gulali.dein.models.entities.EntityHistoryStock
import com.gulali.dein.models.entities.EntityOwner
import com.gulali.dein.models.entities.EntityProduct
import com.gulali.dein.models.entities.EntityTransaction
import com.gulali.dein.models.entities.EntityTransactionItem
import com.gulali.dein.models.entities.EntityUnit

@Database(
    entities = [
        EntityUnit::class,
        EntityCategory::class,
        EntityProduct::class,
        EntityHistoryStock::class,
        EntityTransaction::class,
        EntityTransactionItem::class,
        EntityCart::class,
        EntityOwner::class
    ],
    version = 1.0.toInt()
)
abstract class AdapterDb: RoomDatabase() {
    abstract fun daoUnit(): DaoUnit
    abstract fun daoCategory(): DaoCategory
    abstract fun daoProduct(): DaoProduct
    abstract fun daoHistoryStock(): DaoHistoryStock
    abstract fun daoTransaction(): DaoTransaction
    abstract fun daoTransactionItem(): DaoTransactionItem
    abstract fun daoCart(): DaoCart
    abstract fun daoOwner(): DaoOwner

    companion object {
        @Volatile
        private var INSTANCE: AdapterDb? = null

        fun initDB(ctx: Context): AdapterDb {
            val tmplInstant = INSTANCE
            if (tmplInstant != null) {
                return tmplInstant
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(ctx, AdapterDb::class.java, "gpos")
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                return instance
            }
        }
    }
}