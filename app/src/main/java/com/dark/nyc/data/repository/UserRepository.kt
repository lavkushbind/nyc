package com.dark.nyc.data.repository

import android.util.Log
import com.dark.nyc.data.models.NYCUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    init {
        try {
            firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()
        } catch (e: Exception) {
            Log.w("UserRepo", "Firestore settings already initialized: ${e.message}")
        }
    }

    /**
     * 1️⃣ Get Current User (Real-time Flow with Offline Cache Support)
     */
    fun getCurrentUser(): Flow<NYCUser?> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("nyc_users")
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("UserRepo", "Error fetching current user: ${error.message}")
                    trySend(null)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val user = snapshot.toObject(NYCUser::class.java)
                    trySend(user)
                } else {
                    trySend(null)
                }
            }

        awaitClose { listener.remove() }
    }

    /**
     * 2️⃣ Get Potential Matches (Index Crash-Free & Fast)
     */
    suspend fun getPotentialMatches(
        currentUserId: String,
        limit: Long = 10,
        lastDoc: DocumentSnapshot? = null
    ): Pair<List<NYCUser>, DocumentSnapshot?> {
        return try {
            // Step A: Already swiped users ki list nikalo
            val swipedUserIds = mutableSetOf<String>()
            try {
                val swipesSnapshot = firestore.collection("nyc_swipes")
                    .whereEqualTo("swiperId", currentUserId)
                    .get()
                    .await()

                for (doc in swipesSnapshot.documents) {
                    doc.getString("targetId")?.let { swipedUserIds.add(it) }
                }
            } catch (e: Exception) {
                Log.w("UserRepo", "Could not fetch existing swipes: ${e.message}")
            }

            // Step B: Fetch users from nyc_users (Zero index crash risk)
            var query = firestore.collection("nyc_users")
                .limit(limit * 2) // Extra load karo taaki filtering ke baad bhi enough users bachein

            if (lastDoc != null) {
                query = query.startAfter(lastDoc)
            }

            val snapshot = query.get().await()
            val newLastDoc = snapshot.documents.lastOrNull()

            // Step C: Filter out self and already swiped users in memory
            val users = snapshot.documents.mapNotNull { doc ->
                val user = doc.toObject(NYCUser::class.java)
                // Agar uid blank ho to documentId use kar lo
                if (user != null && user.uid.isBlank()) {
                    user.copy(uid = doc.id)
                } else {
                    user
                }
            }.filter { user ->
                val isValid = user.uid.isNotBlank()
                val isNotSelf = user.uid != currentUserId
                val notSwipedYet = !swipedUserIds.contains(user.uid)
                isValid && isNotSelf && notSwipedYet
            }

            Log.d("UserRepo", "✅ Successfully loaded ${users.size} potential matches from nyc_users")
            Pair(users, newLastDoc)

        } catch (e: Exception) {
            Log.e("UserRepo", "❌ Error in getPotentialMatches: ${e.message}", e)
            Pair(emptyList(), null)
        }
    }

    /**
     * 3️⃣ Save Swipe Action (Right = Like, Left = Pass)
     */
    suspend fun saveSwipe(
        currentUserId: String,
        targetUserId: String,
        isLike: Boolean
    ): Boolean {
        return try {
            val swipeDocId = "${currentUserId}_${targetUserId}"
            val swipeData = mapOf(
                "swiperId" to currentUserId,
                "targetId" to targetUserId,
                "isLike" to isLike,
                "timestamp" to System.currentTimeMillis()
            )

            firestore.collection("nyc_swipes")
                .document(swipeDocId)
                .set(swipeData)
                .await()

            Log.d("UserRepo", "Swipe saved: $currentUserId -> $targetUserId ($isLike)")
            true
        } catch (e: Exception) {
            Log.e("UserRepo", "Failed to save swipe: ${e.message}")
            false
        }
    }

    /**
     * 4️⃣ Check if Mutual Match (O(1) Instant Lookup - No Index Needed)
     */
    suspend fun checkMatch(currentUserId: String, targetUserId: String): Boolean {
        return try {
            // Check if the other person has already liked current user
            val otherSwipeDocId = "${targetUserId}_${currentUserId}"
            val doc = firestore.collection("nyc_swipes")
                .document(otherSwipeDocId)
                .get()
                .await()

            doc.exists() && (doc.getBoolean("isLike") == true)
        } catch (e: Exception) {
            Log.e("UserRepo", "Error checking match: ${e.message}")
            false
        }
    }

    /**
     * 5️⃣ Create Match Document
     */
    suspend fun createMatch(user1: String, user2: String): String {
        return try {
            val matchDoc = firestore.collection("nyc_matches").document()
            val matchId = matchDoc.id
            val matchData = mapOf(
                "matchId" to matchId,
                "users" to listOf(user1, user2),
                "matchedAt" to System.currentTimeMillis(),
                "status" to "active"
            )
            matchDoc.set(matchData).await()
            Log.d("UserRepo", "🎉 Match created successfully: $matchId")
            matchId
        } catch (e: Exception) {
            Log.e("UserRepo", "Failed to create match: ${e.message}")
            ""
        }
    }
}