package agent.qvalue

import extension.Canvas

open class QValue(val id: String, val weight: Double, val description: String, val icon: Canvas? = null) {
    val sliderId = id + "Slider"
}
