package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.DatingViewModel
import com.example.data.ForumComment
import com.example.data.ForumTopic
import com.example.data.Message
import com.example.data.UserProfile
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class MainTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    CITAS("Citas", Icons.Default.Favorite),
    CHATS("Chats", Icons.Default.Chat),
    FOROS("Debates", Icons.Default.Forum),
    PERFIL("Mi Perfil", Icons.Default.Person)
}

enum class NavigationState {
    TABS,
    CHAT_DETAIL,
    FORUM_DETAIL,
    CREATE_TOPIC
}

@Composable
fun DatingAppScreen(viewModel: DatingViewModel) {
    var currentTab by remember { mutableStateOf(MainTab.CITAS) }
    var navState by remember { mutableStateOf(NavigationState.TABS) }

    val activeChatUser by viewModel.activeChatUser.collectAsStateWithLifecycle()
    val activeTopic by viewModel.activeTopic.collectAsStateWithLifecycle()
    val lastMatchEvent by viewModel.lastMatchEvent.collectAsStateWithLifecycle()

    // Handle incoming Match Pop Up Dialog
    if (lastMatchEvent != null) {
        MatchCelebrationDialog(
            matchedUser = lastMatchEvent!!,
            onClose = { viewModel.clearMatchEvent() },
            onSendMessage = {
                val user = lastMatchEvent!!
                viewModel.clearMatchEvent()
                viewModel.setActiveChatUser(user)
                currentTab = MainTab.CHATS
                navState = NavigationState.CHAT_DETAIL
            }
        )
    }

    Scaffold(
        bottomBar = {
            if (navState == NavigationState.TABS) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    MainTab.values().forEach { tab ->
                        NavigationBarItem(
                            selected = currentTab == tab,
                            onClick = { currentTab = tab },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label, fontSize = 12.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CoralPrimary,
                                selectedTextColor = CoralPrimary,
                                indicatorColor = CoralPrimary.copy(alpha = 0.15f),
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            when (navState) {
                NavigationState.TABS -> {
                    when (currentTab) {
                        MainTab.CITAS -> DiscoverScreen(
                            viewModel = viewModel,
                            onGoToChats = {
                                currentTab = MainTab.CHATS
                            },
                            onGoToProfile = {
                                currentTab = MainTab.PERFIL
                            }
                        )
                        MainTab.CHATS -> ChatsScreen(
                            viewModel = viewModel,
                            onOpenChat = { user ->
                                viewModel.setActiveChatUser(user)
                                navState = NavigationState.CHAT_DETAIL
                            }
                        )
                        MainTab.FOROS -> ForumsScreen(
                            viewModel = viewModel,
                            onOpenTopic = { topic ->
                                viewModel.setActiveTopic(topic)
                                navState = NavigationState.FORUM_DETAIL
                            },
                            onCreateTopic = {
                                navState = NavigationState.CREATE_TOPIC
                            }
                        )
                        MainTab.PERFIL -> ProfileScreen(viewModel = viewModel)
                    }
                }
                NavigationState.CHAT_DETAIL -> {
                    activeChatUser?.let { user ->
                        ChatDetailScreen(
                            user = user,
                            viewModel = viewModel,
                            onBack = {
                                viewModel.setActiveChatUser(null)
                                navState = NavigationState.TABS
                            }
                        )
                    } ?: run {
                        navState = NavigationState.TABS
                    }
                }
                NavigationState.FORUM_DETAIL -> {
                    activeTopic?.let { topic ->
                        ForumDetailScreen(
                            topic = topic,
                            viewModel = viewModel,
                            onBack = {
                                viewModel.setActiveTopic(null)
                                navState = NavigationState.TABS
                            }
                        )
                    } ?: run {
                        navState = NavigationState.TABS
                    }
                }
                NavigationState.CREATE_TOPIC -> {
                    CreateTopicScreen(
                        viewModel = viewModel,
                        onBack = {
                            navState = NavigationState.TABS
                        }
                    )
                }
            }
        }
    }
}

// ==========================================
// 1. DISCOVER / SWIPE SCREEN (CITAS)
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiscoverScreen(
    viewModel: DatingViewModel,
    onGoToChats: () -> Unit,
    onGoToProfile: () -> Unit
) {
    val otherProfiles by viewModel.otherProfiles.collectAsStateWithLifecycle()
    val zoneName by viewModel.simulatedLocationZone.collectAsStateWithLifecycle()

    // Filter profiles that are not liked yet and not matched
    val discoverList = remember(otherProfiles) {
        otherProfiles.filter { !it.hasLiked && !it.isMatched }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top branding & info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CercaMatch",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = CoralPrimary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Ubicación",
                        tint = CoralSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Cercanos en $zoneName",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Badge(
                containerColor = CoralPrimary.copy(alpha = 0.2f),
                contentColor = CoralPrimary,
                modifier = Modifier.padding(4.dp)
            ) {
                Text(
                    text = "Gratis",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (discoverList.isEmpty()) {
            // Empty State
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(CardSurface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationSearching,
                            contentDescription = "Buscando",
                            tint = CoralSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "¡Has visto a todos en esta zona!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No quedan más perfiles disponibles en $zoneName. ¡Abre 'Mi Perfil' para simular otra zona de la ciudad y encontrar más personas!",
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onGoToProfile,
                        colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary)
                    ) {
                        Icon(Icons.Default.Map, contentDescription = "Simular ubicación")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simular Otra Ubicación")
                    }
                }
            }
        } else {
            // Cards Swiper Simulation
            val topProfile = discoverList.first()

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, CardSurfaceVariant, RoundedCornerShape(24.dp))
                    .background(CardSurface)
            ) {
                // Profile Image
                AsyncImage(
                    model = topProfile.imageUrl,
                    contentDescription = topProfile.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Dark Bottom Gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                startY = 300f
                            )
                        )
                )

                // Profile Info
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "${topProfile.name}, ${topProfile.age}",
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Gender icon or marker
                        val genderIcon = if (topProfile.gender == "Mujer") Icons.Default.Person else Icons.Default.Person
                        Icon(
                            imageVector = genderIcon,
                            contentDescription = topProfile.gender,
                            tint = RoseTertiary,
                            modifier = Modifier.size(20.dp).padding(bottom = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Distancia",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "A ${topProfile.distance} km de ti",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = topProfile.bio,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (topProfile.tags.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val profileTags = topProfile.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        FlowRow(
                            horizontalArrangement = Arrangement.Start,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            profileTags.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .padding(end = 6.dp)
                                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(100.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "#$tag",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pass button
                IconButton(
                    onClick = { viewModel.dislikeProfile(topProfile) },
                    modifier = Modifier
                        .size(64.dp)
                        .background(CardSurface, CircleShape)
                        .border(1.dp, CloseGrey.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Pasar",
                        tint = CloseGrey,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Like button
                IconButton(
                    onClick = { viewModel.likeProfile(topProfile) },
                    modifier = Modifier
                        .size(72.dp)
                        .background(CoralPrimary, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Me gusta",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

// Celebration / Match Dialog
@Composable
fun MatchCelebrationDialog(
    matchedUser: UserProfile,
    onClose: () -> Unit,
    onSendMessage: () -> Unit
) {
    Dialog(onDismissRequest = onClose) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "¡Es un Match! 🎉",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = CoralSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Tú y ${matchedUser.name} se han gustado mutuamente.",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Avatar overlay
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Current user avatar placeholder
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .border(3.dp, CoralPrimary, CircleShape)
                    ) {
                        AsyncImage(
                            model = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=300&q=80",
                            contentDescription = "Tú",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Love",
                        tint = HeartRed,
                        modifier = Modifier
                            .size(36.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .border(3.dp, CoralPrimary, CircleShape)
                    ) {
                        AsyncImage(
                            model = matchedUser.imageUrl,
                            contentDescription = matchedUser.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = onSendMessage,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary)
                ) {
                    Icon(imageVector = Icons.Default.Chat, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enviar Mensaje Gratis")
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Seguir buscando", color = TextSecondary)
                }
            }
        }
    }
}


// ==========================================
// 2. CHATS LIST SCREEN (CHATS)
// ==========================================
@Composable
fun ChatsScreen(
    viewModel: DatingViewModel,
    onOpenChat: (UserProfile) -> Unit
) {
    val matches by viewModel.matches.collectAsStateWithLifecycle()
    val allMessages by viewModel.allMessages.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Mis Mensajes",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = CoralPrimary
        )
        Text(
            text = "Conversaciones gratis con personas cercanas por ubicación",
            color = TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (matches.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = "Sin matches",
                        tint = TextMuted,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Sin chats todavía",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Desliza en 'Citas' y haz match para empezar a chatear gratis.",
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            // Horizontal list: New matches
            Text(
                text = "Nuevas Conexiones",
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(matches) { match ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onOpenChat(match) }
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .border(2.dp, CoralPrimary, CircleShape)
                        ) {
                            AsyncImage(
                                model = match.imageUrl,
                                contentDescription = match.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = match.name,
                            fontSize = 12.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Chats Recientes",
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Vertical list of chats
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(matches) { match ->
                    val userMessages = allMessages.filter {
                        (it.senderId == match.id && it.receiverId == -1) || (it.senderId == -1 && it.receiverId == match.id)
                    }
                    val lastMessage = userMessages.lastOrNull()

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenChat(match) },
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                            ) {
                                AsyncImage(
                                    model = match.imageUrl,
                                    contentDescription = match.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = match.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = TextPrimary
                                    )
                                    // Distance marker in Chats list
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = TextMuted,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "${match.distance} km",
                                            fontSize = 11.sp,
                                            color = TextMuted,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = lastMessage?.content ?: "¡Hiciste match! Saluda gratis...",
                                    fontSize = 13.sp,
                                    color = if (lastMessage != null) TextSecondary else CoralSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 2B. CHAT DETAIL SCREEN
// ==========================================
@Composable
fun ChatDetailScreen(
    user: UserProfile,
    viewModel: DatingViewModel,
    onBack: () -> Unit
) {
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    var textState by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Automatically scroll to the last message when a new message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Typing simulation state
    var isPartnerTyping by remember { mutableStateOf(false) }
    LaunchedEffect(messages) {
        if (messages.isNotEmpty() && messages.last().senderId == -1) {
            isPartnerTyping = true
            delay(1200) // typing effect delay
            isPartnerTyping = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBackground)
    ) {
        // Chat Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                ) {
                    AsyncImage(
                        model = user.imageUrl,
                        contentDescription = user.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Cercano(a) • a ${user.distance} km de ti",
                        color = CoralSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Badge(
                    containerColor = MatchGreen.copy(alpha = 0.2f),
                    contentColor = MatchGreen
                ) {
                    Text(
                        text = "Activo",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
        ) {
            items(messages) { message ->
                val isMe = message.senderId == -1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .background(
                                color = if (isMe) CoralPrimary else CardSurface,
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isMe) 16.dp else 0.dp,
                                    bottomEnd = if (isMe) 0.dp else 16.dp
                                )
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = message.content,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            if (isPartnerTyping) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CardSurface.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${user.name} está escribiendo...",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Bottom input bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Quick answers row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val suggestions = when (user.id) {
                        4 -> listOf("¿Qué música tocas?", "¿Tomamos café?", "¡Tame Impala es genial!")
                        6 -> listOf("¿Jugamos Catan?", "¡Conozco Plaza Italia!", "¿De qué trabajas?")
                        else -> listOf("¡Hola!", "¿Cómo estás?", "¿Salimos a tomar café?")
                    }
                    suggestions.forEach { label ->
                        SuggestionChip(
                            onClick = {
                                textState = label
                            },
                            label = { Text(label, fontSize = 11.sp, color = Color.White) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = CardSurfaceVariant
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = textState,
                        onValueChange = { textState = it },
                        placeholder = { Text("Escribe un mensaje gratis...", color = TextMuted) },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp)),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = CardSurfaceVariant,
                            unfocusedContainerColor = CardSurfaceVariant,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (textState.isNotBlank()) {
                                    viewModel.sendMessage(user.id, textState)
                                    textState = ""
                                    keyboardController?.hide()
                                }
                            }
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (textState.isNotBlank()) {
                                viewModel.sendMessage(user.id, textState)
                                textState = ""
                                keyboardController?.hide()
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(CoralPrimary, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Enviar",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. DEBATE FORUMS SCREEN (FOROS)
// ==========================================
@Composable
fun ForumsScreen(
    viewModel: DatingViewModel,
    onOpenTopic: (ForumTopic) -> Unit,
    onCreateTopic: () -> Unit
) {
    val forumTopics by viewModel.forumTopics.collectAsStateWithLifecycle()
    var selectedCategory by remember { mutableStateOf("Todos") }
    val categories = listOf("Todos", "Debates", "Consejos", "Locales")

    val filteredTopics = remember(forumTopics, selectedCategory) {
        if (selectedCategory == "Todos") {
            forumTopics
        } else {
            forumTopics.filter { it.category == selectedCategory }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateTopic,
                containerColor = CoralPrimary,
                contentColor = Color.White
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Crear Debate")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Foros de Debate",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = CoralPrimary
            )
            Text(
                text = "Debate, comparte consejos e interactúa con solteros locales sin costo de hosting",
                color = TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Category Chips Selection
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    val isSelected = selectedCategory == category
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isSelected) CoralPrimary else CardSurface,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = category,
                            color = if (isSelected) Color.White else TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            if (filteredTopics.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay debates en esta categoría aún.\n¡Sé el primero en iniciar un debate!",
                        textAlign = TextAlign.Center,
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredTopics) { topic ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenTopic(topic) },
                            colors = CardDefaults.cardColors(containerColor = CardSurface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, CardSurfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Badge(
                                        containerColor = CoralSecondary.copy(alpha = 0.2f),
                                        contentColor = CoralSecondary
                                    ) {
                                        Text(
                                            text = topic.category,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }

                                    Text(
                                        text = "Iniciado por ${topic.authorName}",
                                        fontSize = 11.sp,
                                        color = TextMuted,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = topic.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = TextPrimary
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = topic.description,
                                    fontSize = 13.sp,
                                    color = TextSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Favorite,
                                            contentDescription = "Likes",
                                            tint = CoralSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${topic.likes} votos",
                                            fontSize = 12.sp,
                                            color = TextSecondary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Text(
                                        text = "Ver respuestas →",
                                        fontSize = 12.sp,
                                        color = CoralPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3B. FORUM DETAIL & DISCUSSION SCREEN
// ==========================================
@Composable
fun ForumDetailScreen(
    topic: ForumTopic,
    viewModel: DatingViewModel,
    onBack: () -> Unit
) {
    val comments by viewModel.activeTopicComments.collectAsStateWithLifecycle()
    var commentText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBackground)
    ) {
        // Topic Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .statusBarsPadding()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = TextPrimary
                        )
                    }
                    Text(
                        text = "Foros > ${topic.category}",
                        fontSize = 13.sp,
                        color = CoralSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = topic.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Iniciado por ${topic.authorName}",
                        fontSize = 12.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Medium
                    )

                    Button(
                        onClick = { viewModel.likeTopic(topic.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary.copy(alpha = 0.15f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Me gusta",
                            tint = CoralPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${topic.likes} Votos",
                            fontSize = 12.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = topic.description,
                    fontSize = 15.sp,
                    color = TextSecondary,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Comments Area
        Text(
            text = "Debate local (${comments.size} comentarios)",
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            if (comments.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay respuestas todavía. Escribe algo para unirte al debate.",
                            color = TextMuted,
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(comments) { comment ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = comment.authorName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = CoralSecondary
                                )
                                Text(
                                    text = "Hace poco",
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = comment.content,
                                fontSize = 14.sp,
                                color = TextPrimary,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        // Input comment bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("Escribe una respuesta gratis...", color = TextMuted) },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = CardSurfaceVariant,
                        unfocusedContainerColor = CardSurfaceVariant,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    maxLines = 2,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (commentText.isNotBlank()) {
                                viewModel.addComment(topic.id, "Tú (Usuario)", commentText)
                                commentText = ""
                                keyboardController?.hide()
                            }
                        }
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (commentText.isNotBlank()) {
                            viewModel.addComment(topic.id, "Tú (Usuario)", commentText)
                            commentText = ""
                            keyboardController?.hide()
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(CoralPrimary, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Enviar",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

// ==========================================
// 3C. CREATE DISCUSSION SCREEN
// ==========================================
@Composable
fun CreateTopicScreen(
    viewModel: DatingViewModel,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Debates") }
    val categories = listOf("Debates", "Consejos", "Locales")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = TextPrimary
                )
            }
            Text(
                text = "Iniciar Nuevo Debate",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = CoralPrimary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Topic Category selection
        Text(
            text = "Selecciona Categoría:",
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            categories.forEach { cat ->
                val isSelected = category == cat
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            color = if (isSelected) CoralPrimary else CardSurface,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { category = cat }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cat,
                        color = if (isSelected) Color.White else TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Title Input
        Text(
            text = "Título del Debate:",
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        TextField(
            value = title,
            onValueChange = { title = it },
            placeholder = { Text("Ej: ¿Pagar a medias en la primera cita?", color = TextMuted) },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = CardSurface,
                unfocusedContainerColor = CardSurface,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Description Input
        Text(
            text = "Argumento o Pregunta:",
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        TextField(
            value = description,
            onValueChange = { description = it },
            placeholder = { Text("Desarrolla tu tema para que los demás solteros puedan participar...", color = TextMuted) },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp)),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = CardSurface,
                unfocusedContainerColor = CardSurface,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            maxLines = 15
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (title.isNotBlank() && description.isNotBlank()) {
                    viewModel.createTopic(title, category, description, "Tú (Usuario)")
                    onBack()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary),
            shape = RoundedCornerShape(16.dp),
            enabled = title.isNotBlank() && description.isNotBlank()
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Publicar Debate Gratis", fontWeight = FontWeight.Bold)
        }
    }
}


// ==========================================
// 4. MY PROFILE SCREEN (MI PERFIL)
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(viewModel: DatingViewModel) {
    val myProfile by viewModel.currentUserProfile.collectAsStateWithLifecycle()
    val currentZone by viewModel.simulatedLocationZone.collectAsStateWithLifecycle()

    var showEditDialog by remember { mutableStateOf(false) }

    if (myProfile == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = CoralPrimary)
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Mi Perfil",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = CoralPrimary
            )
            Text(
                text = "Configura tus datos y simula tu ubicación gratis",
                color = TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Header Avatar and Main Info
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .border(3.dp, CoralPrimary, CircleShape)
                    ) {
                        AsyncImage(
                            model = myProfile!!.imageUrl,
                            contentDescription = "Mi foto",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "${myProfile!!.name}, ${myProfile!!.age}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Ubicación",
                            tint = CoralSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Simulado en: $currentZone",
                            color = CoralSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = myProfile!!.bio,
                        fontSize = 14.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Intereses
                    Text(
                        text = "MIS INTERESES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoralPrimary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    if (myProfile!!.tags.isNotBlank()) {
                        val tagList = myProfile!!.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        FlowRow(
                            horizontalArrangement = Arrangement.Center,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                        ) {
                            tagList.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .background(CoralSecondary.copy(alpha = 0.15f), RoundedCornerShape(100.dp))
                                        .border(1.dp, CoralSecondary.copy(alpha = 0.3f), RoundedCornerShape(100.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "# $tag",
                                        color = CoralSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "No has configurado intereses todavía.",
                            fontSize = 12.sp,
                            color = TextMuted,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showEditDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CardSurfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Editar Datos", color = Color.White)
                    }
                }
            }
        }

        // Location Simulator (100% Free Hosting compliance indicator)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, CoralSecondary.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "Simulador",
                            tint = CoralPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Simulador de Ubicación",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Cambia de zona para descubrir solteros cercanos en distintas partes de la ciudad. El cálculo se hace 100% local en tu dispositivo.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val zones = listOf("Zona Centro", "Zona Norte", "Zona Sur")
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        zones.forEach { zone ->
                            val isSelected = currentZone == zone
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = if (isSelected) CoralPrimary else CardSurfaceVariant,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { viewModel.changeSimulatedZone(zone) }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = zone,
                                    color = if (isSelected) Color.White else TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Unlimited Serverless/Offline Information Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardSurface.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = MatchGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Uso Gratuito e Ilimitado",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MatchGreen
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "CercaMatch almacena todos tus datos y chats localmente. Disfruta de una experiencia sin necesidad de internet constante ni servidores costosos de hosting.",
                        fontSize = 11.sp,
                        color = TextMuted,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }

    // Profile Editing Dialog
    if (showEditDialog) {
        var editName by remember { mutableStateOf(myProfile!!.name) }
        var editAgeStr by remember { mutableStateOf(myProfile!!.age.toString()) }
        var editBio by remember { mutableStateOf(myProfile!!.bio) }
        var editTags by remember { mutableStateOf(myProfile!!.tags) }

        Dialog(onDismissRequest = { showEditDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Editar Mi Perfil",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoralPrimary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    TextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Nombre", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = CardSurfaceVariant,
                            unfocusedContainerColor = CardSurfaceVariant,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TextField(
                        value = editAgeStr,
                        onValueChange = { editAgeStr = it },
                        label = { Text("Edad", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = CardSurfaceVariant,
                            unfocusedContainerColor = CardSurfaceVariant,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TextField(
                        value = editBio,
                        onValueChange = { editBio = it },
                        label = { Text("Biografía", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = CardSurfaceVariant,
                            unfocusedContainerColor = CardSurfaceVariant,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TextField(
                        value = editTags,
                        onValueChange = { editTags = it },
                        label = { Text("Intereses (por comas)", color = TextSecondary) },
                        placeholder = { Text("Ej: Música, Cine, Café", color = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = CardSurfaceVariant,
                            unfocusedContainerColor = CardSurfaceVariant,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Sugerencias de Intereses:",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start).padding(bottom = 6.dp)
                    )

                    val commonTags = listOf("Música", "Cine", "Debates", "Café", "Viajes", "Deportes", "Libros", "Cocina", "Fotografía", "Baile", "Arte", "Tecnología")
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val currentList = editTags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        commonTags.forEach { tag ->
                            val isSelected = currentList.any { it.equals(tag, ignoreCase = true) }
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isSelected) CoralPrimary else CardSurfaceVariant,
                                        shape = RoundedCornerShape(100.dp)
                                    )
                                    .clickable {
                                        val newList = if (isSelected) {
                                            currentList.filterNot { it.equals(tag, ignoreCase = true) }
                                        } else {
                                            currentList + tag
                                        }
                                        editTags = newList.joinToString(", ")
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = tag,
                                    color = if (isSelected) Color.White else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = { showEditDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar", color = TextSecondary)
                        }

                        Button(
                            onClick = {
                                val age = editAgeStr.toIntOrNull() ?: myProfile!!.age
                                viewModel.updateMyProfile(
                                    name = editName,
                                    age = age,
                                    bio = editBio,
                                    gender = myProfile!!.gender,
                                    imageUrl = myProfile!!.imageUrl,
                                    tags = editTags
                                )
                                showEditDialog = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary)
                        ) {
                            Text("Guardar", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
