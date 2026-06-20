package util.data

import config.Constants
import util.Util
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Originally transpiled and rearranged from: https://github.com/Tok/Erwin/tree/master/src/main/java/erwin
 * In: https://github.com/Tok/Zir-Watchface/blob/master/Wearable/src/main/kotlin/zir/teq/wearable/watchface/model/data/types/Complex.kt
 */
data class Complex(val re: Double, val im: Double = 0.0) {
    constructor(real: Int, imaginary: Int = 0) : this(real.toDouble(), imaginary.toDouble())

    val magnitude: Double = sqrt(addSquares(re, im))
    val mag: Double = magnitude
    val magnitude2: Double = addSquares(re, im)
    val phase = atan2(im, re)
    val modulus = magnitude

    fun copyWithNewMagnitude(mag: Double) = Complex.fromMagnitudeAndPhase(mag, this.phase)

    fun negate() = Complex(-re, -im)
    fun conjugate() = Complex(re, -im)
    fun reverse() = Complex(-re, im)
    operator fun not() = negate()
    operator fun plus(c: Complex) = Complex(re + c.re, im + c.im)
    operator fun plus(d: Double) = Complex(re + d, im)
    operator fun minus(c: Complex) = Complex(re - c.re, im - c.im)
    operator fun minus(d: Double) = Complex(re - d, im)
    operator fun times(c: Complex) = Complex(re * c.re - im * c.im, re * c.im + im * c.re)
    operator fun div(c: Complex): Complex {
        if (c.re == 0.0) throw IllegalArgumentException("Real part is 0.")
        if (c.im == 0.0) throw IllegalArgumentException("Imaginary part is 0.")
        val d = addSquares(c.re, c.im)
        return Complex((re * c.re + im * c.im) / d, (im * c.re - re * c.im) / d)
    }

    private fun addSquares(re: Double, im: Double) = (re * re + im * im)

    override fun toString(): String {
        return when (this) {
            I -> "i"
            Complex(re, 0.0) -> re.toString()
            Complex(0.0, im) -> im.toString() + "*i"
            else -> {
                val imString = if (im < 0F) "-" + -im else "+$im"
                return re.toString() + imString + "*i"
            }
        }
    }

    companion object {
        val I = Complex(0.0, 1.0)
        val ZERO = Complex(0.0, 0.0)
        val ONE = Complex(1.0, 0.0)
        fun selectStronger(first: Complex, second: Complex) = if (first.mag < second.mag) first else second
        fun fromImaginary(imaginary: Double) = Complex(0.0, imaginary)
        fun fromImaginaryInt(imaginary: Int) = Complex(0.0, imaginary.toDouble())
        fun valueOf(magnitude: Double, phase: Double) = fromMagnitudeAndPhase(magnitude, phase)
        fun fromMagnitudeAndPhase(magnitude: Double, phase: Double) =
                Complex(magnitude * cos(phase), magnitude * sin(phase))

        fun random(): Complex {
            val mag = Util.random()
            val phase = Constants.tau * Util.random()
            return Complex.fromMagnitudeAndPhase(mag, phase)
        }
    }
}
