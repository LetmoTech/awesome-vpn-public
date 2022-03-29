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
import org.json.JSONObject
import org.kamikadzy.awesomevpn.db.user.UserService
import org.kamikadzy.awesomevpn.utils.OkHttpUtils
import org.kamikadzy.awesomevpn.utils.QRGenerator
import org.kamikadzy.awesomevpn.utils.kassert
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.File
import java.io.FileOutputStream
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.annotation.PostConstruct
import javax.imageio.ImageIO
import kotlin.io.path.createDirectory

class NetmakerAPIException(s: String) : Exception(s)

@Component
class NetmakerAPI(
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
    private val requestsQueue = Collections.synchronizedList(LinkedList<suspend () -> Unit>())
    private var isCreatingUsers = AtomicBoolean(false)

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

        var response = OkHttpUtils.makeAsyncRequest(
            client = httpClient,
            request = request
        ) ?: throw NetmakerAPIException("Bad code")

        if (response.trim() == "null") {
            response = "[]"
        }

        val jsonArray = JSONArray(response)
        val userNames = arrayListOf<String>()

        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)

            userNames.add(jsonObject.getString("clientid"))
        }

        return userNames
    }

    suspend fun deleteUser(tgId: Long) {
        val createRequest = Request.Builder()
            .url("${apiUrl}/vpn/${tgId}")
            .delete("".toRequestBody())
            .build()

        OkHttpUtils.makeAsyncRequest(httpClient, createRequest) ?: throw NetmakerAPIException("Bad code")
        userService.setRegisteredById(tgId, false)

        if (File("configs/$tgId.conf").exists()) {
            withContext(Dispatchers.IO) {
                File("configs/$tgId.conf").delete()
            }
        }

        if (File("qrs/$tgId.png").exists()) {
            withContext(Dispatchers.IO) {
                File("qrs/$tgId.png").delete()
            }
        }
    }

    suspend fun enableUser(tgId: Long) {
        val json = JSONObject(changeUserValue(prevName = tgId.toString(), enabled = true))
        userService.setActiveById(tgId, json.getBoolean("enabled"))
    }

    suspend fun disableUser(tgId: Long) {
        val json = JSONObject(changeUserValue(prevName = tgId.toString(), enabled = false))
        userService.setActiveById(tgId, json.getBoolean("enabled"))
    }

    private suspend fun changeUserValue(
        prevName: String,
        newName: String = prevName,
        enabled: Boolean = false
    ): String {
        val updateNameRequest = Request.Builder()
            .url("${apiUrl}/vpn/${prevName}")
            .put("{\"clientid\": \"$newName\", \"enabled\": $enabled}".toRequestBody(OkHttpUtils.JSON_TYPE))
            .build()

        return OkHttpUtils.makeAsyncRequest(httpClient, updateNameRequest) ?: throw NetmakerAPIException("Bad code")
    }

    private suspend fun createUserByAPI() {
        val createRequest = Request.Builder()
            .url(apiCreateUserUrl)
            .post("".toRequestBody())
            .build()

        OkHttpUtils.makeAsyncRequest(httpClient, createRequest) ?: throw NetmakerAPIException("Bad code")
    }

    suspend fun createUser(tgId: Long) {
        kassert(!(userService.getUserByTgId(tgId)?.isRegistered ?: false), NetmakerAPIException("Already registered"))

        synchronized(requestsQueue) {
            requestsQueue.add {
                val usersBefore = getUserNamesList().toSet()
                createUserByAPI()
                val usersAfter = getUserNamesList().toSet()
                val userName = (usersAfter - usersBefore).first()

                changeUserValue(userName, tgId.toString())
            }
        }

        if (!isCreatingUsers.get()) {
            GlobalScope.launch {
                isCreatingUsers.set(true)

                while (requestsQueue.isNotEmpty()) {
                    try {
                        synchronized(requestsQueue) {
                            requestsQueue.removeFirst()
                        }.invoke()

                        userService.setRegisteredById(tgId, true)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                isCreatingUsers.set(false)
            }
        }
    }

    suspend fun getUserConf(tgId: Long): File {
        val file = File("configs/${tgId}.conf")

        if (!file.exists()) {
            withContext(Dispatchers.IO) { file.createNewFile() }
            val request = Request.Builder()
                .url("${apiUrl}/vpn/${tgId}/file")
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
                    outputStream.write(buffer, 0, read)
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

    suspend fun getQrCode(tgId: Long): File {
        val file = File("qrs/${tgId}.png")

        if (!file.exists()) {
            val config = getUserConf(tgId)

            withContext(Dispatchers.IO) {
                ImageIO.write(qrGenerator.getCodeForConfig(config.readText()), "png", file)
            }
        }

        return file
    }
}