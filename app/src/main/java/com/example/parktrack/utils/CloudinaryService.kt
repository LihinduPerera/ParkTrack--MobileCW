package com.example.parktrack.utils

import android.content.Context
import android.net.Uri
import com.cloudinary.Cloudinary
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.parktrack.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class CloudinaryService @Inject constructor(
    private val context: Context
) {
    companion object {
        // Credentials are loaded from local.properties via BuildConfig
        val CLOUD_NAME: String = BuildConfig.CLOUDINARY_CLOUD_NAME
        val API_KEY: String = BuildConfig.CLOUDINARY_API_KEY
        val API_SECRET: String = BuildConfig.CLOUDINARY_API_SECRET
        val UPLOAD_PRESET: String = BuildConfig.CLOUDINARY_UPLOAD_PRESET
    }

    init {
        // Validate credentials are set
        if (CLOUD_NAME.isBlank() || API_KEY.isBlank() || API_SECRET.isBlank()) {
            throw IllegalStateException(
                "Cloudinary credentials not configured. " +
                "Please add them to local.properties file. " +
                "See PROFILE_PICTURE_SETUP.md for instructions."
            )
        }
        
        // Initialize Cloudinary only if not already initialized
        try {
            val config = HashMap<String, String>()
            config["cloud_name"] = CLOUD_NAME
            config["api_key"] = API_KEY
            config["api_secret"] = API_SECRET
            MediaManager.init(context, config)
        } catch (e: Exception) {
            // Already initialized
        }
    }

    /**
     * Upload profile image to Cloudinary
     * @param userId The user ID to use as part of the public_id
     * @param imageUri The URI of the image to upload
     * @return The secure URL of the uploaded image
     */
    suspend fun uploadProfileImage(userId: String, imageUri: Uri): String {
        return withContext(Dispatchers.IO) {
            try {
                // Convert URI to File
                val file = uriToFile(imageUri)
                    ?: throw Exception("Failed to convert URI to file")

                // Create a unique public_id for the image
                val publicId = "profile_${userId}_${System.currentTimeMillis()}"

                // Upload to Cloudinary
                suspendCancellableCoroutine<String> { continuation ->
                    val request = MediaManager.get().upload(file.path)
                        .unsigned(UPLOAD_PRESET)
                        .option("public_id", publicId)
                        .option("folder", "parktrack/profiles")
                        .callback(object : UploadCallback {
                            override fun onStart(requestId: String?) {}
                            
                            override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
                            
                            override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                                val secureUrl = resultData?.get("secure_url") as? String
                                if (secureUrl != null) {
                                    continuation.resume(secureUrl)
                                } else {
                                    continuation.resumeWithException(Exception("No secure URL in response"))
                                }
                            }
                            
                            override fun onError(requestId: String?, error: ErrorInfo?) {
                                continuation.resumeWithException(
                                    Exception(error?.description ?: "Upload failed")
                                )
                            }
                            
                            override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                                continuation.resumeWithException(
                                    Exception(error?.description ?: "Upload rescheduled")
                                )
                            }
                        })
                        .dispatch()
                }
            } catch (e: Exception) {
                throw Exception("Cloudinary upload failed: ${e.message}")
            }
        }
    }

    /**
     * Delete image from Cloudinary
     * @param imageUrl The URL of the image to delete
     */
    suspend fun deleteImage(imageUrl: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Extract public_id from URL
                val publicId = extractPublicIdFromUrl(imageUrl)
                    ?: return@withContext Result.failure(Exception("Could not extract public ID"))

                // Note: For deletion, you typically need server-side authentication
                // This is a placeholder - in production, use a server-side function
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Convert URI to temporary File
     */
    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return null
            
            val tempFile = File.createTempFile("profile_", ".jpg", context.cacheDir)
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            inputStream.close()
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Extract public_id from Cloudinary URL
     */
    private fun extractPublicIdFromUrl(url: String): String? {
        // Cloudinary URL format: https://res.cloudinary.com/cloud_name/image/upload/v1234567890/folder/public_id.jpg
        val regex = Regex("/upload/(?:v\\d+/)?(.+)\\.")
        val matchResult = regex.find(url)
        return matchResult?.groupValues?.get(1)
    }
}
