package dev.faststats.proguard

import com.google.gson.Gson
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant

@DisableCachingByDefault(because = "Uploads files to a remote server")
abstract class UploadProguardMappingsTask : DefaultTask() {

    @get:Input
    abstract val authToken: Property<String>

    @get:Input
    abstract val endpoint: Property<String>

    @get:Input
    abstract val buildId: Property<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val mappingFiles: ConfigurableFileCollection

    companion object {
        private const val MAX_UPLOAD_BODY_BYTES = 50L * 1024 * 1024 // 50MB
    }

    @TaskAction
    fun upload() {
        val token = authToken.orNull
        if (token.isNullOrBlank()) {
            throw GradleException("No auth token configured. Set authToken or FASTSTATS_AUTH_TOKEN env var.")
        }

        val allConfigured = mappingFiles.files
        val files = allConfigured.filter { it.exists() && it.isFile }
        if (files.isEmpty()) {
            val searched = allConfigured.joinToString("\n  - ") { it.absolutePath }
            throw GradleException(
                "No ProGuard mapping files found.\nSearched paths:\n  - $searched\n\n" +
                        """
                        Configure the mapping file location in your build script:
                          mappingsUpload {
                              mappingFiles.from(layout.buildDirectory.file("proguard/mapping.txt"))
                          }
                        """.trimIndent()
            )
        }

        val resolvedBuildId = buildId.get()
        val batches = files.flatMap { file -> createBatches(resolvedBuildId, file) }

        logger.lifecycle("Uploading ${files.size} ProGuard mapping file(s) in ${batches.size} batch(es) (buildId=$resolvedBuildId)")

        val gson = Gson()
        val client = HttpClient.newHttpClient()

        for ((index, batch) in batches.withIndex()) {
            val json = gson.toJson(batch)
            logger.lifecycle("Sending batch ${index + 1}/${batches.size} (${json.toByteArray().size} bytes)")

            val request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint.get()))
                .header("content-type", "application/json")
                .header("authorization", "Bearer $token")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build()

            try {
                val response = client.send(request, HttpResponse.BodyHandlers.ofString())
                if (response.statusCode() !in 200..299) {
                    throw GradleException("Mapping upload failed with status ${response.statusCode()}: ${response.body()}")
                }
            } catch (e: GradleException) {
                throw e
            } catch (e: Exception) {
                throw GradleException("Mapping upload failed: ${e.message}", e)
            }
        }

        logger.lifecycle("Successfully uploaded ${files.size} mapping file(s) in ${batches.size} batch(es).")
    }

    private fun splitIntoClassSections(content: String): List<String> {
        val sections = mutableListOf<String>()
        val current = StringBuilder()

        for (line in content.lineSequence()) {
            if (line.isNotEmpty() && !line[0].isWhitespace() && current.isNotEmpty()) {
                sections.add(current.toString())
                current.clear()
            }
            if (current.isNotEmpty()) current.append('\n')
            current.append(line)
        }

        if (current.isNotEmpty()) {
            sections.add(current.toString())
        }

        return sections
    }

    private fun createBatches(
        resolvedBuildId: String,
        file: File,
    ): List<SourcemapUploadPayload> {
        val sections = splitIntoClassSections(file.readText())
        val uploadedAt = Instant.now().toString()
        val gson = Gson()

        val baseName = file.nameWithoutExtension
        val extension = file.extension
        var batchIndex = 0

        fun batchFileName() = "${baseName}/${batchIndex + 1}.${extension}"

        fun toPayload(sectionContent: String, fileName: String) = SourcemapUploadPayload(
            buildId = resolvedBuildId,
            uploadedAt = uploadedAt,
            files = listOf(SourcemapUpload(fileName = fileName, content = sectionContent)),
        )

        fun payloadSize(sectionContent: String): Long = gson.toJson(toPayload(sectionContent, file.name))
            .toByteArray(Charsets.UTF_8).size.toLong()

        val batches = mutableListOf<SourcemapUploadPayload>()
        val currentSections = StringBuilder()

        for (section in sections) {
            val candidate = if (currentSections.isEmpty()) section else "${currentSections}\n${section}"

            if (payloadSize(candidate) <= MAX_UPLOAD_BODY_BYTES) {
                currentSections.clear()
                currentSections.append(candidate)
                continue
            }

            if (currentSections.isEmpty()) {
                if (payloadSize(section) > MAX_UPLOAD_BODY_BYTES) {
                    throw GradleException("Single class mapping section in ${file.name} exceeds the 50MB upload limit.")
                }
            }

            batches.add(toPayload(currentSections.toString(), batchFileName()))
            batchIndex++
            currentSections.clear()
            currentSections.append(section)

            if (payloadSize(section) > MAX_UPLOAD_BODY_BYTES) {
                throw GradleException("Single class mapping section in ${file.name} exceeds the 50MB upload limit.")
            }
        }

        if (currentSections.isNotEmpty()) {
            batches.add(toPayload(currentSections.toString(), batchFileName()))
        }

        return batches
    }
}
