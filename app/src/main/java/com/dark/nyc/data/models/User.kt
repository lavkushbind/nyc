package com.dark.nyc.data.models

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@IgnoreExtraProperties
data class NYCUser(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val age: Int = 0,
    val gender: String = "",
    val borough: String = "",
    val neighborhood: String = "",
    val bio: String = "Living the NYC dream 🗽",

    @get:PropertyName("photoURL")
    @set:PropertyName("photoURL")
    var photoURL: String = "",

    val personalityTags: List<String> = emptyList(),
    val nycEnergy: String = "Chill Local",
    val unpopularOpinion: String = "",
    val lookingFor: String = "Open to anything",
    val groupCode: String = "",
    val hasActiveSubscription: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val onboardingComplete: Boolean = true
) {
    fun toMap(): Map<String, Any> = mapOf(
        "uid" to uid,
        "name" to name,
        "email" to email,
        "age" to age,
        "gender" to gender,
        "borough" to borough,
        "neighborhood" to neighborhood,
        "bio" to bio,
        "photoURL" to photoURL,
        "personalityTags" to personalityTags,
        "nycEnergy" to nycEnergy,
        "unpopularOpinion" to unpopularOpinion,
        "lookingFor" to lookingFor,
        "groupCode" to groupCode,
        "hasActiveSubscription" to hasActiveSubscription,
        "createdAt" to createdAt,
        "onboardingComplete" to onboardingComplete
    )
}
