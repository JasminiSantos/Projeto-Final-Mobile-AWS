package br.pucpr.authserver.users

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

@Service
class GravatarService(val avatarService: AvatarService) {

    fun fetchAndSave(user: User): String {
        val (bytes, contentType) = tryGravatar(user.email) ?: fetchUiAvatar(user.name)
        val cleanContentType = contentType.split(";").first().trim()
        val ext = if (cleanContentType == "image/png") "png" else "jpg"
        val file = DownloadedMultipartFile(
            fieldName = "avatar",
            originalFilename = "avatar.$ext",
            contentType = cleanContentType,
            bytes = bytes
        )
        return avatarService.save(user, file)
    }

    private fun tryGravatar(email: String): Pair<ByteArray, String>? {
        val hash = md5(email.lowercase().trim())
        val conn = URI("https://www.gravatar.com/avatar/$hash?d=404&s=200")
            .toURL()
            .openConnection() as HttpURLConnection
        conn.instanceFollowRedirects = true
        conn.connect()
        return if (conn.responseCode == 200) {
            val ct = conn.contentType ?: "image/jpeg"
            log.info("Gravatar found for hash $hash (content-type: $ct)")
            conn.inputStream.readBytes() to ct
        } else {
            log.info("No Gravatar for hash $hash (HTTP ${conn.responseCode}), falling back to ui-avatars")
            conn.disconnect()
            null
        }
    }

    private fun fetchUiAvatar(name: String): Pair<ByteArray, String> {
        val encoded = URLEncoder.encode(name, StandardCharsets.UTF_8)
        val conn = URI("https://ui-avatars.com/api/?name=$encoded&format=png&size=200&background=random&bold=true")
            .toURL()
            .openConnection() as HttpURLConnection
        conn.instanceFollowRedirects = true
        conn.connect()
        check(conn.responseCode == 200) { "ui-avatars.com returned HTTP ${conn.responseCode}" }
        log.info("ui-avatars avatar generated for name '$name'")
        return conn.inputStream.readBytes() to "image/png"
    }

    private fun md5(input: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(input.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private val log = LoggerFactory.getLogger(GravatarService::class.java)
    }
}
