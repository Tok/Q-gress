plugins {
    kotlin("multiplatform") version "2.4.0"
}

group = "zir.teq"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
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
        }
        jsTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
