package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.InstallmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InstallmentDao {

    @Query("SELECT * FROM installments ORDER BY parcelNumber ASC")
    fun getAllInstallments(): Flow<List<InstallmentEntity>>

    @Query("SELECT * FROM installments WHERE parcelNumber = :parcelNumber LIMIT 1")
    suspend fun getInstallmentByNumber(parcelNumber: Int): InstallmentEntity?

    @Query("SELECT * FROM installments WHERE isPaid = 1 ORDER BY parcelNumber ASC")
    fun getPaidInstallments(): Flow<List<InstallmentEntity>>

    @Query("SELECT * FROM installments WHERE isPaid = 0 ORDER BY parcelNumber ASC")
    fun getPendingInstallments(): Flow<List<InstallmentEntity>>

    @Query("SELECT * FROM installments WHERE isPaid = 0 ORDER BY parcelNumber ASC LIMIT 1")
    fun getNextPendingInstallment(): Flow<InstallmentEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(installments: List<InstallmentEntity>)

    @Update
    suspend fun updateInstallment(installment: InstallmentEntity)

    @Query("DELETE FROM installments")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM installments WHERE isPaid = 1")
    fun getPaidCount(): Flow<Int>
}
