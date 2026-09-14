package com.strobingn.bowtune

import android.app.Application
import com.strobingn.bowtune.data.AppDatabase
import com.strobingn.bowtune.data.ChecklistStore

class BowTuneApp : Application() {
    lateinit var database: AppDatabase
        private set
    lateinit var checklistStore: ChecklistStore
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.get(this)
        checklistStore = ChecklistStore(this)
    }
}
