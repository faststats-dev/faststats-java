import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

/**
 * Lets only one publish task run at a time.
 *
 * The Minecraft version projects of a platform publish the same artifact in different versions,
 * publishing them in parallel corrupts the shared maven-metadata.xml.
 */
abstract class PublishingLock : BuildService<BuildServiceParameters.None>
