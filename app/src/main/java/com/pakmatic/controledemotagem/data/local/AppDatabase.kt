package com.pakmatic.controledemotagem.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// <<< VERSÃO ATUALIZADA E NOVA ENTIDADE 'Fase' >>>
@Database(entities = [Apontamento::class, Impedimento::class, Fase::class], version = 3)
abstract class AppDatabase : RoomDatabase() {

    abstract fun apontamentoDao(): ApontamentoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "controle_montagem_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}