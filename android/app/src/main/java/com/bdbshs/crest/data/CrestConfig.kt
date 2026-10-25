package com.bdbshs.crest.data

import com.bdbshs.crest.BuildConfig

object StorageConfig {
    val BUCKET_ID: String
        get() = BuildConfig.APPWRITE_BUCKET_ID
}

object FirestorePaths {
    const val STUDENTS = "users/user_details/students"
    const val TEACHERS = "users/user_details/teachers"
    const val QUALITATIVE_RESEARCHES = "researches/research_details/qualitative"
    const val QUANTITATIVE_RESEARCHES = "researches/research_details/quantitative"
    const val RESEARCHES_BASE = "researches/research_details"
    const val USER_FAVORITES = "user_favorites"
    const val DOCUMENTS = "documents"
    const val GROUPS = "groups"
}