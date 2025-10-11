package dev.iiahmed.recraft

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class RecraftExtention @Inject constructor(objects: ObjectFactory) {

    val minecraftVersion: Property<String> = objects.property(String::class.java)
    val baselineMajor: Property<Int> = objects.property(Int::class.java).convention(52) // Java 8 default

    val jarFilePattern: Property<String> = objects.property(String::class.java)
    val paperPrefix: Property<String> = objects.property(String::class.java).convention("P")
    val spigotPrefix: Property<String> = objects.property(String::class.java).convention("S")

    val targetedPackages: ListProperty<String> = objects.listProperty(String::class.java).convention(emptyList())
}
