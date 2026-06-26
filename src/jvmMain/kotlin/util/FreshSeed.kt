package util

// JVM (test-only target): a fresh 32-bit seed. Exists so commonMain compiles + runs under jvmTest/Kover.
actual fun freshSeed(): Int = kotlin.random.Random.Default.nextInt()
