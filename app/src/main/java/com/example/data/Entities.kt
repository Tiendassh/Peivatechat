package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val age: Int,
    val bio: String,
    val gender: String,
    val distance: Double, // in kilometers
    val imageUrl: String,
    val isMatched: Boolean = false,
    val hasLiked: Boolean = false,
    val isCurrentUser: Boolean = false,
    val tags: String = "" // Comma-separated interest tags
)

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val senderId: Int, // id of sender, or -1 for current user
    val receiverId: Int, // id of receiver
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "forum_topics")
data class ForumTopic(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val category: String, // e.g., "Citas", "Consejos", "Locales"
    val description: String,
    val authorName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val likes: Int = 0
)

@Entity(tableName = "forum_comments")
data class ForumComment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val topicId: Int,
    val authorName: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
