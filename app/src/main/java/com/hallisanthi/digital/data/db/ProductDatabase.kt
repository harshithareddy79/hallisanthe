package com.hallisanthi.digital.data.db

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hallisanthi.digital.data.dao.*
import com.hallisanthi.digital.models.*

@Database(
    entities = [
        Product::class,
        User::class,
        ChatMessage::class,
        Order::class,
        AppNotification::class,
        Review::class,
        RecentlyViewed::class
    ],
    version = 8,
    exportSchema = false
)
abstract class ProductDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun userDao(): UserDao
    abstract fun chatDao(): ChatDao
    abstract fun orderDao(): OrderDao
    abstract fun notificationDao(): NotificationDao
    abstract fun reviewDao(): ReviewDao
    abstract fun recentlyViewedDao(): RecentlyViewedDao

    companion object {
        @Volatile private var INSTANCE: ProductDatabase? = null

        private val M12 = object : Migration(1, 2) { override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE products ADD COLUMN isAvailable INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE products ADD COLUMN wishlistCount INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE products ADD COLUMN description TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE products ADD COLUMN artisanName TEXT NOT NULL DEFAULT ''")
        }}
        private val M23 = object : Migration(2, 3) { override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE products ADD COLUMN rating REAL NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE products ADD COLUMN ratingCount INTEGER NOT NULL DEFAULT 0")
        }}
        private val M34 = object : Migration(3, 4) { override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL DEFAULT '', phone TEXT NOT NULL DEFAULT '', role TEXT NOT NULL DEFAULT 'BUYER', createdAt INTEGER NOT NULL DEFAULT 0)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_users_phone ON users(phone)")
        }}
        private val M45 = object : Migration(4, 5) { override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS users")
            db.execSQL("""CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL DEFAULT '', email TEXT NOT NULL DEFAULT '', phone TEXT NOT NULL DEFAULT '',
                passwordHash TEXT NOT NULL DEFAULT '', role TEXT NOT NULL DEFAULT 'BUYER',
                profileImagePath TEXT NOT NULL DEFAULT '', bio TEXT NOT NULL DEFAULT '',
                location TEXT NOT NULL DEFAULT '', isEmailVerified INTEGER NOT NULL DEFAULT 0,
                resetToken TEXT NOT NULL DEFAULT '', resetTokenExpiry INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL DEFAULT 0, lastLoginAt INTEGER NOT NULL DEFAULT 0)""")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_users_email ON users(email)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_users_phone ON users(phone)")
        }}
        private val M56 = object : Migration(5, 6) { override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""CREATE TABLE IF NOT EXISTS chat_messages (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                conversationId TEXT NOT NULL DEFAULT '', senderId INTEGER NOT NULL DEFAULT 0,
                receiverId INTEGER NOT NULL DEFAULT 0, productId INTEGER NOT NULL DEFAULT 0,
                productName TEXT NOT NULL DEFAULT '', message TEXT NOT NULL DEFAULT '',
                isRead INTEGER NOT NULL DEFAULT 0, timestamp INTEGER NOT NULL DEFAULT 0)""")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_chat_conv ON chat_messages(conversationId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_chat_receiver ON chat_messages(receiverId)")
        }}
        private val M67 = object : Migration(6, 7) { override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""CREATE TABLE IF NOT EXISTS orders (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                buyerId INTEGER NOT NULL DEFAULT 0, sellerId INTEGER NOT NULL DEFAULT 0,
                productId INTEGER NOT NULL DEFAULT 0, productName TEXT NOT NULL DEFAULT '',
                productImagePath TEXT NOT NULL DEFAULT '', price REAL NOT NULL DEFAULT 0,
                category TEXT NOT NULL DEFAULT '', artisanName TEXT NOT NULL DEFAULT '',
                quantity INTEGER NOT NULL DEFAULT 1, status TEXT NOT NULL DEFAULT 'CONTACTED',
                note TEXT NOT NULL DEFAULT '', createdAt INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0)""")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_orders_buyer ON orders(buyerId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_orders_seller ON orders(sellerId)")
            db.execSQL("""CREATE TABLE IF NOT EXISTS notifications (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                userId INTEGER NOT NULL DEFAULT 0, type TEXT NOT NULL DEFAULT 'SYSTEM',
                title TEXT NOT NULL DEFAULT '', body TEXT NOT NULL DEFAULT '',
                referenceId INTEGER NOT NULL DEFAULT 0, isRead INTEGER NOT NULL DEFAULT 0,
                timestamp INTEGER NOT NULL DEFAULT 0)""")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_notif_user ON notifications(userId)")
        }}
        private val M78 = object : Migration(7, 8) { override fun migrate(db: SupportSQLiteDatabase) {
            // Reviews table
            db.execSQL("""CREATE TABLE IF NOT EXISTS reviews (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                productId INTEGER NOT NULL DEFAULT 0,
                reviewerId INTEGER NOT NULL DEFAULT 0,
                reviewerName TEXT NOT NULL DEFAULT '',
                stars INTEGER NOT NULL DEFAULT 5,
                comment TEXT NOT NULL DEFAULT '',
                isVerifiedPurchase INTEGER NOT NULL DEFAULT 0,
                helpfulCount INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL DEFAULT 0)""")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_review_unique ON reviews(productId, reviewerId)")
            // Recently viewed table
            db.execSQL("""CREATE TABLE IF NOT EXISTS recently_viewed (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                userId INTEGER NOT NULL DEFAULT 0,
                productId INTEGER NOT NULL DEFAULT 0,
                productName TEXT NOT NULL DEFAULT '',
                productPrice REAL NOT NULL DEFAULT 0,
                productCategory TEXT NOT NULL DEFAULT '',
                productImagePath TEXT NOT NULL DEFAULT '',
                viewedAt INTEGER NOT NULL DEFAULT 0)""")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_rv_unique ON recently_viewed(userId, productId)")
        }}

        fun getDatabase(context: Context): ProductDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, ProductDatabase::class.java, "halli_santhe_db")
                    .addMigrations(M12, M23, M34, M45, M56, M67, M78)
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}
