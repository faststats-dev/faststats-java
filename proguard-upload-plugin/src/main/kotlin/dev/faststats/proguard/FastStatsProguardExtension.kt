package dev.faststats.proguard

import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class FastStatsProguardExtension @Inject constructor(objects: ObjectFactory) {

    val authToken: Property<String> = objects.property(String::class.java)

    val endpoint: Property<String> = objects.property(String::class.java)
        .convention("https://sourcemaps.faststats.dev/v0/upload")

    val buildId: Property<String> = objects.property(String::class.java)

    val proguardTask: Property<Task> = objects.property(Task::class.java)

    val mappingFiles: ConfigurableFileCollection = objects.fileCollection()
}
