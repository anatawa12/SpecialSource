import java.util.zip.ZipInputStream

plugins {
    java
    `maven-publish`
}

group = "com.anatawa12.forge"

version = "${property("version")!!}"

java {
    targetCompatibility = JavaVersion.VERSION_1_8
    sourceCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()
}

val jar by tasks.getting(Jar::class) {
    manifest {
        attributes(mapOf(
            "version" to project.version,
            "javaCompliance" to project.java.targetCompatibility,
            "group" to project.group,
            "Implementation-Version" to "${project.version}${getGitHash()}",
            "Main-Class" to "net.md_5.specialsource.SpecialSource",
        ))
    }
}

dependencies {
    implementation("org.ow2.asm:asm-commons:9.4")
    compileOnly("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")
    implementation("net.sf.jopt-simple:jopt-simple:5.0.4")
    implementation("com.google.guava:guava:31.1-jre")
    implementation("com.opencsv:opencsv:5.7.0")
}

java {
    withJavadocJar()
    withSourcesJar()
}

artifacts {
    archives(jar)
}

publishing {
    publications {
        val maven_central by this.creating(MavenPublication::class) {
            from(components["java"])
            artifactId = base.archivesName.get()

            pom {
                name.set(project.base.archivesName.get())
                description.set("A jar compare and renaming engine designed for comparing and remapping 2 jars " +
                        "of differnent obfuscation mappings. Can also be useful for reobfuscation. " +
                        "(fork for https://github.com/anatawa12/ForgeGradle-1.2)")
                url.set("https://github.com/anatawa12/SpecialSource")

                scm {
                    url.set("https://github.com/anatawa12/SpecialSource")
                    connection.set("scm:git:git://github.com/anatawa12/SpecialSource.git")
                    developerConnection.set("scm:git:git@github.com:anatawa12/SpecialSource.git")
                }

                issueManagement {
                    system.set("github")
                    url.set("https://github.com/anatawa12/SpecialSource/issues")
                }

                licenses {
                    license {
                        name.set("The BSD 3-Clause License")
                        url.set("https://opensource.org/licenses/BSD-3-Clause")
                        distribution.set("repo")
                    }
                }

                developers {
                    developer {
                        id.set("md-5")
                        name.set("md-5")
                        roles.set(setOf("developer"))
                    }

                    developer {
                        id.set("anatawa12")
                        name.set("anatawa12")
                        roles.set(setOf("developer"))
                    }
                }
            }
        }
    }
}

val dependenciesJava8CompatibilityCheck by tasks.creating {
    doLast {
        if (System.getenv("CHECK_JDK_COMPATIBILITY")?.toBoolean() == true) {
            configurations.runtimeClasspath.get().asSequence().forEach {
                val reading = ByteArray(8)
                val zis = ZipInputStream(it.inputStream())
                while (true) {
                    val entry = zis.nextEntry ?: break
                    if (!entry.name.endsWith(".class")) continue
                    if (entry.name == "module-info.class") continue
                    if (entry.name.contains("META-INF/")) continue
                    if (zis.read(reading) != reading.size) continue
                    if (reading[0] == 0xCA.toByte() &&
                        reading[1] == 0xFE.toByte() &&
                        reading[2] == 0xBA.toByte() &&
                        reading[3] == 0xBE.toByte() &&
                        reading[4] == 0x00.toByte() &&
                        reading[5] == 0x00.toByte()) {
                        val major = reading[6].toInt().and(0xFF).shl(8) or reading[7].toInt().and(0xFF)
                        if (major > 52)
                            throw IllegalStateException("${entry.name} of $it is not compatible with java 8 (${major-44}): class ${entry.name}")
                    }
                }
            }
        }
    }
}

tasks.check.get().dependsOn(dependenciesJava8CompatibilityCheck)

// write out version so its convenient for doc deployment
file("build").mkdirs()
file("build/version.txt").writeText("$version")

fun getGitHash(): String {
    val process = ProcessBuilder("git", "rev-parse", "--short", "HEAD")
        .directory(file("."))
        .start()
    process.waitFor()
    return "-" + (if (process.exitValue() != 0) "unknown" else process.inputStream.reader().use { it.readText() }.trim())
}
