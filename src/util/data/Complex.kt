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
data class Complex(val re: Float, val im: Float = 0F) {
    constructor(real: Int, imaginary: Int = 0) : this(real.toFloat(), imaginary.toFloat())

    val magnitude: Float = sqrt(addSquares(re, im)).toFloat()
    val mag: Float = magnitude
    val magnitude2: Float = addSquares(re, im)
    val phase = atan2(im.toDouble(), re.toDouble())
    val modulus = magnitude

    fun copyWithNewMagnitude(mag: Float) = Complex.fromMagnitudeAndPhase(mag, this.phase)

    fun negate() = Complex(-re, -im)
    fun conjugate() = Complex(re, -im)
    fun reverse() = Complex(-re, im)
    operator fun not() = negate()
    operator fun plus(c: Complex) = Complex(re + c.re, im + c.im)
    operator fun plus(d: Float) = Complex(re + d, im)
    operator fun minus(c: Complex) = Complex(re - c.re, im - c.im)
    operator fun minus(d: Float) = Complex(re - d, im)
    operator fun times(c: Complex) = Complex(re * c.re - im * c.im, re * c.im + im * c.re)
    operator fun div(c: Complex): Complex {
        if (c.re == 0F) throw IllegalArgumentException("Real part is 0.")
        if (c.im == 0F) throw IllegalArgumentException("Imaginary part is 0.")
        val d = addSquares(c.re, c.im).toFloat()
        return Complex((re * c.re + im * c.im) / d, (im * c.re - re * c.im) / d)
    }

    private fun addSquares(re: Float, im: Float) = (re * re + im * im)

    override fun toString(): String {
        return when (this) {
            I -> "i"
            Complex(re) -> re.toString()
            Complex(im) -> im.toString() + "*i"
            else -> {
                val imString = if (im < 0F) "-" + -im else "+$im"
                return re.toString() + imString + "*i"
            }
        }
    }

    companion object {
        val I = Complex(0F, 1F)
        val ZERO = Complex(0F, 0F)
        val ONE = Complex(1F, 0F)
        fun selectStronger(first: Complex, second: Complex) = if (first.mag < second.mag) first else second
        fun fromImaginary(imaginary: Float) = Complex(0F, imaginary)
        fun fromImaginaryInt(imaginary: Int) = Complex(0F, imaginary.toFloat())
        fun valueOf(magnitude: Float, phase: Double) = fromMagnitudeAndPhase(magnitude, phase)
        fun fromMagnitudeAndPhase(magnitude: Float, phase: Double) =
                Complex(magnitude * cos(phase).toFloat(), magnitude * sin(phase).toFloat())
        fun random(): Complex {
            val mag = Util.random().toFloat()
            val phase = Constants.tau * Util.random()
            return Complex.fromMagnitudeAndPhase(mag, phase)
        }
    }
}
