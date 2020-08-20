package ru.mipt.npm.gradle

import org.gradle.api.Project
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.repositories

class SerializationTargets(
    val sourceSet: DependencySourceSet,
    val configuration: DependencyConfiguration
) {
    fun Project.cbor(
        version: String = KScienceVersions.serializationVersion
    ) {
        useCommonDependency(
            "org.jetbrains.kotlinx:kotlinx-serialization-cbor:$version",
            dependencySourceSet = sourceSet,
            dependencyConfiguration = configuration
        )
    }

    fun Project.protobuf(
        version: String = KScienceVersions.serializationVersion
    ) {
        useCommonDependency(
            "org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$version",
            dependencySourceSet = sourceSet,
            dependencyConfiguration = configuration
        )
    }

    fun Project.xml(
        version: String = KScienceVersions.Serialization.xmlVersion
    ) {
        repositories {
            maven("https://dl.bintray.com/pdvrieze/maven")
        }
        useDependency(
            "common" to "net.devrieze:xmlutil-serialization:$version",
            "jvm" to "net.devrieze:xmlutil-serialization:$version",
            "js" to "net.devrieze:xmlutil-serialization:$version",
            dependencySourceSet = sourceSet,
            dependencyConfiguration = configuration
        )
    }

    fun Project.yaml(
        version: String = KScienceVersions.Serialization.yamlVersion
    ) {
        useDependency(
            "jvm" to "com.charleskorn.kaml:kaml:$version",
            dependencySourceSet = sourceSet,
            dependencyConfiguration = configuration
        )
    }

    fun Project.bson(
        version: String = KScienceVersions.Serialization.bsonVersion
    ) {
        useDependency(
            "jvm" to "com.github.jershell:kbson:$version",
            dependencySourceSet = sourceSet,
            dependencyConfiguration = configuration
        )
    }
}
