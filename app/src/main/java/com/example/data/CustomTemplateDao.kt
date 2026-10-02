package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomTemplateDao {

    @Query("SELECT * FROM custom_templates ORDER BY createdAtMillis DESC")
    fun getAllCustomTemplates(): Flow<List<CustomTemplateEntity>>

    @Query("SELECT * FROM custom_templates WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CustomTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: CustomTemplateEntity): Long

    @Update
    suspend fun update(template: CustomTemplateEntity)

    @Delete
    suspend fun delete(template: CustomTemplateEntity)

    @Query("DELETE FROM custom_templates WHERE id = :id")
    suspend fun deleteById(id: Long)
}
