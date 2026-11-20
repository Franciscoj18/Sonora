package com.sonora.player.data

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
import androidx.room.Update
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "custom_albums")
data class CustomAlbumEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val artist: String,
    val year: Int,
    val coverPath: String?,
    val createdAt: Long,
)

/** Cambios que el usuario hace a una canción sin tocar el archivo original. */
@Entity(tableName = "song_overrides")
data class SongOverrideEntity(
    @PrimaryKey val songId: Long,
    val title: String? = null,
    val artist: String? = null,
    val customAlbumId: Long? = null,
    val track: Int? = null,
)

/** Carátula elegida para un álbum que ya existía en el teléfono. */
@Entity(tableName = "album_covers")
data class AlbumCoverEntity(
    @PrimaryKey val albumKey: String,
    val coverPath: String,
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val coverPath: String?,
    val createdAt: Long,
)

@Entity(tableName = "playlist_songs", primaryKeys = ["playlistId", "songId"])
data class PlaylistSongEntity(
    val playlistId: Long,
    val songId: Long,
    val position: Int,
)

/**
 * Letra guardada para una canción. `source` dice de dónde salió:
 * "lrclib" (internet), "file" (dentro del MP3), "user" (pegada o importada por ti)
 * o "removed" (quitada a propósito: no se vuelve a buscar sola).
 */
@Entity(tableName = "lyrics")
data class LyricsEntity(
    @PrimaryKey val songId: Long,
    val plain: String?,
    val synced: String?,
    val source: String,
    val instrumental: Boolean,
    val updatedAt: Long,
)

@Dao
interface MusicDao {
    @Query("SELECT * FROM custom_albums ORDER BY createdAt DESC")
    fun customAlbums(): Flow<List<CustomAlbumEntity>>

    @Query("SELECT * FROM song_overrides")
    fun overrides(): Flow<List<SongOverrideEntity>>

    @Query("SELECT * FROM album_covers")
    fun albumCovers(): Flow<List<AlbumCoverEntity>>

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun playlists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlist_songs ORDER BY position ASC")
    fun playlistSongs(): Flow<List<PlaylistSongEntity>>

    // Álbumes propios
    @Insert
    suspend fun insertCustomAlbum(album: CustomAlbumEntity): Long

    @Update
    suspend fun updateCustomAlbum(album: CustomAlbumEntity)

    @Query("SELECT * FROM custom_albums WHERE id = :id")
    suspend fun customAlbum(id: Long): CustomAlbumEntity?

    @Query("DELETE FROM custom_albums WHERE id = :id")
    suspend fun deleteCustomAlbum(id: Long)

    @Query("UPDATE song_overrides SET customAlbumId = NULL, track = NULL WHERE customAlbumId = :id")
    suspend fun clearAlbumFromSongs(id: Long)

    @Query("SELECT COALESCE(MAX(track), 0) FROM song_overrides WHERE customAlbumId = :id")
    suspend fun maxTrack(id: Long): Int

    // Canciones
    @Query("SELECT * FROM song_overrides WHERE songId = :id")
    suspend fun override(id: Long): SongOverrideEntity?

    @Upsert
    suspend fun upsertOverride(item: SongOverrideEntity)

    // Carátulas de álbumes existentes
    @Query("SELECT * FROM album_covers WHERE albumKey = :key")
    suspend fun albumCover(key: String): AlbumCoverEntity?

    @Upsert
    suspend fun upsertAlbumCover(item: AlbumCoverEntity)

    @Query("DELETE FROM album_covers WHERE albumKey = :key")
    suspend fun deleteAlbumCover(key: String)

    // Playlists
    @Insert
    suspend fun insertPlaylist(item: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(item: PlaylistEntity)

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun playlist(id: Long): PlaylistEntity?

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :id")
    suspend fun clearPlaylist(id: Long)

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_songs WHERE playlistId = :id")
    suspend fun maxPosition(id: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistSongs(items: List<PlaylistSongEntity>)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeFromPlaylist(playlistId: Long, songId: Long)

    // Letras
    @Query("SELECT * FROM lyrics WHERE songId = :songId")
    suspend fun lyrics(songId: Long): LyricsEntity?

    @Upsert
    suspend fun upsertLyrics(item: LyricsEntity)
}

@Database(
    entities = [
        CustomAlbumEntity::class,
        SongOverrideEntity::class,
        AlbumCoverEntity::class,
        PlaylistEntity::class,
        PlaylistSongEntity::class,
        LyricsEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class SonoraDatabase : RoomDatabase() {
    abstract fun dao(): MusicDao

    companion object {
        @Volatile
        private var instance: SonoraDatabase? = null

        /** Agrega la tabla de letras sin borrar tus álbumes, playlists ni carátulas. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `lyrics` (" +
                        "`songId` INTEGER NOT NULL, `plain` TEXT, `synced` TEXT, " +
                        "`source` TEXT NOT NULL, `instrumental` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, PRIMARY KEY(`songId`))"
                )
            }
        }

        fun get(context: Context): SonoraDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SonoraDatabase::class.java,
                    "sonora.db"
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
