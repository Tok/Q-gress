import util.Util
import kotlin.math.floor

//Partially transpiled from Ken Perlins 'ImprovedNoise' JAVA reference implementation, see: http://mrl.nyu.edu/~perlin/noise/
object ImprovedNoise {
    val p = IntArray(512)
    private val permutation = intArrayOf(
            151, 160, 137, 91, 90, 15, 131, 13, 201, 95, 96, 53, 194, 233, 7, 225, 140, 36, 103, 30, 69, 142, 8, 99,
            37, 240, 21, 10, 23, 190, 6, 148, 247, 120, 234, 75, 0, 26, 197, 62, 94, 252, 219, 203, 117, 35, 11, 32,
            57, 177, 33, 88, 237, 149, 56, 87, 174, 20, 125, 136, 171, 168, 68, 175, 74, 165, 71, 134, 139, 48, 27,
            166, 77, 146, 158, 231, 83, 111, 229, 122, 60, 211, 133, 230, 220, 105, 92, 41, 55, 46, 245, 40, 244, 102,
            143, 54, 65, 25, 63, 161, 1, 216, 80, 73, 209, 76, 132, 187, 208, 89, 18, 169, 200, 196, 135, 130, 116,
            188, 159, 86, 164, 100, 109, 198, 173, 186, 3, 64, 52, 217, 226, 250, 124, 123, 5, 202, 38, 147, 118, 126,
            255, 82, 85, 212, 207, 206, 59, 227, 47, 16, 58, 17, 182, 189, 28, 42, 223, 183, 170, 213, 119, 248, 152,
            2, 44, 154, 163, 70, 221, 153, 101, 155, 167, 43, 172, 9, 129, 22, 39, 253, 19, 98, 108, 110, 79, 113,
            224, 232, 178, 185, 112, 104, 218, 246, 97, 228, 251, 34, 242, 193, 238, 210, 144, 12, 191, 179, 162, 241,
            81, 51, 145, 235, 249, 14, 239, 107, 49, 192, 214, 31, 181, 199, 106, 157, 184, 84, 204, 176, 115, 121,
            50, 45, 127, 4, 150, 254, 138, 236, 205, 93, 222, 114, 67, 29, 24, 72, 243, 141, 128, 195, 78, 66, 215,
            61, 156, 180)

    init {
        (0..255).map { p[it] = permutation[it]; p[256 + it] = p[it] }
    }

    fun noiseColorInt(x: Double, y: Double) = 255 * (((noise(x, y) + 1)) / 2)
    private fun noise(x: Double, y: Double, z: Double = 0.0): Double {
        fun fade(t: Double): Double = t * t * t * (t * (t * 6 - 15) + 10)
        fun lerp(t: Double, a: Double, b: Double) = a + t * (b - a)
        fun grad(hash: Int, x: Double, y: Double, z: Double): Double {
            val h = hash and 15         // CONVERT LO 4 BITS OF HASH CODE
            val u = if (h < 8) x else y // INTO 12 GRADIENT DIRECTIONS.
            val v = if (h < 4) y else if (h == 12 || h == 14) x else z
            return (if (h and 1 == 0) u else -u) + if (h and 2 == 0) v else -v
        }

        var xx = x
        var yy = y
        var zz = z
        val xxx = floor(xx).toInt() + 255 // FIND UNIT CUBE THAT
        val yyy = floor(yy).toInt() + 255 // CONTAINS POINT.
        val zzz = floor(zz).toInt() + 255
        xx -= floor(xx) // FIND RELATIVE X,Y,Z
        yy -= floor(yy) // OF POINT IN CUBE.
        zz -= floor(zz)
        val u = fade(xx) // COMPUTE FADE CURVES
        val v = fade(yy) // FOR EACH OF X,Y,Z.
        val w = fade(zz)
        val a = p[xxx] + yyy
        val aa = p[a] + zzz
        val ab = p[a + 1] + zzz // HASH COORDINATES OF
        val b = p[xxx + 1] + yyy
        val ba = p[b] + zzz
        val bb = p[b + 1] + zzz // THE 8 CUBE CORNERS,

        return lerp(w, lerp(v, lerp(u, grad(p[aa], xx, yy, zz),     // AND ADD
                grad(p[ba], xx - 1, yy, zz)),                    // BLENDED
                lerp(u, grad(p[ab], xx, yy - 1, zz),             // RESULTS
                        grad(p[bb], xx - 1, yy - 1, zz))),    // FROM  8
                lerp(v, lerp(u, grad(p[aa + 1], xx, yy, zz - 1), // CORNERS
                        grad(p[ba + 1], xx - 1, yy, zz - 1)), // OF CUBE
                        lerp(u, grad(p[ab + 1], xx, yy - 1, zz - 1),
                                grad(p[bb + 1], xx - 1, yy - 1, zz - 1))))
    }
    fun generateRawMap(width: Int, height: Int, wavelength: Double = 5 + (Util.random() * 5)): Array<DoubleArray> {
        val frequency = wavelength / width
        val noise: Array<DoubleArray> = Array(width) { DoubleArray(height) }
        val z = Util.random() * 1000
        for (x in 0 until width) {
            for (y in 0 until height) {
                noise[x][y] = ImprovedNoise.noise(x * frequency, y * frequency, z * frequency)
            }
        }
        return noise
    }
    fun generateEdgeMap(width: Int, height: Int, wavelength: Double = 10.0): Array<DoubleArray> {
        val frequency = wavelength / width
        val noise: Array<DoubleArray> = Array(width) { DoubleArray(height) }
        val z = Util.random() * 1000
        val steps = 5.0
        for (x in 0 until width) {
            for (y in 0 until height) {
                val raw = ImprovedNoise.noise(x * frequency, y * frequency, z * frequency)
                noise[x][y] = floor((raw + 0.5) * steps) / steps
            }
        }
        return noise
    }
}
