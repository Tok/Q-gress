package util.ui

import external.FontLoader
import external.TextGeometry
import external.Three
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import kotlin.math.sin

/**
 * A small, self-contained **3D extruded Q-GRESS wordmark** for the world-generation loading screen —
 * a fresh mini three.js renderer (the title's live scene can't survive the onboarding page reload).
 * Reuses the brand font + extrude/material recipe from [system.display.TitleWordmark]. Renders into the
 * loading title element; on font-load it hides the DOM fallback text. Animated: shrinks in, then drifts.
 * [mount] from [LoadingOverlay.show], [unmount] from [LoadingOverlay.done].
 */
object LoadingWordmark {
    private const val TEXT = "Q-GRESS"
    private const val FONT_URL = "fonts/ChakraPetch-Bold.typeface.json"
    private const val RENDER_W = 520
    private const val RENDER_H = 150
    private const val SIZE = 7.0
    private const val DEPTH = 1.6
    private const val SPACING = 1.4
    private const val INTRO_MS = 900.0

    private var renderer: dynamic = null
    private var scene: dynamic = null
    private var camera: dynamic = null
    private var group: dynamic = null
    private var host: HTMLElement? = null
    private var rafId = 0
    private var startMs = 0.0
    private var running = false

    /** Build the mini-renderer into [titleHost] (the loading title element) and start animating. */
    fun mount(titleHost: HTMLElement) {
        if (running) return
        host = titleHost
        val canvas = document.createElement("canvas") as HTMLCanvasElement
        canvas.className = "loadingWordmark"
        titleHost.appendChild(canvas)
        val params: dynamic = js("({ antialias: true, alpha: true })")
        params.canvas = canvas
        renderer = Three.WebGLRenderer(params).asDynamic()
        renderer.setSize(RENDER_W, RENDER_H, false)
        renderer.setPixelRatio(window.devicePixelRatio)
        scene = Three.Scene()
        camera = Three.PerspectiveCamera(32.0, RENDER_W.toDouble() / RENDER_H, 0.1, 1000.0).asDynamic()
        camera.position.set(0.0, 0.0, 64.0)
        scene.add(Three.AmbientLight(js("0xffffff"), 0.9))
        val dir = Three.DirectionalLight(js("0xffffff"), 0.85).asDynamic()
        dir.position.set(0.4, 0.8, 1.0)
        scene.add(dir)
        FontLoader().load(FONT_URL, { font -> build(font) })
        startMs = now()
        running = true
        loop()
    }

    private fun build(font: dynamic) {
        if (!running) return
        val g: dynamic = Three.Group()
        val capMat = standard("#ffffff", 0.45, "#ffffff", 0.35)
        val sideMat = standard("#0b0b0d", 0.5, "#000000", 0.0)
        val letters = mutableListOf<dynamic>()
        var cursor = 0.0
        TEXT.forEach { ch ->
            val geo = letterGeo(font, ch)
            geo.computeBoundingBox()
            val bb = geo.boundingBox
            val minX = bb.min.x as Double
            val maxX = bb.max.x as Double
            val w = maxX - minX
            geo.asDynamic().translate(-(minX + maxX) / 2.0, -((bb.min.y as Double) + (bb.max.y as Double)) / 2.0, -DEPTH / 2.0)
            val mesh = Three.Mesh(geo, arrayOf(capMat, sideMat)).asDynamic()
            mesh.position.x = cursor + w / 2.0
            g.add(mesh)
            letters.add(mesh)
            cursor += w + SPACING
        }
        val half = (cursor - SPACING) / 2.0
        letters.forEach { it.position.x = (it.position.x as Double) - half } // centre the word on the group origin
        scene.add(g)
        group = g
        host?.asDynamic()?.style?.color = "transparent" // 3D is up → hide the DOM fallback text
    }

    private fun letterGeo(font: dynamic, ch: Char): dynamic {
        val p: dynamic = js("({})")
        p.font = font
        p.size = SIZE
        p.depth = DEPTH // three r150+
        p.height = DEPTH // older three
        p.curveSegments = 4
        p.bevelEnabled = true
        p.bevelThickness = 0.14
        p.bevelSize = 0.1
        p.bevelSegments = 1
        return TextGeometry(ch.toString(), p)
    }

    private fun standard(color: String, roughness: Double, emissive: String, emissiveIntensity: Double): dynamic {
        val p: dynamic = js("({})")
        p.color = color
        p.metalness = 0.0
        p.roughness = roughness
        p.emissive = emissive
        p.emissiveIntensity = emissiveIntensity
        return Three.MeshStandardMaterial(p)
    }

    private fun loop() {
        if (!running) return
        group?.let { g ->
            val t = ((now() - startMs) / INTRO_MS).coerceIn(0.0, 1.0)
            val intro = 1.0 - (1.0 - t) * (1.0 - t) // easeOut: overshoot-free shrink-in
            val s = 1.3 - 0.3 * intro // 1.3 → 1.0
            g.scale.set(s, s, s)
            val drift = (now() - startMs) / 1000.0
            g.rotation.y = sin(drift * 0.6) * 0.32 // gentle yaw wobble so it reads 3D
            g.rotation.x = -0.05
        }
        renderer?.render(scene, camera)
        rafId = window.requestAnimationFrame { loop() }
    }

    /** Stop + tear down the renderer (called when the loading overlay finishes). */
    fun unmount() {
        if (!running) return
        running = false
        if (rafId != 0) window.cancelAnimationFrame(rafId)
        rafId = 0
        renderer?.dispose()
        renderer = null
        scene = null
        camera = null
        group = null
        host = null
    }

    private fun now() = js("performance.now()") as Double
}
