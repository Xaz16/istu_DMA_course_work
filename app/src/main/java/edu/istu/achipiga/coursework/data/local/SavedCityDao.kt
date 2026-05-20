package edu.istu.achipiga.coursework.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedCityDao {

    @Query("SELECT * FROM saved_cities ORDER BY sort_order ASC")
    fun observeSavedCities(): Flow<List<SavedCityEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(city: SavedCityEntity): Long

    @Query("DELETE FROM saved_cities WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM saved_cities WHERE LOWER(api_name) = LOWER(:name)")
    suspend fun countByNameIgnoreCase(name: String): Int

    @Query("SELECT COALESCE(MAX(sort_order), -1) FROM saved_cities")
    suspend fun maxSortOrder(): Int

    @Query("SELECT COUNT(*) FROM saved_cities")
    suspend fun count(): Int
}
