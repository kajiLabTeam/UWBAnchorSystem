package net.harutiro.uwbanchorsystemminio.feature.http

import android.content.Context
import android.util.Log
import net.harutiro.uwbanchorsystemminio.BuildConfig
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File

class MinioApiClient(private val context: Context) {
    private val TAG = "MinioApiClient"
    private val BASE_URL = BuildConfig.MINIO_API_BASE_URL
    private val USERNAME = BuildConfig.MINIO_API_USERNAME
    private val PASSWORD = BuildConfig.MINIO_API_PASSWORD

    private val client: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            setLevel(HttpLoggingInterceptor.Level.BODY)
        }

        OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(BasicAuthInterceptor(USERNAME, PASSWORD))
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val apiService: MinioApiService by lazy {
        retrofit.create(MinioApiService::class.java)
    }

    fun uploadFile(file: File, bucket: String, path: String, callback: (Boolean, String?) -> Unit) {
        val requestBody = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
        val filePart = MultipartBody.Part.createFormData("file", file.name, requestBody)

        // テキストとして送信するように変更
        val bucketPart = MultipartBody.Part.createFormData("bucket", bucket)
        val pathPart = MultipartBody.Part.createFormData("path", path)

        val call = apiService.uploadFile(bucketPart, pathPart, filePart)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val responseString = response.body()?.string()
                    Log.d(TAG, "Upload successful: $responseString")
                    callback(true, responseString)
                } else {
                    val errorMessage = "Upload failed: ${response.code()} - ${response.message()}"
                    Log.e(TAG, errorMessage)
                    callback(false, errorMessage)
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                val errorMessage = "Upload error: ${t.message}"
                Log.e(TAG, errorMessage, t)
                callback(false, errorMessage)
            }
        })
    }
} 