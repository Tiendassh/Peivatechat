package com.example.data

import kotlinx.coroutines.flow.Flow

class DatingRepository(private val datingDao: DatingDao) {

    val otherProfiles: Flow<List<UserProfile>> = datingDao.getOtherUserProfiles()
    val currentUserProfile: Flow<UserProfile?> = datingDao.getCurrentUserProfile()
    val matches: Flow<List<UserProfile>> = datingDao.getMatchedUsers()
    val allMessages: Flow<List<Message>> = datingDao.getAllMessages()
    val forumTopics: Flow<List<ForumTopic>> = datingDao.getForumTopics()

    fun getMessagesForUser(userId: Int): Flow<List<Message>> = datingDao.getMessagesForUser(userId)

    fun getForumTopicById(topicId: Int): Flow<ForumTopic?> = datingDao.getForumTopicById(topicId)

    fun getCommentsForTopic(topicId: Int): Flow<List<ForumComment>> = datingDao.getCommentsForTopic(topicId)

    suspend fun getUserProfileByIdSuspend(id: Int): UserProfile? = datingDao.getUserProfileByIdSuspend(id)

    suspend fun insertMessage(message: Message) = datingDao.insertMessage(message)

    suspend fun insertUserProfile(profile: UserProfile) = datingDao.insertUserProfile(profile)

    suspend fun updateUserProfile(profile: UserProfile) = datingDao.updateUserProfile(profile)

    suspend fun insertForumTopic(topic: ForumTopic): Long = datingDao.insertForumTopic(topic)

    suspend fun insertForumComment(comment: ForumComment) = datingDao.insertForumComment(comment)

    suspend fun incrementTopicLikes(topicId: Int) = datingDao.incrementTopicLikes(topicId)
}
