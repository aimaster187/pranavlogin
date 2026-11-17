package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "cached_snapshots")
data class CachedSnapshotEntity(
    @PrimaryKey val cacheKey: String,
    val payloadJson: String,
    val updatedAtMs: Long
)

@Dao
interface PranavCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CachedSnapshotEntity)

    @Query("SELECT * FROM cached_snapshots WHERE cacheKey = :key LIMIT 1")
    suspend fun getByKey(key: String): CachedSnapshotEntity?

    @Query("DELETE FROM cached_snapshots")
    suspend fun clearAll()
}

@Database(entities = [CachedSnapshotEntity::class], version = 1, exportSchema = false)
abstract class PranavLocalDatabase : RoomDatabase() {
    abstract fun cacheDao(): PranavCacheDao

    companion object {
        @Volatile
        private var instance: PranavLocalDatabase? = null

        fun getInstance(context: Context): PranavLocalDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PranavLocalDatabase::class.java,
                    "pranav_local_cache.db"
                ).build().also { instance = it }
            }
        }
    }
}
