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

    @Query("SELECT * FROM bow_setups")
    suspend fun getAll(): List<BowSetup>

    @Query("SELECT * FROM bow_setups WHERE id = :id")
    suspend fun getById(id: Long): BowSetup?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(setup: BowSetup): Long

    @Update
    suspend fun update(setup: BowSetup)

    @Delete
    suspend fun delete(setup: BowSetup)

    @Query("DELETE FROM bow_setups")
    suspend fun deleteAll()
}

@Dao
interface TuneSessionDao {
    @Query("SELECT * FROM tune_sessions ORDER BY dateEpochMs DESC")
    fun observeAll(): Flow<List<TuneSession>>

    @Query("SELECT * FROM tune_sessions ORDER BY dateEpochMs DESC")
    suspend fun getAll(): List<TuneSession>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: TuneSession): Long

    @Delete
    suspend fun delete(session: TuneSession)

    @Query("DELETE FROM tune_sessions")
    suspend fun deleteAll()
}

@Dao
interface GuidedTuneSessionDao {
    @Query("SELECT * FROM guided_tune_sessions ORDER BY updatedAtEpochMs DESC")
    fun observeAll(): Flow<List<GuidedTuneSession>>

    @Query("SELECT * FROM guided_tune_sessions")
    suspend fun getAll(): List<GuidedTuneSession>

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

    @Query("DELETE FROM guided_tune_sessions")
    suspend fun deleteAll()
}

@Dao
interface ArrowShaftDao {
    @Query("SELECT * FROM arrow_shafts ORDER BY name ASC")
    fun observeAll(): Flow<List<ArrowShaft>>

    @Query("SELECT * FROM arrow_shafts")
    suspend fun getAll(): List<ArrowShaft>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(arrow: ArrowShaft): Long

    @Delete
    suspend fun delete(arrow: ArrowShaft)

    @Query("DELETE FROM arrow_shafts")
    suspend fun deleteAll()
}

@Dao
interface PaperTearLogDao {
    @Query("SELECT * FROM paper_tear_logs ORDER BY dateEpochMs DESC")
    fun observeAll(): Flow<List<PaperTearLog>>

    @Query("SELECT * FROM paper_tear_logs")
    suspend fun getAll(): List<PaperTearLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: PaperTearLog): Long

    @Delete
    suspend fun delete(log: PaperTearLog)

    @Query("DELETE FROM paper_tear_logs")
    suspend fun deleteAll()
}

@Dao
interface AdjustmentLogDao {
    @Query("SELECT * FROM adjustment_logs ORDER BY dateEpochMs DESC")
    fun observeAll(): Flow<List<AdjustmentLog>>

    @Query("SELECT * FROM adjustment_logs")
    suspend fun getAll(): List<AdjustmentLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: AdjustmentLog): Long

    @Delete
    suspend fun delete(log: AdjustmentLog)

    @Query("DELETE FROM adjustment_logs")
    suspend fun deleteAll()
}

@Dao
interface MaintenanceLogDao {
    @Query("SELECT * FROM maintenance_logs ORDER BY dateEpochMs DESC")
    fun observeAll(): Flow<List<MaintenanceLog>>

    @Query("SELECT * FROM maintenance_logs")
    suspend fun getAll(): List<MaintenanceLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: MaintenanceLog): Long

    @Delete
    suspend fun delete(log: MaintenanceLog)

    @Query("DELETE FROM maintenance_logs")
    suspend fun deleteAll()
}
