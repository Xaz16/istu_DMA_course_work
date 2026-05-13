package edu.istu.achipiga.coursework.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "saved_cities",
    indices = [Index(value = ["api_name"], unique = true)]
)
data class SavedCityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "api_name") val apiName: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int
)
