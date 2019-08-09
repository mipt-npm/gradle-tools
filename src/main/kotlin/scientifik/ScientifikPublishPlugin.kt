package scientifik

import com.jfrog.bintray.gradle.BintrayExtension
import com.jfrog.bintray.gradle.tasks.BintrayUploadTask
import groovy.lang.GroovyObject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.internal.artifact.FileBasedMavenArtifact
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.kotlin.dsl.*
import org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention
import org.jfrog.gradle.plugin.artifactory.dsl.PublisherConfig
import org.jfrog.gradle.plugin.artifactory.dsl.ResolverConfig
import org.jfrog.gradle.plugin.artifactory.task.ArtifactoryTask


open class ScientifikPublishPlugin : Plugin<Project> {

    override fun apply(project: Project) {

        project.plugins.apply("maven-publish")

        project.run {
            val bintrayRepo = if (project.version.toString().contains("dev")) {
                "dev"
            } else {
                findProperty("bintrayRepo") as? String
            }

            val githubProject = findProperty("githubProject") as? String
            val vcs = findProperty("vcs") as? String
                ?: githubProject?.let { "https://github.com/mipt-npm/$it" }

            if (vcs == null) {
                project.logger.warn("[${project.name}] Missing deployment configuration. Skipping publish.")
                return@apply
            }

            project.plugins.apply("com.jfrog.bintray")
            project.plugins.apply("com.jfrog.artifactory")

            project.configure<PublishingExtension> {
                // Process each publication we have in this project
                publications.filterIsInstance<MavenPublication>().forEach { publication ->

                    @Suppress("UnstableApiUsage")
                    publication.pom {
                        name.set(project.name)
                        description.set(project.description)
                        url.set(vcs)

                        licenses {
                            license {
                                name.set("The Apache Software License, Version 2.0")
                                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                                distribution.set("repo")
                            }
                        }
                        developers {
                            developer {
                                id.set("MIPT-NPM")
                                name.set("MIPT nuclear physics methods laboratory")
                                organization.set("MIPT")
                                organizationUrl.set("http://npm.mipt.ru")
                            }

                        }
                        scm {
                            url.set(vcs)
                        }
                    }
                }

                val githubUser: String? by project
                val githubToken: String? by project

                if (githubProject != null && githubUser != null && githubToken != null) {
                    repositories {
                        val repository = maven {
                            name = "github"
                            url = uri("https://maven.pkg.github.com/mipt-npm/$githubProject/")
                            credentials {
                                username = githubUser
                                password = githubToken
                            }
                        }

                        val githubPublishTasks = publications.filterIsInstance<MavenPublication>().map { publication ->
                            tasks.register<PublishToMavenRepository>("publish${publication.name.capitalize()}ToGithub") {
                                group = "publishing"
                                this.publication = publication
                                this.repository = repository
                            }
                        }

                        tasks.register<PublishToMavenRepository>("publishToGithub") {
                            group = "publishing"
                            dependsOn(githubPublishTasks)
                        }

                    }
                }

                pluginManager.withPlugin("scientifik.mpp"){
                    tasks.filter { it is ArtifactoryTask || it is BintrayUploadTask }.forEach {
                        it.doFirst {
                            publications.filterIsInstance<MavenPublication>()
                                .forEach { publication ->
                                    val moduleFile =
                                        buildDir.resolve("publications/${publication.name}/module.json")
                                    if (moduleFile.exists()) {
                                        publication.artifact(object : FileBasedMavenArtifact(moduleFile) {
                                            override fun getDefaultExtension() = "module"
                                        })
                                    }
                                }
                        }
                    }
                }

            }

            project.configure<ArtifactoryPluginConvention> {
                val artifactoryUser: String? by project
                val artifactoryPassword: String? by project
                val artifactoryContextUrl = "http://npm.mipt.ru:8081/artifactory"

                setContextUrl(artifactoryContextUrl)//The base Artifactory URL if not overridden by the publisher/resolver
                publish(delegateClosureOf<PublisherConfig> {
                    repository(delegateClosureOf<GroovyObject> {
                        setProperty("repoKey", "gradle-dev-local")
                        setProperty("username", artifactoryUser)
                        setProperty("password", artifactoryPassword)
                    })

                    defaults(delegateClosureOf<GroovyObject> {
                        invokeMethod("publications", arrayOf("jvm", "js", "kotlinMultiplatform", "metadata"))
                    })
                })
                resolve(delegateClosureOf<ResolverConfig> {
                    repository(delegateClosureOf<GroovyObject> {
                        setProperty("repoKey", "gradle-dev")
                        setProperty("username", artifactoryUser)
                        setProperty("password", artifactoryPassword)
                    })
                })
            }

            if (bintrayRepo == null) {
                project.logger.warn("[${project.name}] Bintray repository not defined")
            } else {

                project.configure<PublishingExtension> {
                    repositories {
                        maven("https://bintray.com/mipt-npm/$bintrayRepo")
                    }
                }

                project.configure<BintrayExtension> {
                    user = project.findProperty("bintrayUser") as? String?
                    key = project.findProperty("bintrayApiKey") as? String?
                    publish = true
                    override = true

                    // We have to use delegateClosureOf because bintray supports only dynamic groovy syntax
                    // this is a problem of this plugin
                    pkg.apply {
                        userOrg = "mipt-npm"
                        repo = bintrayRepo
                        name = project.name
                        issueTrackerUrl = "$vcs/issues"
                        setLicenses("Apache-2.0")
                        vcsUrl = vcs
                        version.apply {
                            name = project.version.toString()
                            vcsTag = project.version.toString()
                            released = java.util.Date().toString()
                        }
                    }

                    //workaround bintray bug
                    setPublications(*project.extensions.findByType<PublishingExtension>()!!.publications.names.toTypedArray())
                }

//                tasks.withType<BintrayUploadTask>{
//                    doFirst {
//                        logger.info("repoName = $repoName")
//                        logger.info("versionName = $versionName")
//                        logger.info("packageName = $packageName")
//                    }
//                }
            }
        }
    }
}