package com.deploydulupulangnanti.asmrcuttingfruit

import kotlin.math.abs
import kotlin.math.hypot

data class Vec(val x: Float, val y: Float) {
    operator fun plus(o: Vec) = Vec(x + o.x, y + o.y)
    operator fun minus(o: Vec) = Vec(x - o.x, y - o.y)
    operator fun times(v: Float) = Vec(x * v, y * v)
}

object SliceGeometry {
    fun area(poly: List<Vec>): Float = abs(poly.indices.sumOf { i ->
        val p = poly[i]; val q = poly[(i + 1) % poly.size]
        (p.x * q.y - q.x * p.y).toDouble()
    }.toFloat()) / 2f

    fun split(poly: List<Vec>, a: Vec, b: Vec): Pair<List<Vec>, List<Vec>>? {
        if (hypot(b.x - a.x, b.y - a.y) < .015f || poly.size < 3) return null
        fun side(p: Vec) = (b.x - a.x) * (p.y - a.y) - (b.y - a.y) * (p.x - a.x)
        // A finite gesture must enter the polygon; its infinite extension must not cut distant fruit.
        var lo = 0f; var hi = 1f
        val winding = poly.indices.sumOf { i ->
            val p = poly[i]; val q = poly[(i + 1) % poly.size]
            (p.x * q.y - q.x * p.y).toDouble()
        }
        val sign = if (winding >= 0) 1f else -1f
        for (i in poly.indices) {
            val p = poly[i]; val q = poly[(i + 1) % poly.size]
            fun edge(v: Vec) = sign * ((q.x - p.x) * (v.y - p.y) - (q.y - p.y) * (v.x - p.x))
            val start = edge(a); val delta = edge(b) - start
            if (abs(delta) < 1e-6f) { if (start < 0) return null }
            else { val t = -start / delta; if (delta > 0) lo = maxOf(lo, t) else hi = minOf(hi, t) }
            if (lo > hi) return null
        }
        fun clip(positive: Boolean): List<Vec> {
            val result = mutableListOf<Vec>()
            for (i in poly.indices) {
                val p = poly[i]; val q = poly[(i + 1) % poly.size]
                val sp = side(p); val sq = side(q)
                val insideP = if (positive) sp >= 0 else sp <= 0
                val insideQ = if (positive) sq >= 0 else sq <= 0
                if (insideP) result += p
                if (insideP != insideQ) result += p + (q - p) * (sp / (sp - sq))
            }
            return result
        }
        val left = clip(true); val right = clip(false)
        return if (area(left) > .012f && area(right) > .012f) left to right else null
    }
}
