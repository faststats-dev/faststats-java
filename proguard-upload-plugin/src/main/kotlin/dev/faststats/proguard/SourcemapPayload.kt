package dev.faststats.proguard

data class SourcemapUpload(
    val fileName: String,
    val content: String,
)

data class SourcemapUploadPayload(
    val type: String = "proguard",
    val buildId: String,
    val uploadedAt: String,
    val files: List<SourcemapUpload>,
)
