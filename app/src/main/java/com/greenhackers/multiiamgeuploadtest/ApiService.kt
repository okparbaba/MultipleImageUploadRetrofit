package com.greenhackers.multiiamgeuploadtest

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part



interface ApiService {
    @Multipart
    @POST("upload.php")
    fun uploadMultiple(
        @Part("description") description: RequestBody,
        @Part("size") size: RequestBody,
        @Part files: List<MultipartBody.Part>
    ): Call<ResponseBody>
}
