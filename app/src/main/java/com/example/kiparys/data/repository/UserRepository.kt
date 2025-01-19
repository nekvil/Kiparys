package com.example.kiparys.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.example.kiparys.data.model.User
import com.example.kiparys.Constants
import com.example.kiparys.data.model.UserProject
import com.example.kiparys.data.model.UserTask
import com.google.firebase.Firebase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.database
import com.google.firebase.database.snapshots
import com.google.firebase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream


class UserRepository {

    private val database = Firebase.database
    private val storage = Firebase.storage


    suspend fun checkUserData(userId: String): Result<Boolean> {
        return try {
            val userRef = database.getReference("users").child(userId)
            val dataSnapshot = userRef.get().await()
            if (dataSnapshot.exists()) {
                val firstName =
                    dataSnapshot.child("firstName").getValue(String::class.java).orEmpty()
                val email = dataSnapshot.child("email").getValue(String::class.java).orEmpty()
                val profileImageUrl =
                    dataSnapshot.child("profileImageUrl").getValue(String::class.java).orEmpty()
                if (firstName.isNotBlank() && email.isNotBlank() && profileImageUrl.isNotBlank()) {
                    Result.success(true)
                } else {
                    Result.success(false)
                }
            } else {
                Result.success(false)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun uploadProfileImage(
        profileImageBitmap: Bitmap,
        profileImageFileName: String,
        imageQuality: Int
    ): Result<String> {
        return try {
            val profileImageRef = storage.reference.child(profileImageFileName)
            val byteArrayOutputStream = ByteArrayOutputStream()
            profileImageBitmap.compress(
                Bitmap.CompressFormat.JPEG,
                imageQuality,
                byteArrayOutputStream
            )
            val profileImageData = byteArrayOutputStream.toByteArray()
            profileImageRef.putBytes(profileImageData).await()
            val downloadUrl = profileImageRef.downloadUrl.await()
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteUserTask(userId: String, taskId: String): Result<Unit> {
        return try {
            database.getReference("userTasks")
                .child(userId)
                .child(taskId)
                .removeValue()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserTask(
        userId: String,
        taskId: String,
        updates: Map<String, Any?>
    ): Result<Unit> {
        return try {
            database.getReference("userTasks").child(userId).child(taskId)
                .updateChildren(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserData(userId: String, updates: Map<String, Any?>): Result<Unit> {
        return try {
            database.getReference("users").child(userId)
                .updateChildren(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFileFromStorage(fileUrl: String): Result<Unit> {
        return try {
            val storageRef = storage.getReferenceFromUrl(fileUrl)
            storageRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting file: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getProfileImageUrl(userId: String, containsString: String): String? {
        return try {
            val storageRef = storage.reference.child("users/$userId/profileImage/")
            val result = storageRef.listAll().await()
            val matchingFileRef =
                result.items.find { it.name.contains(containsString, ignoreCase = true) }
            matchingFileRef?.downloadUrl?.await()?.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching profile image containing '$containsString': ${e.message}", e)
            null
        }
    }

    suspend fun fetchUserDataOnce(userId: String): Result<Map<String, Any?>> {
        return try {
            val snapshot = database.getReference("users").child(userId)
                .get()
                .await()
            val userData = snapshot.value
            if (userData is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                Result.success(userData as Map<String, Any?>)
            } else {
                Result.failure(Exception("User data not found or invalid format"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchMembersByEmail(
        emailQuery: String,
        members: Map<String, Boolean>?
    ): Result<List<User>> {
        return try {
            val query = database.getReference("users")
                .orderByChild("email")
                .startAt(emailQuery)
                .endAt(emailQuery + "\uf8ff")
            val snapshot = query.get().await()

            val userList = mutableListOf<User>()
            for (userSnapshot in snapshot.children) {
                val userId = userSnapshot.key
                val userData = userSnapshot.getValue(User::class.java)
                if (userId != null && userData != null && userData.deleted != true) {
                    if (members?.get(userId) == true) {
                        userData.id = userId
                        userList.add(userData)
                    }
                }
            }
            Result.success(userList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchUsersByEmail(emailQuery: String, currentUserId: String): Result<List<User>> {
        if (emailQuery.isBlank()) {
            return Result.success(emptyList())
        }
        return try {
            val query = database.getReference("users")
                .orderByChild("email")
                .startAt(emailQuery)
                .endAt(emailQuery + "\uf8ff")
                .limitToFirst(100)
            val snapshot = query.get().await()

            val userList = mutableListOf<User>()
            for (userSnapshot in snapshot.children) {
                val userId = userSnapshot.key
                val userData = userSnapshot.getValue(User::class.java)

                if (userId != null && userId != currentUserId && userData != null) {
                    if (userData.deleted != true) {
                        userData.id = userId
                        userList.add(userData)
                    }
                }
            }

            Result.success(userList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

//    suspend fun disconnectUser(userId: String) {
//        val userConnectionsRef = database.getReference("users/$userId/connections")
//        val lastOnlineRef = database.getReference("users/$userId/lastOnline")
//
//        try {
//            removeConnections(userConnectionsRef)
//            updateLastOnline(lastOnlineRef)
//            Log.d(TAG, "Пользователь $userId отключен. lastOnline обновлен.")
//        } catch (e: Exception) {
//            Log.e(TAG, "Ошибка при отключении пользователя $userId: ${e.message}")
//        }
//    }
//
//    private suspend fun removeConnections(userConnectionsRef: DatabaseReference) {
//        suspendCancellableCoroutine<Unit> { continuation ->
//            userConnectionsRef.removeValue().addOnCompleteListener { task ->
//                if (task.isSuccessful) {
//                    continuation.resumeWith(Result.success(Unit))
//                } else {
//                    task.exception?.let { continuation.resumeWithException(it) }
//                }
//            }
//        }
//    }
//
//    private suspend fun updateLastOnline(lastOnlineRef: DatabaseReference) {
//        suspendCancellableCoroutine<Unit> { continuation ->
//            lastOnlineRef.setValue(ServerValue.TIMESTAMP).addOnCompleteListener { task ->
//                if (task.isSuccessful) {
//                    continuation.resumeWith(Result.success(Unit))
//                } else {
//                    task.exception?.let { continuation.resumeWithException(it) }
//                }
//            }
//        }
//    }

//        fun getUserDataFlow(userId: String): Flow<Result<User>> = callbackFlow {
//        val userRef = database.getReference("users").child(userId)
//
//        val listener = object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                if (snapshot.exists()) {
//                    val userData = snapshot.getValue(User::class.java)
//                    if (userData != null) {
//                        trySend(Result.success(userData))
//                    } else {
//                        trySend(Result.failure(Exception(Constants.ERROR_UNKNOWN)))
//                    }
//                } else {
//                    trySend(Result.failure(Exception(Constants.ERROR_UNKNOWN)))
//                }
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                trySend(Result.failure(error.toException()))
//            }
//        }
//
//        userRef.addValueEventListener(listener)
//        Log.d(TAG, "Слушатель добавлен для userId: $userId")
//        awaitClose {
//            userRef.removeEventListener(listener)
//            Log.d(TAG, "Слушатель удалён для userId: $userId")
//        }
//
//    }

//    fun observeUserOnlineStatus(userId: String): Flow<Result<Boolean>> = callbackFlow {
//        val connectedRef = database.getReference(".info/connected")
//        val userConnectionsRef = database.getReference("users/$userId/connections")
//        val lastOnlineRef = database.getReference("users/$userId/lastOnline")
//
//        val listener = object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                val connected = snapshot.getValue<Boolean>() == true
//                if (connected) {
//
//                    val connectionRef = userConnectionsRef.push()
//
//                    connectionRef.onDisconnect().removeValue()
//
//                    lastOnlineRef.onDisconnect().setValue(ServerValue.TIMESTAMP)
//
//                    connectionRef.setValue(true)
//
//                    trySend(Result.success(true))
//                } else {
//                    trySend(Result.success(false))
//                }
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                trySend(Result.failure(error.toException()))
//            }
//        }
//
//        connectedRef.addValueEventListener(listener)
//        Log.d(TAG, "Добавлен слушатель для состояния онлайн для userId: $userId")
//
//        awaitClose {
//            connectedRef.removeEventListener(listener)
//            Log.d(TAG, "Слушатель для состояния онлайн удалён для userId: $userId")
//        }
//    }

    fun getUserTasksFlow(userId: String): Flow<Result<List<UserTask>>> =
        database.getReference("userTasks")
            .child(userId)
            .orderByChild("timestamp")
            .snapshots
            .map { snapshot ->
                val userTasks = snapshot.children.mapNotNull { child ->
                    child.getValue(UserTask::class.java)?.apply { id = child.key }
                }
                if (userTasks.isNotEmpty()) {
                    val sortedTasks = userTasks.sortedWith(
                        compareBy<UserTask> { it.completed == true }
                            .thenByDescending { it.created }
                    )
                    Result.success(sortedTasks)
                } else {
                    Result.success(emptyList())
                }
            }
            .catch { e -> emit(Result.failure(e)) }

    fun getUserIncompleteTasksCountFlow(userId: String): Flow<Result<Int>> =
        database.getReference("userTasks")
            .child(userId)
            .snapshots
            .map { snapshot ->
                val incompleteTasksCount = snapshot.children.mapNotNull { child ->
                    child.getValue(UserTask::class.java)?.apply { id = child.key }
                }.count { userTask ->
                    userTask.completed != true
                }
                Result.success(incompleteTasksCount)
            }
            .catch { e -> emit(Result.failure(e)) }

    fun getUserProjectsFlow(userId: String): Flow<Result<List<UserProject>>> =
        database.getReference("userProjects")
            .child(userId)
            .orderByChild("timestamp")
            .snapshots
            .map { snapshot ->
                val userProjects = snapshot.children.mapNotNull { child ->
                    child.getValue(UserProject::class.java)?.apply { id = child.key }
                }
                if (userProjects.isNotEmpty()) {
                    Result.success(userProjects.sortedBy { it.pinned })
                } else {
                    Result.success(emptyList())
                }
            }
            .catch { e -> emit(Result.failure(e)) }

    fun getUserProjectsUnreadCountFlow(userId: String): Flow<Result<Int>> =
        database.getReference("userProjects")
            .child(userId)
            .snapshots
            .map { snapshot ->
                val totalUnreadCount = snapshot.children
                    .mapNotNull { child ->
                        child.getValue(UserProject::class.java)?.apply { id = child.key }
                    }
                    .sumOf { userProject ->
                        userProject.unreadMessagesCount ?: 0
                    }
                Result.success(totalUnreadCount)
            }
            .catch { e -> emit(Result.failure(e)) }

    fun getUserDataFlow(userId: String): Flow<Result<User>> =
        database.getReference("users").child(userId).snapshots
            .map { snapshot ->
                snapshot.getValue(User::class.java)?.let { userData ->
                    Result.success(userData)
                } ?: Result.failure(Exception(Constants.ERROR_UNKNOWN))
            }
            .catch { e -> emit(Result.failure(e)) }

    fun getMemberDataFlow(userId: String): Flow<Result<User>> =
        database.getReference("users").child(userId).snapshots
            .map { snapshot ->
                snapshot.getValue(User::class.java)?.let { userData ->
                    Result.success(userData)
                } ?: Result.failure(Exception(Constants.ERROR_UNKNOWN))
            }
            .catch { e -> emit(Result.failure(e)) }

    fun observeUserOnlineStatus(userId: String): Flow<Result<Boolean>> =
        database.getReference(".info/connected").snapshots
            .map { snapshot ->
                val connected = snapshot.getValue(Boolean::class.java) == true
                if (connected) {
                    val userConnectionsRef = database.getReference("users/$userId/connections")
                    val lastOnlineRef = database.getReference("users/$userId/lastOnline")
                    val connectionRef = userConnectionsRef.push()

                    connectionRef.onDisconnect().removeValue()

                    lastOnlineRef.onDisconnect().setValue(ServerValue.TIMESTAMP)

                    connectionRef.setValue(ServerValue.TIMESTAMP)

                    Result.success(true)
                } else {
                    Result.success(false)
                }
            }
            .catch { e -> emit(Result.failure(e)) }

    fun userInProjectChatStatusFlow(userId: String, projectId: String): Flow<Result<Boolean>> =
        database.getReference(".info/connected").snapshots
            .map { snapshot ->
                val connected = snapshot.getValue(Boolean::class.java) == true
                if (connected) {
                    val userConnectionsRef = database.getReference("projects/$projectId/nowInChat")
                    val connectionRef = userConnectionsRef.child(userId)
                    val userTypingRef = database.getReference("projects/$projectId/typing")
                    val typingRef = userTypingRef.child(userId)
                    typingRef.onDisconnect().removeValue()
                    connectionRef.onDisconnect().removeValue()
                    connectionRef.setValue(java.lang.Boolean.TRUE)
                    Result.success(true)
                } else {
                    Result.success(false)
                }
            }
            .catch { e -> emit(Result.failure(e)) }

    fun getUnreadProjectMessagesCountFlow(userId: String, projectId: String): Flow<Result<Int>> =
        database.getReference("userProjects")
            .child(userId)
            .child(projectId)
            .child("unreadMessagesCount")
            .snapshots
            .map { snapshot ->
                val unreadCount = snapshot.getValue(Int::class.java) ?: 0
                Result.success(unreadCount)
            }
            .catch { e -> emit(Result.failure(e)) }

    suspend fun cleanupUserStatus(userId: String, projectId: String): Result<Unit> {
        return try {
            val userConnectionsRef =
                database.getReference("projects/$projectId/nowInChat").child(userId)
            userConnectionsRef.removeValue().await()
            val userTypingRef = database.getReference("projects/$projectId/typing").child(userId)
            userTypingRef.removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun goOnline() {
        database.goOnline()
    }

    fun goOffline() {
        database.goOffline()
    }

    companion object {
        private const val TAG = "UserRepository"
    }

}
