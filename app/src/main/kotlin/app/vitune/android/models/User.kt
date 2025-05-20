package app.vitune.android.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class User(
    @PrimaryKey val email: String,
    val passWord: String,
    val name: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)