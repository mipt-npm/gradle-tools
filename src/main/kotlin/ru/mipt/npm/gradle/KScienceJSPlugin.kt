package ru.mipt.npm.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.getting
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension

open class KScienceJSPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        plugins.apply("org.jetbrains.kotlin.js")
        registerKScienceExtension()

        repositories.applyRepos()

        configure<KotlinJsProjectExtension> {
            explicitApiWarning()
            js(IR) {
                browser()
                nodejs()
            }
            sourceSets["main"].apply {
                languageSettings.applySettings()

                dependencies {
                    api(kotlin("stdlib-js"))
                }
            }

            sourceSets["test"].apply {
                languageSettings.applySettings()
                dependencies {
                    implementation(kotlin("test-js"))
                }
            }
        }

        tasks.apply {

            val processResources by getting(Copy::class)
            processResources.copyJSResources(configurations["runtimeClasspath"])
        }
    }

}