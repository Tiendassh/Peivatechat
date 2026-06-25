package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DatingDao {

    // --- User Profiles ---
    @Query("SELECT * FROM user_profiles WHERE isCurrentUser = 0 ORDER BY distance ASC")
    fun getOtherUserProfiles(): Flow<List<UserProfile>>

    @Query("SELECT * FROM user_profiles WHERE isCurrentUser = 1 LIMIT 1")
    fun getCurrentUserProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profiles WHERE id = :id")
    fun getUserProfileById(id: Int): Flow<UserProfile?>

    @Query("SELECT * FROM user_profiles WHERE id = :id")
    suspend fun getUserProfileByIdSuspend(id: Int): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfile)

    @Update
    suspend fun updateUserProfile(profile: UserProfile)

    @Query("SELECT * FROM user_profiles WHERE isMatched = 1 AND isCurrentUser = 0")
    fun getMatchedUsers(): Flow<List<UserProfile>>

    // --- Messages (Chats) ---
    @Query("SELECT * FROM messages WHERE (senderId = :userId AND receiverId = -1) OR (senderId = -1 AND receiverId = :userId) ORDER BY timestamp ASC")
    fun getMessagesForUser(userId: Int): Flow<List<Message>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<Message>>

    // --- Forums ---
    @Query("SELECT * FROM forum_topics ORDER BY timestamp DESC")
    fun getForumTopics(): Flow<List<ForumTopic>>

    @Query("SELECT * FROM forum_topics WHERE id = :topicId")
    fun getForumTopicById(topicId: Int): Flow<ForumTopic?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForumTopic(topic: ForumTopic): Long

    @Query("SELECT * FROM forum_comments WHERE topicId = :topicId ORDER BY timestamp ASC")
    fun getCommentsForTopic(topicId: Int): Flow<List<ForumComment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForumComment(comment: ForumComment)

    @Query("UPDATE forum_topics SET likes = likes + 1 WHERE id = :topicId")
    suspend fun incrementTopicLikes(topicId: Int)
}
