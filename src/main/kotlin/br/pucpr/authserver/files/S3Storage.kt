package br.pucpr.authserver.files

import br.pucpr.authserver.users.User
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.transfer.TransferManagerBuilder
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

@Component
class S3Storage : FileStorage {
    private val region = System.getenv("AWS_REGION") ?: "us-east-2"
    private val prefix = "https://$BUCKET.s3.$region.amazonaws.com"

    private val s3: AmazonS3 = AmazonS3ClientBuilder.standard()
        .withRegion(Regions.fromName(region))
        .withCredentials(EnvironmentVariableCredentialsProvider())
        .build()

    override fun save(user: User, path: String, file: MultipartFile) {
        val contentType = file.contentType!!

        val meta = ObjectMetadata()
        meta.contentType = contentType
        meta.contentLength = file.size
        meta.userMetadata["userId"] = "${user.id}"
        meta.userMetadata["originalFilename"] = file.originalFilename

        val transferManager = TransferManagerBuilder.standard()
            .withS3Client(s3)
            .build()
        transferManager
            .upload(BUCKET, path, file.inputStream, meta)
            .waitForUploadResult()
    }

    override fun load(path: String): Resource? = InputStreamResource(
        s3.getObject(BUCKET, path.replace("--", "/")).objectContent
    )

    override fun urlFor(name: String) = "$prefix/$name"

    companion object {
        private const val BUCKET = "jasmini-authserver-avatars"
    }
}