package com.safelink.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 검사 기록 로컬 DB (Task 7.1).
 *
 * 기기 내부의 앱 전용 저장소에만 만들어지며 다른 앱에서 접근할 수 없다.
 * 서버로 올리는 경로는 앱 어디에도 없다.
 */
@Database(
    entities = [DetectionRecordEntity::class, DiagnosisRecordEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(RecordConverters::class)
abstract class SafeLinkDatabase : RoomDatabase() {

    abstract fun detectionRecordDao(): DetectionRecordDao
    abstract fun diagnosisRecordDao(): DiagnosisRecordDao

    companion object {
        /**
         * v1 → v2: AI 보조분석 결과 칸 추가.
         * 기존 기록을 지우지 않도록 칸만 덧붙인다. 실기기에 쌓인 시연용 기록이 앱 업데이트로
         * 사라지면 안 되므로, 아래 fallbackToDestructiveMigration 에 맡기지 않고 경로를 직접 둔다.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE detection_records ADD COLUMN aiSummary TEXT")
                db.execSQL("ALTER TABLE detection_records ADD COLUMN aiDetectedPattern TEXT")
            }
        }

        /** v2 → v3: 링크 검사 판정 칸 추가. v1 → v2 와 같은 이유로 기존 기록을 지우지 않고 칸만 덧붙인다. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE detection_records ADD COLUMN linkResultsJson TEXT")
            }
        }

        @Volatile
        private var instance: SafeLinkDatabase? = null

        fun get(context: Context): SafeLinkDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                SafeLinkDatabase::class.java,
                "safelink.db"
            )
                // 스키마를 바꿀 때는 반드시 위처럼 마이그레이션을 추가한다.
                // 예전에는 fallbackToDestructiveMigration() 이 붙어 있어, 마이그레이션을 빠뜨리면
                // 사용자의 검사 기록이 조용히 전부 지워졌다(복구 불가). 지금은 그런 경우 개발 중에 바로 드러난다.
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                .also { instance = it }
        }
    }
}
