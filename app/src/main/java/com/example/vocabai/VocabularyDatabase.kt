package com.example.vocabai

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
import androidx.room.Transaction

@Entity(tableName = "vocabulary_books")
data class VocabularyBookEntity(
  @PrimaryKey val id: String,
  val title: String,
  val createdAt: Long,
  val updatedAt: Long,
)

@Entity(tableName = "word_entries")
data class WordEntryEntity(
  @PrimaryKey val id: String,
  val bookId: String,
  val english: String,
  val koreanMeaning: String,
  val englishMeaning: String,
  val memorized: Boolean,
)

@Dao
abstract class VocabularyDao {
  @Query("SELECT * FROM vocabulary_books ORDER BY createdAt DESC")
  abstract suspend fun books(): List<VocabularyBookEntity>

  @Query("SELECT * FROM word_entries ORDER BY id ASC")
  abstract suspend fun words(): List<WordEntryEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  abstract suspend fun insertBooks(books: List<VocabularyBookEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  abstract suspend fun insertWords(words: List<WordEntryEntity>)

  @Query("DELETE FROM vocabulary_books")
  abstract suspend fun clearBooks()

  @Query("DELETE FROM word_entries")
  abstract suspend fun clearWords()

  @Transaction
  open suspend fun replaceState(state: VocabularyUiState) {
    clearWords()
    clearBooks()
    insertBooks(state.books.map { it.toEntity() })
    insertWords(state.words.map { it.toEntity() })
  }
}

@Database(
  entities = [VocabularyBookEntity::class, WordEntryEntity::class],
  version = 1,
  exportSchema = false,
)
abstract class VocabularyDatabase : RoomDatabase() {
  abstract fun vocabularyDao(): VocabularyDao

  companion object {
    @Volatile private var instance: VocabularyDatabase? = null

    fun get(context: Context): VocabularyDatabase =
      instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder(
          context.applicationContext,
          VocabularyDatabase::class.java,
          "wordnote.db",
        ).build().also { instance = it }
      }
  }
}

fun VocabularyBookEntity.toModel(): VocabularyBook =
  VocabularyBook(id = id, title = title, createdAt = createdAt, updatedAt = updatedAt)

fun WordEntryEntity.toModel(): WordEntry =
  WordEntry(
    id = id,
    bookId = bookId,
    english = english,
    koreanMeaning = koreanMeaning,
    englishMeaning = englishMeaning,
    memorized = memorized,
  )

private fun VocabularyBook.toEntity(): VocabularyBookEntity =
  VocabularyBookEntity(id = id, title = title, createdAt = createdAt, updatedAt = updatedAt)

private fun WordEntry.toEntity(): WordEntryEntity =
  WordEntryEntity(
    id = id,
    bookId = bookId,
    english = english,
    koreanMeaning = koreanMeaning,
    englishMeaning = englishMeaning,
    memorized = memorized,
  )
