package com.strobingn.bowtune.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BowSetupDao {
    @Query("SELECT * FROM bow_setups ORDER BY name ASC")
    fun observeAll(): Flow<List<BowSetup>>

    @Query("SELECT * FROM bow_setups WHERE id = :id")
    suspend fun getById(id: Long): BowSetup?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(setup: BowSetup): Long

    @Update
    suspend fun update(setup: BowSetup)

    @Delete
    suspend fun delete(setup: BowSetup)
}

@Dao
interface TuneSessionDao {
    @Query("SELECT * FROM tune_sessions ORDER BY dateEpochMs DESC")
    fun observeAll(): Flow<List<TuneSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: TuneSession): Long

    @Delete
    suspend fun delete(session: TuneSession)
}

@Dao
interface GuidedTuneSessionDao {
    @Query("SELECT * FROM guided_tune_sessions ORDER BY updatedAtEpochMs DESC")
    fun observeAll(): Flow<List<GuidedTuneSession>>

    @Query(
        "SELECT * FROM guided_tune_sessions WHERE protocolId = :protocolId AND status = 'IN_PROGRESS' ORDER BY updatedAtEpochMs DESC LIMIT 1"
    )
    suspend fun findInProgress(protocolId: String): GuidedTuneSession?

    @Query("SELECT * FROM guided_tune_sessions WHERE id = :id")
    suspend fun getById(id: Long): GuidedTuneSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: GuidedTuneSession): Long

    @Delete
    suspend fun delete(session: GuidedTuneSession)
}
