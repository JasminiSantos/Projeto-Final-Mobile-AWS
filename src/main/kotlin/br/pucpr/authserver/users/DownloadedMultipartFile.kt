package br.pucpr.authserver.users

import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.io.File

class DownloadedMultipartFile(
    private val fieldName: String,
    private val originalFilename: String,
    private val contentType: String,
    private val bytes: ByteArray
) : MultipartFile {
    override fun getName() = fieldName
    override fun getOriginalFilename() = originalFilename
    override fun getContentType() = contentType
    override fun isEmpty() = bytes.isEmpty()
    override fun getSize() = bytes.size.toLong()
    override fun getBytes() = bytes
    override fun getInputStream() = ByteArrayInputStream(bytes)
    override fun transferTo(dest: File) = dest.writeBytes(bytes)
}
