package org.kamikadzy.awesomevpn.domain.netmaker

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.kamikadzy.awesomevpn.db.user.UserService
import org.kamikadzy.awesomevpn.utils.OkHttpUtils
import org.kamikadzy.awesomevpn.utils.QRGenerator
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.File
import java.io.FileOutputStream
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.annotation.PostConstruct
import javax.imageio.ImageIO
import kotlin.collections.ArrayDeque
import kotlin.io.path.createDirectory

class NetmakerAPIException(s: String) : Exception(s)

@Component
class NetmakerAPI (
    @Value("\${netmaker.token}")
    val token: String,
    @Value("\${netmaker.api-url}")
    val apiUrl: String,
    @Value("\${netmaker.api-create-user-url}")
    val apiCreateUserUrl: String,
    val userService: UserService,
    val qrGenerator: QRGenerator
        ) {
    private val httpClient = OkHttpClient.Builder()
            .addNetworkInterceptor(TokenInterceptor())
            .connectTimeout(Duration.ofMinutes(1))
            .build()
    private val requestsQueue: ArrayDeque<suspend () -> Unit> = Collections.synchronizedCollection(ArrayDeque<suspend () -> Unit>()) as ArrayDeque<suspend () -> Unit>
    private var isCreatingUsers = AtomicBoolean()

    private inner class TokenInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val newReq = chain.request()
                .newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
            return chain.proceed(newReq)
        }
    }

    @PostConstruct
    private fun ensureFolders() {
        val configs = File("configs")
        val qrs = File("qrs")

        if (!configs.exists()) {
            configs.toPath().createDirectory()
        }

        if (!qrs.exists()) {
            qrs.toPath().createDirectory()
        }
    }

    private suspend fun getUserNamesList(): List<String> {
        val request = Request.Builder()
            .url(apiUrl)
            .get()
            .build()

        val response = OkHttpUtils.makeAsyncRequest(
            client = httpClient,
            request = request
        ) ?: throw NetmakerAPIException("Bad code")

        val jsonArray = JSONArray(response)
        val userNames = arrayListOf<String>()

        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)

            userNames.add(jsonObject.getString("clientid"))
        }

        return userNames
    }

     suspend fun deleteUserByUsername(userName: String) {
        val createRequest = Request.Builder()
                .url("${apiUrl}/vpn/${userName}")
                .delete("".toRequestBody())
                .build()

        OkHttpUtils.makeAsyncRequest(httpClient, createRequest) ?: throw NetmakerAPIException("Bad code")
    }

    suspend fun changeUserValue(prevName: String, newName: String = prevName, enabled: Boolean = false) {
        val updateNameRequest = Request.Builder()
            .url("${apiUrl}/vpn/${prevName}")
            .post("{\"clientid\": \"$newName\", \"enabled\": $enabled}".toRequestBody(OkHttpUtils.JSON_TYPE))
            .build()

        OkHttpUtils.makeAsyncRequest(httpClient, updateNameRequest) ?: throw NetmakerAPIException("Bad code")
    }

    private suspend fun createUserByAPI() {
        val createRequest = Request.Builder()
            .url(apiCreateUserUrl)
            .post("".toRequestBody())
            .build()

        OkHttpUtils.makeAsyncRequest(httpClient, createRequest) ?: throw NetmakerAPIException("Bad code")
    }

    suspend fun createUser(id: Long) {
        synchronized(requestsQueue) {
            requestsQueue.add {
                val usersBefore = getUserNamesList().toSet()
                createUserByAPI()
                val usersAfter = getUserNamesList().toSet()
                val userName = (usersAfter - usersBefore).first()

                changeUserValue(userName, id.toString())
            }
        }

        if (!isCreatingUsers.get()) {
            GlobalScope.launch {
                isCreatingUsers.set(true)

                while (requestsQueue.isEmpty()) {
                    synchronized(requestsQueue) {
                        requestsQueue.removeFirst()
                    }.invoke()

                    userService.setRegistratedById(id, true)
                }

                isCreatingUsers.set(true)
            }
        }
    }

    suspend fun getUserConf(id: Long): File {
        val file = File("configs/${id}.conf")

        if (!file.exists()) {
            withContext(Dispatchers.IO) { file.createNewFile() }
            val request = Request.Builder()
                .url("${apiUrl}/vpn/${id}")
                .get()
                .build()
            val call = OkHttpUtils.makeAsyncRequestRaw(httpClient, request)
            val inputStream = call.body?.byteStream() ?: kotlin.run {
                call.body?.close()
                call.close()

                throw NetmakerAPIException("Bad source")
            }

            val buffer = ByteArray(1024)
            val outputStream = withContext(Dispatchers.IO) {
                FileOutputStream(file)
            }

            while (true) {
                val read = withContext(Dispatchers.IO) {
                    inputStream.read(buffer)
                }

                if (read == -1) {
                    break
                }

                withContext(Dispatchers.IO) {
                    outputStream.write(buffer)
                }
            }

            withContext(Dispatchers.IO) {
                outputStream.flush()
                outputStream.close()
                inputStream.close()
            }

            call.body?.close()
            call.close()
        }

        return file
    }
    
    suspend fun getQrCode(id: Long): File {
        val file = File("qrs/${id}.png")

        if (!file.exists()) {
            val config = getUserConf(id)

            withContext(Dispatchers.IO) {
                ImageIO.write(qrGenerator.getCodeForConfig(config.readText()), "png", file)
            }
        }

        return file
    }
}