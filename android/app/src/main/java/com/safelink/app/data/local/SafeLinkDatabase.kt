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
    version = 2,
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

        @Volatile
        private var instance: SafeLinkDatabase? = null

        fun get(context: Context): SafeLinkDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                SafeLinkDatabase::class.java,
                "safelink.db"
            )
                // 개발 중 스키마 변경 시 기록을 유지할 필요가 없어 재생성으로 둔다.
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()
                .also { instance = it }
        }
    }
}
