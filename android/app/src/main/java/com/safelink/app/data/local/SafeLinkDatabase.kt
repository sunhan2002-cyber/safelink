package com.safelink.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * 검사 기록 로컬 DB (Task 7.1).
 *
 * 기기 내부의 앱 전용 저장소에만 만들어지며 다른 앱에서 접근할 수 없다.
 * 서버로 올리는 경로는 앱 어디에도 없다.
 */
@Database(
    entities = [DetectionRecordEntity::class, DiagnosisRecordEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(RecordConverters::class)
abstract class SafeLinkDatabase : RoomDatabase() {

    abstract fun detectionRecordDao(): DetectionRecordDao
    abstract fun diagnosisRecordDao(): DiagnosisRecordDao

    companion object {
        @Volatile
        private var instance: SafeLinkDatabase? = null

        fun get(context: Context): SafeLinkDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                SafeLinkDatabase::class.java,
                "safelink.db"
            )
                // 개발 중 스키마 변경 시 기록을 유지할 필요가 없어 재생성으로 둔다.
                .fallbackToDestructiveMigration()
                .build()
                .also { instance = it }
        }
    }
}
