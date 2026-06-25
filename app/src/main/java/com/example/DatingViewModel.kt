package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DatingViewModel(private val repository: DatingRepository) : ViewModel() {

    // --- State flows ---
    val otherProfiles: StateFlow<List<UserProfile>> = repository.otherProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUserProfile: StateFlow<UserProfile?> = repository.currentUserProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val matches: StateFlow<List<UserProfile>> = repository.matches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMessages: StateFlow<List<Message>> = repository.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val forumTopics: StateFlow<List<ForumTopic>> = repository.forumTopics
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Active Item states ---
    private val _activeChatUser = MutableStateFlow<UserProfile?>(null)
    val activeChatUser: StateFlow<UserProfile?> = _activeChatUser.asStateFlow()

    private val _activeTopic = MutableStateFlow<ForumTopic?>(null)
    val activeTopic: StateFlow<ForumTopic?> = _activeTopic.asStateFlow()

    private val _simulatedLocationZone = MutableStateFlow("Zona Centro")
    val simulatedLocationZone: StateFlow<String> = _simulatedLocationZone.asStateFlow()

    // --- Match Pop Up state ---
    private val _lastMatchEvent = MutableStateFlow<UserProfile?>(null)
    val lastMatchEvent: StateFlow<UserProfile?> = _lastMatchEvent.asStateFlow()

    // --- Dynamic screen-specific message stream ---
    val chatMessages: StateFlow<List<Message>> = _activeChatUser
        .flatMapLatest { user ->
            if (user != null) {
                repository.getMessagesForUser(user.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Dynamic screen-specific comments stream ---
    val activeTopicComments: StateFlow<List<ForumComment>> = _activeTopic
        .flatMapLatest { topic ->
            if (topic != null) {
                repository.getCommentsForTopic(topic.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setActiveChatUser(user: UserProfile?) {
        _activeChatUser.value = user
    }

    fun setActiveTopic(topic: ForumTopic?) {
        _activeTopic.value = topic
    }

    fun clearMatchEvent() {
        _lastMatchEvent.value = null
    }

    // --- Swipe/Match Actions ---
    fun likeProfile(profile: UserProfile, onMatchResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            // Mark as liked by the user
            val updated = profile.copy(hasLiked = true)
            repository.updateUserProfile(updated)

            // 70% chance of a mutual Match!
            val isMutualMatch = (1..100).random() <= 75
            if (isMutualMatch) {
                delay(400) // slight dramatic delay
                val matchedProfile = updated.copy(isMatched = true)
                repository.updateUserProfile(matchedProfile)
                _lastMatchEvent.value = matchedProfile
                onMatchResult(true)

                // Add an initial greeting from the matched person
                delay(1000)
                val greetings = listOf(
                    "¡Hola! Qué gusto hacer match con vos 😊 ¿Cómo estás?",
                    "¡Hola! Vi tu perfil y me pareció súper interesante. ¿De qué parte de la ciudad eres?",
                    "¡Hola! Qué bueno que hicimos match, ¿qué tal va tu día?"
                )
                repository.insertMessage(
                    Message(
                        senderId = matchedProfile.id,
                        receiverId = -1,
                        content = greetings.random()
                    )
                )
            } else {
                onMatchResult(false)
            }
        }
    }

    fun dislikeProfile(profile: UserProfile) {
        // Just hide it or simulate pass (we can mark hasLiked = false or just filter out in matching screen state)
        viewModelScope.launch(Dispatchers.IO) {
            // We can set distance to negative or some flag to skip, or just keep it as-is
            // For now, let's just skip it locally in UI list
        }
    }

    // --- Location Simulator ---
    fun changeSimulatedZone(zoneName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _simulatedLocationZone.value = zoneName
            // Dynamically vary distances to simulate movement!
            val currentProfiles = otherProfiles.value
            for (p in currentProfiles) {
                val delta = when (zoneName) {
                    "Zona Centro" -> (1..20).random() / 10.0
                    "Zona Norte" -> ((5..35).random() / 10.0)
                    "Zona Sur" -> ((15..65).random() / 10.0)
                    else -> (1..50).random() / 10.0
                }
                repository.updateUserProfile(p.copy(distance = delta))
            }
        }
    }

    // --- Chat Messages ---
    fun sendMessage(receiverId: Int, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Insert user message
            val userMsg = Message(senderId = -1, receiverId = receiverId, content = content)
            repository.insertMessage(userMsg)

            // 2. Trigger auto response from the recipient profile
            triggerAutoResponse(receiverId, content)
        }
    }

    private suspend fun triggerAutoResponse(userId: Int, userMsgContent: String) {
        delay(1500) // Wait 1.5 seconds for typing effect
        val senderProfile = repository.getUserProfileByIdSuspend(userId) ?: return
        val textLower = userMsgContent.lowercase()

        val replyContent = when (senderProfile.id) {
            4 -> { // Valentina
                if (textLower.contains("hola") || textLower.contains("buen")) {
                    "¡Hola! ¿Cómo estás? Contame, ¿qué tal tu día hoy? 😊"
                } else if (textLower.contains("música") || textLower.contains("banda") || textLower.contains("tocar")) {
                    "¡Totalmente! Tocar la guitarra acústica me relaja muchísimo. ¿Tocas algún instrumento o solo disfrutas escuchar?"
                } else if (textLower.contains("cine") || textLower.contains("película") || textLower.contains("director")) {
                    "Me encanta el cine de Wes Anderson, su paleta de colores es hermosa. ¿Cuál es tu director de cine favorito?"
                } else if (textLower.contains("salir") || textLower.contains("café") || textLower.contains("tomar")) {
                    "¡Me encantaría! Podríamos tomar algo y charlar de música. ¿Qué días sueles estar libre?"
                } else {
                    "¡Qué interesante lo que dices! Justo estaba escuchando un vinilo de rock nacional. ¿Qué planes tienes para el fin de semana?"
                }
            }
            6 -> { // Camila
                if (textLower.contains("hola") || textLower.contains("buen")) {
                    "¡Hola! Qué gusto saludarte. ¿Cómo va todo por allá?"
                } else if (textLower.contains("juego") || textLower.contains("mesa") || textLower.contains("catan")) {
                    "¡Sii! Amo las tardes de Catan o Carcassonne con amigos. ¡Soy bastante competitiva, te advierto! 😜 ¿Cuál es tu juego preferido?"
                } else if (textLower.contains("café") || textLower.contains("vino") || textLower.contains("tomar")) {
                    "¡Suena espectacular! Un buen café de especialidad nunca falla. Conozco un lugar hermoso cerca de Plaza Italia, ¿lo conoces?"
                } else if (textLower.contains("código") || textLower.contains("ingeniera") || textLower.contains("software")) {
                    "Trabajo en Python y Kotlin mayormente. ¡Es un mundo divertido! ¿A qué te dedicas vos?"
                } else {
                    "Me alegra mucho charlar con alguien con tan buena vibra y que viva tan cerca. ¡Deberíamos organizar algo pronto!"
                }
            }
            else -> { // Default generic replies for other profiles if matched
                if (textLower.contains("hola")) {
                    "¡Hola! Qué gusto saludarte, ¿cómo va tu semana?"
                } else if (textLower.contains("salir") || textLower.contains("tomar") || textLower.contains("café")) {
                    "¡Me parece genial la idea! organicemos y vamos a tomar algo tranqui."
                } else {
                    "¡Qué buena onda! Me encanta conversar con gente así. Cuéntame más de ti."
                }
            }
        }

        repository.insertMessage(
            Message(
                senderId = userId,
                receiverId = -1,
                content = replyContent
            )
        )
    }

    // --- Forums ---
    fun createTopic(title: String, category: String, description: String, authorName: String) {
        if (title.isBlank() || description.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val topic = ForumTopic(
                title = title,
                category = category,
                description = description,
                authorName = authorName,
                likes = 0
            )
            val newId = repository.insertForumTopic(topic)
            // Auto update active topic if on that view
            val insertedTopic = topic.copy(id = newId.toInt())
            _activeTopic.value = insertedTopic
        }
    }

    fun addComment(topicId: Int, authorName: String, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val comment = ForumComment(
                topicId = topicId,
                authorName = authorName,
                content = content
            )
            repository.insertForumComment(comment)
        }
    }

    fun likeTopic(topicId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.incrementTopicLikes(topicId)
            // Refresh active topic if it is the current one
            val current = _activeTopic.value
            if (current != null && current.id == topicId) {
                _activeTopic.value = current.copy(likes = current.likes + 1)
            }
        }
    }

    // --- User Profile Edit ---
    fun updateMyProfile(name: String, age: Int, bio: String, gender: String, imageUrl: String, tags: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentMyProfile = currentUserProfile.value
            if (currentMyProfile != null) {
                val updated = currentMyProfile.copy(
                    name = name,
                    age = age,
                    bio = bio,
                    gender = gender,
                    imageUrl = imageUrl,
                    tags = tags
                )
                repository.updateUserProfile(updated)
            }
        }
    }
}

class DatingViewModelFactory(private val repository: DatingRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DatingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DatingViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
