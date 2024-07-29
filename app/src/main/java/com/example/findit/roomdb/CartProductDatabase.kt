package com.example.findit.roomdb

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CartProducts::class], version = 1, exportSchema = false)

abstract class CartProductDatabase : RoomDatabase() {

    abstract fun cartsProductsDao() : CartProductDao

    companion object{
        @Volatile
        var INSTANCE : CartProductDatabase ?= null

        fun getDatabaseInstance(context: Context) : CartProductDatabase{
            val tempInstance = INSTANCE
            if(tempInstance != null) return tempInstance

            synchronized(this){
                val roomdb = Room.databaseBuilder(context, CartProductDatabase::class.java, "CartProducts").allowMainThreadQueries().build()
                INSTANCE = roomdb
                return roomdb
            }
        }
    }

}