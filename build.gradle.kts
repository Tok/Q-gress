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

plugins {
    kotlin("multiplatform") version "2.4.0"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
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
    // Multiplatform: point the single `detekt` task at the JS source sets.
    source.setFrom("src/jsMain/kotlin", "src/jsTest/kotlin")
}

// Use the single configured `detekt` task as the gate; disable the redundant
// per-source-set tasks the KMP integration auto-creates (they'd need separate
// baselines and duplicate the same analysis).
tasks.matching {
    it.name in listOf("detektJsMain", "detektJsTest", "detektMetadataMain")
}.configureEach { enabled = false }

// --- Git hooks: point git at the in-repo .githooks dir so the pre-commit gate
// (ktlint + detekt) is enforced for everyone. Run once after cloning. ---
tasks.register<Exec>("installGitHooks") {
    group = "git hooks"
    description = "Configure git to use the in-repo .githooks directory."
    commandLine("git", "config", "core.hooksPath", ".githooks")
    doLast { logger.lifecycle("core.hooksPath set to .githooks (pre-commit gate active).") }
}

kotlin {
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
        // Unit tests run fast & headless in Node (mirrors the original Mocha setup).
        nodejs {
            testTask {
                useMocha()
            }
        }
    }

    sourceSets {
        jsMain.dependencies {
            // kotlin.browser.* / kotlin.dom.* moved here after Kotlin 1.4
            implementation("org.jetbrains.kotlinx:kotlinx-browser:0.5.0")
            // three.js is bundled via webpack (was a UMD CDN <script>); npm/ESM unlocks GLTFLoader.
            implementation(npm("three", "0.160.0"))
            // cannon-es: rigid-body physics for the glass shards (tumble + settle).
            implementation(npm("cannon-es", "0.20.0"))
        }
        jsTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
