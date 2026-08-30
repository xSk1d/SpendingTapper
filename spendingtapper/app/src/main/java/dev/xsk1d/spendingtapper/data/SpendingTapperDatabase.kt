package dev.xsk1d.spendingtapper.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter
    fun kindToString(kind: Kind): String = kind.name

    @TypeConverter
    fun stringToKind(raw: String): Kind = runCatching { Kind.valueOf(raw) }.getOrDefault(Kind.NEED)
}

@Database(entities = [Expense::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class SpendingTapperDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao

    companion object {
        fun build(context: Context): SpendingTapperDatabase =
            Room.databaseBuilder(context, SpendingTapperDatabase::class.java, "spendingtapper.db").build()
    }
}
