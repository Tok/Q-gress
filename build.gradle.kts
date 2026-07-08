// ============================================================================
// JDK / toolchain note
// ----------------------------------------------------------------------------
// This build runs on JDK 21 (LTS), not the latest JDK 25, on purpose:
// detekt's latest release (1.23.8) bundles an older Kotlin compiler that
// crashes when it runs inside a JDK 25 process (it can't parse the "25.0.x"
// runtime version). detekt is our cyclomatic-complexity gate, so we keep the
// build JVM at 21 to keep that gate working. This is purely a build-tool
// choice — the project targets Kotlin/JS and ships no JVM bytecode, so the
// JDK running Gradle does not affect the product at all. This is a settled
// decision: we stay on JDK 21 LTS for the build and keep Kotlin/Gradle current.
// ============================================================================

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

plugins {
    kotlin("multiplatform") version "2.4.0"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
    // Line-coverage for the pure functional core. Kover instruments JVM bytecode, so coverage is
    // measured by the `jvm()` test target below (the product still ships only JS). See docs/ARCHITECTURE.md.
    id("org.jetbrains.kotlinx.kover") version "0.9.8"
}

group = "zir.teq"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

// --- Static analysis / formatting (enforced on commit; see .githooks) ---

ktlint {
    // Engine version is the plugin default; rules live in .editorconfig
    // so the IDE and CLI agree.
    filter {
        exclude { it.file.path.contains("/build/") }
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("config/detekt/detekt.yml"))
    baseline = file("config/detekt/baseline.xml")
    // Multiplatform: point the single `detekt` task at every source set (the shared core + JS shell).
    source.setFrom("src/commonMain/kotlin", "src/commonTest/kotlin", "src/jsMain/kotlin", "src/jsTest/kotlin")
}

// Use the single configured `detekt` task as the gate; disable the redundant
// per-source-set tasks the KMP integration auto-creates (they'd need separate
// baselines and duplicate the same analysis).
tasks.matching {
    it.name in listOf(
        "detektJsMain",
        "detektJsTest",
        "detektJvmMain",
        "detektJvmTest",
        "detektMetadataMain",
    )
}.configureEach { enabled = false }

// --- Git hooks: point git at the in-repo .githooks dir so the pre-commit gate
// (ktlint + detekt) is enforced for everyone. Run once after cloning. ---
tasks.register<Exec>("installGitHooks") {
    group = "git hooks"
    description = "Configure git to use the in-repo .githooks directory."
    commandLine("git", "config", "core.hooksPath", ".githooks")
    doLast { logger.lifecycle("core.hooksPath set to .githooks (pre-commit gate active).") }
}

// --- Build info: generate config/BuildInfo (timestamp + git short-sha) at build time so any
// deployed bundle is identifiable in-app. Written into a generated source dir (not committed). ---
val generateBuildInfo = tasks.register("generateBuildInfo") {
    val outDir = layout.buildDirectory.dir("generated/buildinfo/kotlin")
    val projectVersion = version.toString() // captured at config time (config-cache: no script refs in the action)
    outputs.dir(outDir)
    doLast {
        val ts = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'")
            .withZone(ZoneOffset.UTC).format(Instant.now())
        val sha = try {
            ProcessBuilder("git", "rev-parse", "--short", "HEAD")
                .redirectErrorStream(true).start().inputStream.bufferedReader().readText().trim()
        } catch (e: Exception) {
            "unknown"
        }
        val label = "3D v$projectVersion · $ts · $sha"
        val file = outDir.get().file("config/BuildInfo.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            package config

            // Generated at build time (build.gradle.kts: generateBuildInfo). Do not edit.
            object BuildInfo {
                const val BUILD_TIME = "$ts"
                const val GIT_SHA = "$sha"
                const val VERSION = "$projectVersion"
                const val LABEL = "$label"
            }
            """.trimIndent() + "\n",
        )
    }
}

kotlin {
    // JVM target: NOT shipped. It exists purely so the pure functional core in `commonMain` can be
    // unit-tested on the JVM and instrumented by Kover for line coverage (Kover can't read Kotlin/JS).
    // Only `commonMain`/`commonTest`/`jvmTest` compile here — all browser/WebGL/three.js code stays in jsMain.
    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }
    js {
        // Output bundle is named after the root project: Q-Gress.js
        binaries.executable()
        // The app runs in the browser (webpack distribution + dev server).
        browser {
            commonWebpackConfig {
                outputFileName = "Q-Gress.js"
            }
            testTask {
                // Browser tests use Karma + headless Chrome (see karma.config.d).
                // Disabled for now: the current unit tests target the pure logic
                // core and run in Node (below); they rely on isRunningInBrowser()
                // being false to keep canvas/DOM code paths guarded off.
                enabled = false
                useKarma {
                    useChromeHeadless()
                }
            }
        }
        // Unit tests run fast & headless in Node (mirrors the original Mocha setup). The default 2 s Mocha
        // timeout is too tight for the headless AI-match tests (SimRunner/Tournament run full sims over
        // thousands of ticks), so give them room.
        nodejs {
            testTask {
                useMocha {
                    // Default 60 s per test; scripts/bake-champs.sh raises it via -PmochaTimeout for the long
                    // per-arch champion bakes (a full training run is minutes, not seconds).
                    timeout = (findProperty("mochaTimeout") as? String) ?: "60s"
                }
                // -PbakeVerbose=1 (set by scripts/bake-champs.sh) streams test stdout live, so the long bake
                // shows per-generation progress instead of going silent until the single test method returns.
                if ((findProperty("bakeVerbose") as? String) == "1") {
                    testLogging { showStandardStreams = true }
                }
            }
        }
    }

    sourceSets {
        // Shared pure functional core — must compile for BOTH js and jvm, so NO js()/DOM/WebGL/three.js here.
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        jsMain {
            kotlin.srcDir(generateBuildInfo) // generated config/BuildInfo (build timestamp + git-sha)
            dependencies {
                // kotlin.browser.* / kotlin.dom.* moved here after Kotlin 1.4
                implementation("org.jetbrains.kotlinx:kotlinx-browser:0.5.0")
                // Cooperative async for the heat-map / vector-field calc (non-blocking portal creation).
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
                // three.js is bundled via webpack (was a UMD CDN <script>); npm/ESM unlocks GLTFLoader.
                implementation(npm("three", "0.160.0"))
                // cannon-es: rigid-body physics for the glass shards (tumble + settle).
                implementation(npm("cannon-es", "0.20.0"))
            }
        }
        jsTest.dependencies {
            implementation(kotlin("test"))
        }
        jvmTest.dependencies {
            implementation(kotlin("test-junit5"))
        }
    }
}

// The jvm() target's tests exist only to exercise + measure the shared core; run them on JUnit 5.
tasks.withType<Test>().configureEach { useJUnitPlatform() }
