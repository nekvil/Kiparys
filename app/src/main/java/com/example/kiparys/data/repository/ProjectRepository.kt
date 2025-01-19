package com.example.kiparys.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.kiparys.Constants
import com.example.kiparys.data.model.Project
import com.example.kiparys.data.model.ProjectIdea
import com.example.kiparys.data.model.ProjectMedia
import com.example.kiparys.data.model.ProjectMessage
import com.example.kiparys.data.model.ProjectTask
import com.example.kiparys.data.model.UserProject
import com.example.kiparys.util.MimeTypeUtil.getExtensionFromMimeType
import com.google.firebase.Firebase
import com.google.firebase.database.database
import com.google.firebase.database.snapshots
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.storage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.absoluteValue


class ProjectRepository {

    private val database = Firebase.database
    private val storage = Firebase.storage

    suspend fun downloadAndCacheFile(
        context: Context,
        url: String,
        mimeType: String,
        timestamp: Long
    ): Result<File> {
        return try {
            val uniqueFileName =
                "${url.hashCode().absoluteValue}${timestamp}.${getExtensionFromMimeType(mimeType)}"
            val cacheDir = File(context.cacheDir, "files_cache")
            if (!cacheDir.exists()) cacheDir.mkdir()

            val cachedFile = File(cacheDir, uniqueFileName)

            if (cachedFile.exists()) {
                return Result.success(cachedFile)
            }

            val httpsReference = storage.getReferenceFromUrl(url)
            val tempFile = File.createTempFile("file", mimeType.substringAfter("/"))
            httpsReference.getFile(tempFile).await()

            tempFile.renameTo(cachedFile)
            Result.success(cachedFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadProjectFile(
        uriFile: Uri,
        projectFileName: String
    ): Result<Pair<String, StorageMetadata>> {
        return try {
            val projectFileRef = storage.reference.child(projectFileName)
            val uploadTask = projectFileRef.putFile(uriFile).await()
            val metadata = uploadTask.metadata ?: throw Exception("Metadata is null")
            val downloadUrl = projectFileRef.downloadUrl.await()
            Result.success(downloadUrl.toString() to metadata)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadProjectMediaJpeg(
        projectImageBitmap: Bitmap,
        projectImageFileName: String,
        imageQuality: Int
    ): Result<Pair<String, StorageMetadata>> {
        return try {
            val projectImageRef = storage.reference.child(projectImageFileName)
            val byteArrayOutputStream = ByteArrayOutputStream()
            projectImageBitmap.compress(
                Bitmap.CompressFormat.JPEG,
                imageQuality,
                byteArrayOutputStream
            )
            val projectImageData = byteArrayOutputStream.toByteArray()
            val uploadTask = projectImageRef.putBytes(projectImageData).await()
            val metadata = uploadTask.metadata ?: throw Exception("Metadata is null")
            val downloadUrl = projectImageRef.downloadUrl.await()
            Result.success(downloadUrl.toString() to metadata)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProjectTask(projectId: String, taskId: String): Result<Unit> {
        return try {
            database.getReference("projectTasks")
                .child(projectId)
                .child(taskId)
                .removeValue()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun deleteProjectTasks(projectId: String): Result<Unit> {
        return try {
            database.getReference("projectTasks")
                .child(projectId)
                .removeValue()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProjectIdea(projectId: String, ideaId: String): Result<Unit> {
        return try {
            database.getReference("projectIdeas")
                .child(projectId)
                .child(ideaId)
                .removeValue()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProjectIdeas(projectId: String): Result<Unit> {
        return try {
            database.getReference("projectIdeas")
                .child(projectId)
                .removeValue()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProjectMessagesSeenBy(projectId: String): Result<Unit> {
        return try {
            database.getReference("projectMessagesSeenBy")
                .child(projectId)
                .removeValue()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProjectMessages(projectId: String): Result<Unit> {
        return try {
            database.getReference("projectMessages")
                .child(projectId)
                .removeValue()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAllProjectFile(projectId: String): Result<Unit> {
        return try {
            database.getReference("projectFiles")
                .child(projectId)
                .removeValue()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAllProjectMedia(projectId: String): Result<Unit> {
        return try {
            database.getReference("projectMedia")
                .child(projectId)
                .removeValue()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProjectFile(projectId: String, fileId: String): Result<Unit> {
        return try {
            database.getReference("projectFiles")
                .child(projectId)
                .child(fileId)
                .removeValue()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProjectMedia(projectId: String, mediaId: String): Result<Unit> {
        return try {
            database.getReference("projectMedia")
                .child(projectId)
                .child(mediaId)
                .removeValue()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProjectMessageMedia(
        projectId: String,
        messageId: String,
        mediaId: String
    ): Result<Unit> {
        return try {
            database.getReference("projectMessages")
                .child(projectId)
                .child(messageId)
                .child("media")
                .child(mediaId)
                .removeValue()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProjectMessageSeenBy(projectId: String, messageId: String): Result<Unit> {
        return try {
            database.getReference("projectMessagesSeenBy")
                .child(projectId)
                .child(messageId)
                .removeValue()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProjectMessage(projectId: String, messageId: String): Result<Unit> {
        return try {
            database.getReference("projectMessages")
                .child(projectId)
                .child(messageId)
                .removeValue()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProjectStorageAttachmentsChat(projectId: String): Result<Unit> {
        return try {
            val storageRef =
                Firebase.storage.reference.child("projects/$projectId/attachments/chat")
            deleteStorageRecursively(storageRef)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProjectStorage(projectId: String): Result<Unit> {
        return try {
            val storageRef = Firebase.storage.reference.child("projects/$projectId")
            deleteStorageRecursively(storageRef)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun deleteStorageRecursively(directoryRef: StorageReference) {
        val allFiles = directoryRef.listAll().await()
        coroutineScope {
            allFiles.items.map { fileRef ->
                async {
                    fileRef.delete().await()
                }
            }.awaitAll()

            allFiles.prefixes.map { subDirectoryRef ->
                async {
                    deleteStorageRecursively(subDirectoryRef)
                }
            }.awaitAll()
        }
    }

    suspend fun deleteProjectStorageFile(fileUrl: String): Result<Unit> {
        return try {
            val storageReference = storage.getReferenceFromUrl(fileUrl)
            storageReference.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProject(projectId: String): Result<Unit> {
        return try {
            database.getReference("projects")
                .child(projectId)
                .removeValue()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProjectMessagesWithReply(
        projectId: String,
        replyMessageId: String
    ): Result<List<ProjectMessage>> {
        return try {
            val projectMessagesRef = database.getReference("projectMessages").child(projectId)
                .orderByChild("replyTo/replyMessageId").equalTo(replyMessageId)
            val snapshot = projectMessagesRef.get().await()

            val messages = snapshot.children.mapNotNull { dataSnapshot ->
                dataSnapshot.getValue(ProjectMessage::class.java)?.also {
                    it.id = dataSnapshot.key
                }
            }

            Result.success(messages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNotSeenProjectMessages(
        projectId: String,
        userId: String
    ): Result<List<ProjectMessage>> {
        return try {
            val projectMessagesRef = database.getReference("projectMessages").child(projectId)
                .orderByChild("seen")
                .equalTo(null)
            val snapshot = projectMessagesRef.get().await()
            val messages = snapshot.children.mapNotNull { dataSnapshot ->
                dataSnapshot.getValue(ProjectMessage::class.java)?.also {
                    it.id = dataSnapshot.key
                }
            }.filter { message ->
                message.senderId != userId
            }
            Result.success(messages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserProjectTasks(userId: String, projectId: String): Result<List<String>> {
        return try {
            val userTasksRef = database.getReference("userTasks").child(userId)
            val snapshot = userTasksRef.get().await()
            val tasks = snapshot.children.mapNotNull { taskSnapshot ->
                val taskProjectId = taskSnapshot.child("projectId").getValue(String::class.java)
                if (taskProjectId == projectId) {
                    taskSnapshot.key
                } else {
                    null
                }
            }
            Result.success(tasks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteUserProject(userId: String, projectId: String): Result<Unit> {
        return try {
            database.getReference("userProjects")
                .child(userId)
                .child(projectId)
                .removeValue()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLastProjectMessage(projectId: String): Result<ProjectMessage?> {
        return try {
            val snapshot = database.getReference("projectMessages")
                .child(projectId)
                .orderByChild("timestamp")
                .limitToLast(1)
                .get()
                .await()

            if (snapshot.children.iterator().hasNext()) {
                val childSnapshot = snapshot.children.first()
                val message = childSnapshot.getValue(ProjectMessage::class.java)
                if (message != null) {
                    message.id = childSnapshot.key
                    Result.success(message)
                } else {
                    Result.success(null)
                }
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserProject(userId: String, projectId: String): Result<UserProject> {
        return try {
            val userProjectRef =
                database.getReference("userProjects").child(userId).child(projectId)
            val snapshot = userProjectRef.get().await()
            val userProject = snapshot.getValue(UserProject::class.java)
            if (userProject != null) {
                userProject.id = projectId
                Result.success(userProject)
            } else {
                Result.failure(NullPointerException("Project data is null"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProject(projectId: String): Result<Project> {
        return try {
            val snapshot = database.getReference("projects")
                .child(projectId)
                .get()
                .await()
            val project = snapshot.getValue(Project::class.java)
            if (project != null) {
                project.id = projectId
                Result.success(project)
            } else {
                Result.failure(NullPointerException("Project data is null"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserProject(
        userId: String,
        projectId: String,
        updates: Map<String, Any?>
    ): Result<Unit> {
        return try {
            database.getReference("userProjects").child(userId).child(projectId)
                .updateChildren(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProjectMessage(
        projectId: String,
        messageId: String,
        updates: Map<String, Any?>
    ): Result<Unit> {
        return try {
            database.getReference("projectMessages").child(projectId).child(messageId)
                .updateChildren(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProjectTyping(projectId: String, updates: Map<String, Any?>): Result<Unit> {
        return try {
            database.getReference("projects").child(projectId).child("typing")
                .updateChildren(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProjectIdea(
        projectId: String,
        ideaId: String,
        updates: Map<String, Any?>
    ): Result<Unit> {
        return try {
            database.getReference("projectIdeas").child(projectId).child(ideaId)
                .updateChildren(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProjectTask(
        projectId: String,
        taskId: String,
        updates: Map<String, Any?>
    ): Result<Unit> {
        return try {
            database.getReference("projectTasks").child(projectId).child(taskId)
                .updateChildren(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProjectMessageSeenByCount(projectId: String, messageId: String): Result<Int?> {
        return try {
            val projectMessagesSeenByRef =
                database.getReference("projectMessagesSeenBy").child(projectId).child(messageId)
            val snapshot = projectMessagesSeenByRef.get().await()
            val count = snapshot.childrenCount.toInt()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveMessageSeenBy(
        projectId: String,
        messageId: String,
        userId: String
    ): Result<String?> {
        return try {
            val newSeenByRef =
                database.getReference("projectMessagesSeenBy").child(projectId).child(messageId)
                    .child(userId)
            newSeenByRef.setValue(true).await()
            Result.success(newSeenByRef.key)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveMessage(projectId: String, messageData: Map<String, Any?>): Result<String?> {
        return try {
            val newMessageRef = database.getReference("projectMessages").child(projectId).push()
            newMessageRef.setValue(messageData).await()
            Result.success(newMessageRef.key)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveProject(projectData: Map<String, Any?>): Result<String?> {
        return try {
            val newProjectRef = database.getReference("projects").push()
            newProjectRef.setValue(projectData).await()
            Result.success(newProjectRef.key)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveProjectTask(
        projectId: String,
        projectTaskData: Map<String, Any?>
    ): Result<String?> {
        return try {
            val newTaskProjectRef = database.getReference("projectTasks").child(projectId).push()
            newTaskProjectRef.setValue(projectTaskData).await()
            Result.success(newTaskProjectRef.key)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveProjectIdea(
        projectId: String,
        projectIdeaData: Map<String, Any?>
    ): Result<String?> {
        return try {
            val newIdeaProjectRef = database.getReference("projectIdeas").child(projectId).push()
            newIdeaProjectRef.setValue(projectIdeaData).await()
            Result.success(newIdeaProjectRef.key)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProjectFile(
        projectId: String,
        fileKey: String,
        fileData: Map<String, Any?>
    ): Result<Unit> {
        return try {
            database.getReference("projectFiles").child(projectId).child(fileKey)
                .updateChildren(fileData)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProjectMedia(
        projectId: String,
        mediaKey: String,
        mediaData: Map<String, Any?>
    ): Result<Unit> {
        return try {
            database.getReference("projectMedia").child(projectId).child(mediaKey)
                .updateChildren(mediaData)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProjectMessageMedia(
        projectId: String,
        messageId: String,
        mediaKey: String,
        mediaData: Map<String, Any?>
    ): Result<Unit> {
        return try {
            database.getReference("projectMessages").child(projectId).child(messageId)
                .child("media").child(mediaKey)
                .updateChildren(mediaData)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveProjectFile(
        projectId: String,
        projectFileData: Map<String, Any?>
    ): Result<String?> {
        return try {
            val newFileProjectRef = database.getReference("projectFiles").child(projectId).push()
            newFileProjectRef.setValue(projectFileData).await()
            Result.success(newFileProjectRef.key)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveProjectMedia(
        projectId: String,
        projectMediaData: Map<String, Any?>
    ): Result<String?> {
        return try {
            val newMediaProjectRef = database.getReference("projectMedia").child(projectId).push()
            newMediaProjectRef.setValue(projectMediaData).await()
            Result.success(newMediaProjectRef.key)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProject(projectId: String, updates: Map<String, Any?>): Result<Unit> {
        return try {
            database.getReference("projects").child(projectId)
                .updateChildren(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getProjectFlow(projectId: String): Flow<Result<Project>> =
        database.getReference("projects").child(projectId).snapshots
            .map { snapshot ->
                snapshot.getValue(Project::class.java)?.let { projectData ->
                    Result.success(projectData)
                } ?: Result.failure(Exception(Constants.ERROR_UNKNOWN))
            }
            .catch { e -> emit(Result.failure(e)) }

    fun getProjectMessagesFlow(projectId: String): Flow<Result<List<ProjectMessage>>> =
        database.getReference("projectMessages").child(projectId)
            .orderByChild("timestamp").snapshots
            .map { snapshot ->
                val projectMessages = snapshot.children.mapNotNull { child ->
                    child.getValue(ProjectMessage::class.java)?.apply { id = child.key }
                }
                if (projectMessages.isNotEmpty()) {
                    Result.success(projectMessages)
                } else {
                    Result.success(emptyList())
                }
            }
            .catch { e -> emit(Result.failure(e)) }

    fun getProjectTasksFlow(projectId: String): Flow<Result<List<ProjectTask>>> =
        database.getReference("projectTasks")
            .child(projectId)
            .orderByChild("timestamp")
            .snapshots
            .map { snapshot ->
                val projectTasks = snapshot.children.mapNotNull { child ->
                    child.getValue(ProjectTask::class.java)?.apply { id = child.key }
                }
                if (projectTasks.isNotEmpty()) {
                    val sortedTasks = projectTasks.sortedWith(
                        compareBy<ProjectTask> { it.completed == true }
                            .thenByDescending { it.created }
                    )
                    Result.success(sortedTasks)
                } else {
                    Result.success(emptyList())
                }
            }
            .catch { e -> emit(Result.failure(e)) }

    fun getProjectIdeasFlow(projectId: String): Flow<Result<List<ProjectIdea>>> =
        database.getReference("projectIdeas")
            .child(projectId)
            .orderByChild("timestamp")
            .snapshots
            .map { snapshot ->
                val projectIdeas = snapshot.children.mapNotNull { child ->
                    child.getValue(ProjectIdea::class.java)?.apply { id = child.key }
                }
                val sortedIdeas = projectIdeas.sortedByDescending { idea ->
                    idea.votes?.count { it.value } ?: 0
                }
                if (sortedIdeas.isNotEmpty()) {
                    Result.success(sortedIdeas)
                } else {
                    Result.success(emptyList())
                }
            }
            .catch { e -> emit(Result.failure(e)) }

    fun getProjectMediaFlow(projectId: String): Flow<Result<List<ProjectMedia>>> =
        database.getReference("projectMedia").child(projectId).orderByKey().snapshots
            .map { snapshot ->
                val projectMedia = snapshot.children.mapNotNull { child ->
                    child.getValue(ProjectMedia::class.java)?.apply { id = child.key }
                }
                if (projectMedia.isNotEmpty()) {
                    Result.success(projectMedia)
                } else {
                    Result.success(emptyList())
                }
            }
            .catch { e -> emit(Result.failure(e)) }

}
