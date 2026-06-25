package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [UserProfile::class, Message::class, ForumTopic::class, ForumComment::class],
    version = 2,
    exportSchema = false
)
abstract class DatingDatabase : RoomDatabase() {

    abstract fun datingDao(): DatingDao

    companion object {
        @Volatile
        private var INSTANCE: DatingDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): DatingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DatingDatabase::class.java,
                    "dating_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatingDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatingDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.datingDao())
                }
            }
        }

        suspend fun populateDatabase(dao: DatingDao) {
            // 1. Insert current user profile
            dao.insertUserProfile(
                UserProfile(
                    id = 1,
                    name = "Tú (Usuario)",
                    age = 24,
                    bio = "¡Hola! Estoy buscando conocer gente interesante cerca de mí, charlar y debatir sobre diversos temas en los foros.",
                    gender = "Hombre",
                    distance = 0.0,
                    imageUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=300&q=80",
                    isCurrentUser = true,
                    tags = "Música, Cine, Debates, Café, Viajes"
                )
            )

            // 2. Insert nearby user profiles
            val profiles = listOf(
                UserProfile(
                    id = 2,
                    name = "Sophia",
                    age = 25,
                    bio = "Bailarina y amante del buen café. Busco reír, charlar sobre libros y caminar por la ciudad.",
                    gender = "Mujer",
                    distance = 1.2,
                    imageUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=300&q=80",
                    isMatched = false,
                    hasLiked = false,
                    tags = "Café, Baile, Libros, Museos"
                ),
                UserProfile(
                    id = 3,
                    name = "Mateo",
                    age = 28,
                    bio = "Apasionado de la montaña, la escalada y la pizza de masa madre. Hablemos de música indie y viajes.",
                    gender = "Hombre",
                    distance = 2.5,
                    imageUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=300&q=80",
                    isMatched = false,
                    hasLiked = false,
                    tags = "Montaña, Escalada, Pizza, Rock"
                ),
                UserProfile(
                    id = 4,
                    name = "Valentina",
                    age = 24,
                    bio = "Melómana de clóset, toco la guitarra acústica y me encantan los debates de medianoche y el cine alternativo.",
                    gender = "Mujer",
                    distance = 3.8,
                    imageUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=300&q=80",
                    isMatched = true, // start matched to show active chat feature immediately!
                    hasLiked = true,
                    tags = "Guitarra, Debates, Cine, Arte"
                ),
                UserProfile(
                    id = 5,
                    name = "Lucas",
                    age = 31,
                    bio = "Cocinero amateur y fotógrafo de fines de semana. ¿Vamos por un par de cervezas artesanales y buena conversación?",
                    gender = "Hombre",
                    distance = 5.1,
                    imageUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=300&q=80",
                    isMatched = false,
                    hasLiked = false,
                    tags = "Cocina, Fotografía, Cerveza, Charla"
                ),
                UserProfile(
                    id = 6,
                    name = "Camila",
                    age = 27,
                    bio = "Ingeniera de software que ama el senderismo y los juegos de mesa. Café por la mañana, vino por la noche.",
                    gender = "Mujer",
                    distance = 0.8,
                    imageUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=300&q=80",
                    isMatched = true, // another starting match
                    hasLiked = true,
                    tags = "Senderismo, Juegos de Mesa, Café, Tecnología"
                ),
                UserProfile(
                    id = 7,
                    name = "Diego",
                    age = 29,
                    bio = "Viajero empedernido, 30 países y contando. Busco compañera de aventuras y risas sinceras.",
                    gender = "Hombre",
                    distance = 4.2,
                    imageUrl = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=300&q=80",
                    isMatched = false,
                    hasLiked = false,
                    tags = "Viajes, Fotografía, Aventuras, Playa"
                ),
                UserProfile(
                    id = 8,
                    name = "Elena",
                    age = 26,
                    bio = "Amante de las galerías de arte y los museos. Dibujo e ilustro en mis tiempos libres. ¿Cine clásico o exposición?",
                    gender = "Mujer",
                    distance = 6.0,
                    imageUrl = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=300&q=80",
                    isMatched = false,
                    hasLiked = false,
                    tags = "Arte, Ilustración, Museos, Libros, Cine"
                )
            )
            for (p in profiles) {
                dao.insertUserProfile(p)
            }

            // 3. Insert initial messages
            val initialMessages = listOf(
                Message(
                    senderId = 4, // Valentina
                    receiverId = -1,
                    content = "¡Hola! Vi que nos gusta la misma música. ¿Qué bandas has estado escuchando últimamente?",
                    timestamp = System.currentTimeMillis() - 3600000 * 2 // 2 hours ago
                ),
                Message(
                    senderId = -1, // Me
                    receiverId = 4, // Valentina
                    content = "¡Hola Valentina! Sí, me encanta el indie rock. He estado escuchando bastante a Arctic Monkeys y Tame Impala.",
                    timestamp = System.currentTimeMillis() - 3600000 * 1 // 1 hour ago
                ),
                Message(
                    senderId = 4, // Valentina
                    receiverId = -1,
                    content = "¡Qué genial! Tame Impala es increíble en vivo. ¿Has ido a alguno de sus conciertos?",
                    timestamp = System.currentTimeMillis() - 1800000 // 30 mins ago
                ),
                Message(
                    senderId = 6, // Camila
                    receiverId = -1,
                    content = "Hola, ¡qué coincidencia que vivamos tan cerca! ¿Te gustan los juegos de mesa?",
                    timestamp = System.currentTimeMillis() - 7200000 // 2 hours ago
                )
            )
            for (m in initialMessages) {
                dao.insertMessage(m)
            }

            // 4. Insert initial forum topics
            val topic1Id = dao.insertForumTopic(
                ForumTopic(
                    title = "Cine en la primera cita: ¿Sí o No?",
                    category = "Debates",
                    description = "Muchos dicen que no se puede hablar en el cine y es mala idea, mientras que otros opinan que es genial para romper el hielo y tener tema de conversación luego. ¿Ustedes qué piensan?",
                    authorName = "Camila",
                    likes = 12
                )
            ).toInt()

            val topic2Id = dao.insertForumTopic(
                ForumTopic(
                    title = "Mejores cafeterías para una cita tranquila",
                    category = "Consejos",
                    description = "Estoy planeando una primera cita y busco recomendaciones de lugares con buen café, ambiente relajado y música de fondo suave para conversar. ¡Los leo!",
                    authorName = "Lucas",
                    likes = 8
                )
            ).toInt()

            val topic3Id = dao.insertForumTopic(
                ForumTopic(
                    title = "Debate: ¿Quién debería pagar en la primera cita?",
                    category = "Debates",
                    description = "Un clásico debate que nunca pasa de moda. ¿Se divide la cuenta 50/50, paga quien invita, o depende de la química del momento? Opiniones sinceras.",
                    authorName = "Sophia",
                    likes = 15
                )
            ).toInt()

            // 5. Insert comments for topics
            val comments = listOf(
                ForumComment(
                    topicId = topic1Id,
                    authorName = "Sophia",
                    content = "¡Totalmente NO! No se habla nada en dos horas. Prefiero mil veces un café rápido y luego caminar por la plaza.",
                    timestamp = System.currentTimeMillis() - 12000000
                ),
                ForumComment(
                    topicId = topic1Id,
                    authorName = "Mateo",
                    content = "De acuerdo con Sophia. El cine es para la tercera o cuarta cita, cuando ya hay suficiente confianza.",
                    timestamp = System.currentTimeMillis() - 10000000
                ),
                ForumComment(
                    topicId = topic1Id,
                    authorName = "Lucas",
                    content = "Yo creo que si ven la película primero y luego van a cenar, es perfecto porque ya tienen un tema garantizado para platicar.",
                    timestamp = System.currentTimeMillis() - 8000000
                ),
                ForumComment(
                    topicId = topic2Id,
                    authorName = "Elena",
                    content = "La cafetería 'Café de las Artes' tiene un jardín interior hermoso y es súper silenciosa. ¡Altamente recomendada!",
                    timestamp = System.currentTimeMillis() - 5000000
                ),
                ForumComment(
                    topicId = topic2Id,
                    authorName = "Camila",
                    content = "Apoyo a Elena, 'Café de las Artes' es ideal. Los pasteles son una delicia.",
                    timestamp = System.currentTimeMillis() - 3000000
                ),
                ForumComment(
                    topicId = topic3Id,
                    authorName = "Diego",
                    content = "Para mí, lo más justo en la primera cita es dividir 50/50. Así no hay presiones ni expectativas de ningún lado.",
                    timestamp = System.currentTimeMillis() - 9000000
                ),
                ForumComment(
                    topicId = topic3Id,
                    authorName = "Valentina",
                    content = "A mí me gusta que quien tenga la iniciativa de invitar pague, pero siempre ofrezco pagar mi parte por educación. Si insiste, acepto y yo invito la ronda de postres.",
                    timestamp = System.currentTimeMillis() - 6000000
                )
            )
            for (c in comments) {
                dao.insertForumComment(c)
            }
        }
    }
}
