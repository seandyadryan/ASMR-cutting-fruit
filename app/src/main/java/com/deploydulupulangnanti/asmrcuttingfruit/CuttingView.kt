package com.deploydulupulangnanti.asmrcuttingfruit

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import kotlin.math.*
import kotlin.random.Random

data class SlicePiece(val polygon: List<Vec>, var offset: Vec = Vec(0f, 0f), var velocity: Vec = Vec(0f, 0f))
class CuttingState(var item: Int = 0, var cuts: Int = 0) {
    val pieces = mutableListOf<SlicePiece>()
    fun reset(index: Int) {
        item = index; cuts = 0; pieces.clear()
        pieces += SlicePiece(List(48) { i -> val a = i * 2 * PI / 48; Vec(cos(a).toFloat(), sin(a).toFloat()) })
    }
    init { reset(item) }
}

class CuttingView(context: Context, private val state: CuttingState, private val onCut: () -> Unit) : View(context) {
    private val prefs = context.getSharedPreferences("studio", Context.MODE_PRIVATE)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val path = Path()
    private val artRect = RectF(-1f, -1f, 1f, 1f)
    private var boardImage: Bitmap? = null
    private val art = BitmapFactory.decodeStream(context.assets.open("produce/${Pantry.items[state.item].id}.png"))
    private var radius = 1f
    private var cx = 0f
    private var cy = 0f
    private var down: Vec? = null
    private var pointer: Vec? = null
    private var trail: Pair<Vec, Vec>? = null
    private var trailTime = 0L
    private var lastTime = 0L
    private var paused = false
    private val dust = mutableListOf<Drop>()
    private data class Drop(var x: Float, var y: Float, val vx: Float, var vy: Float, var life: Float, val size: Float)
    init {
        isFocusable = true; isClickable = true
        contentDescription = "${Pantry.items[state.item].name} cutting board. Swipe across the produce to slice. Double tap with TalkBack to make a slice."
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }
    fun setPaused(value: Boolean) { paused = value; down = null; pointer = null; lastTime = 0; if (!value) invalidate() }
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        cx = w / 2f; cy = h / 2f + 5 * resources.displayMetrics.density
        radius = min(w * .315f, h * .33f)
        if (w > 0 && h > 0) {
            boardImage = Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888).also { drawBoard(Canvas(it)) }
        }
    }
    private fun polygon(canvas: Canvas, poly: List<Vec>) {
        path.reset(); poly.forEachIndexed { i, v -> if (i == 0) path.moveTo(v.x, v.y) else path.lineTo(v.x, v.y) }; path.close()
        canvas.clipPath(path)
    }
    private fun drawBoard(c: Canvas) {
        val dp = resources.displayMetrics.density
        val walnut = prefs.getInt("board", 0) == 1
        val sage = prefs.getInt("board", 0) == 2
        val boardColor = if (walnut) Color.rgb(111, 76, 53) else if (sage) Color.rgb(162, 177, 152) else Color.rgb(218, 178, 126)
        val board = RectF(12 * dp, 12 * dp, width - 12 * dp, height - 12 * dp)
        paint.shader = null; paint.color = Color.argb(20, 46, 36, 20)
        c.drawRoundRect(RectF(board.left, board.top + 5 * dp, board.right, board.bottom + 5 * dp), 26 * dp, 26 * dp, paint)
        paint.alpha = 255
        paint.shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), intArrayOf(lighten(boardColor, 14), boardColor, lighten(boardColor, -12)), null, Shader.TileMode.CLAMP)
        c.drawRoundRect(board, 26 * dp, 26 * dp, paint); paint.shader = null
        c.save(); path.reset(); path.addRoundRect(board, 26 * dp, 26 * dp, Path.Direction.CW); c.clipPath(path)
        val grain = Random(42)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = .7f * dp
        repeat(95) { i ->
            val x = board.left + i * board.width() / 94
            paint.color = Color.argb(if (sage) 10 else 18, 75, 42, 15)
            path.reset(); path.moveTo(x, board.top)
            path.cubicTo(x + grain.nextFloat() * 22 * dp, height * .28f, x - grain.nextFloat() * 22 * dp, height * .7f, x + grain.nextFloat() * 7 * dp, board.bottom)
            c.drawPath(path, paint)
        }
        paint.color = Color.argb(28, 80, 47, 23); paint.strokeWidth = 1.5f * dp
        c.drawRoundRect(RectF(board.left + 10 * dp, board.top + 10 * dp, board.right - 10 * dp, board.bottom - 10 * dp), 19 * dp, 19 * dp, paint)
        paint.style = Paint.Style.FILL
        // A quiet engraved maker's mark, positioned clear of the slicing area.
        paint.color = if (walnut) 0x77F8E5C4 else 0x66583821
        paint.textSize = 9 * dp; paint.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL); paint.textAlign = Paint.Align.CENTER
        c.drawText("T H E   Q U I E T   K I T C H E N", cx, board.bottom - 22 * dp, paint)
        paint.textAlign = Paint.Align.LEFT
        // A small resting chef's knife is drawn once with the board texture.
        c.save(); c.translate(board.right - 35*dp,cy - 14*dp); c.rotate(17f)
        paint.alpha=255
        paint.shader=LinearGradient(-12*dp,0f,12*dp,0f,intArrayOf(0xFFCED6D0.toInt(),Color.WHITE,0xFFABB7B0.toInt()),null,Shader.TileMode.CLAMP)
        path.reset(); path.moveTo(-12*dp,-80*dp); path.lineTo(10*dp,-69*dp); path.lineTo(10*dp,0f); path.lineTo(-12*dp,0f); path.close(); c.drawPath(path,paint)
        paint.shader=null; paint.color=0xFF5B3C2A.toInt(); c.drawRoundRect(-8*dp,-3*dp,9*dp,49*dp,5*dp,5*dp,paint)
        paint.color=0xFFD0C5AE.toInt(); c.drawCircle(.5f*dp,12*dp,1.5f*dp,paint); c.drawCircle(.5f*dp,34*dp,1.5f*dp,paint)
        c.restore(); c.restore()
    }
    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val now = System.nanoTime()
        val dt = if (lastTime == 0L || paused) 0f else ((now - lastTime) / 1e9f).coerceAtMost(.04f)
        lastTime = now
        paint.color=Color.WHITE; paint.shader=null; paint.alpha=255
        boardImage?.let { c.drawBitmap(it,0f,0f,paint) }
        c.save(); c.translate(cx, cy); c.scale(radius, radius)
        val reduced = prefs.getBoolean("motion", false)
        for (piece in state.pieces) {
            if (!reduced && dt > 0) {
                piece.offset = piece.offset + piece.velocity * dt
                piece.velocity = piece.velocity * exp(-5f * dt)
                piece.offset = Vec(piece.offset.x.coerceIn(-.36f, .36f), piece.offset.y.coerceIn(-.36f, .36f))
            }
            c.save(); c.translate(piece.offset.x, piece.offset.y)
            // A small offset under each polygon gives separate pieces tactile depth.
            c.save(); c.translate(.012f, .032f); polygon(c, piece.polygon)
            paint.color = 0x404B2E1A; c.drawOval(artRect,paint); c.restore()
            c.save(); polygon(c, piece.polygon); paint.color = Color.WHITE
            c.drawBitmap(art, null, artRect, paint); c.restore(); c.restore()
        }
        val iter = dust.iterator()
        while (iter.hasNext()) {
            val drop = iter.next(); drop.life -= dt
            if (drop.life <= 0) { iter.remove(); continue }
            drop.x += drop.vx * dt; drop.y += drop.vy * dt; drop.vy += .65f * dt
            paint.color = Pantry.items[state.item].color; paint.alpha = (drop.life.coerceAtMost(1f) * 170).toInt()
            c.drawCircle(drop.x, drop.y, drop.size, paint)
        }
        paint.alpha = 255
        val line = if (down != null && pointer != null) down!! to pointer!! else trail
        if (line != null) {
            val alpha = if (down != null) 190 else (1f - (System.currentTimeMillis() - trailTime) / 400f).coerceIn(0f, 1f).times(230).toInt()
            paint.color = Color.argb(alpha, 255, 254, 230); paint.strokeWidth = .018f; paint.strokeCap = Paint.Cap.ROUND
            c.drawLine(line.first.x, line.first.y, line.second.x, line.second.y, paint)
        }
        paint.strokeCap = Paint.Cap.BUTT; c.restore()
        if (!paused && (dust.isNotEmpty() || down != null || state.pieces.any { abs(it.velocity.x) + abs(it.velocity.y) > .003f } || System.currentTimeMillis() - trailTime < 450)) postInvalidateOnAnimation()
    }
    private fun lighten(color: Int, amount: Int) = Color.rgb((Color.red(color) + amount).coerceIn(0,255), (Color.green(color) + amount).coerceIn(0,255), (Color.blue(color) + amount).coerceIn(0,255))
    private fun local(x: Float, y: Float) = Vec((x - cx) / radius, (y - cy) / radius)
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (paused || state.cuts >= 6) return true
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { parent.requestDisallowInterceptTouchEvent(true); down = local(event.x,event.y); pointer = down; invalidate() }
            MotionEvent.ACTION_MOVE -> { pointer = local(event.x,event.y); invalidate() }
            MotionEvent.ACTION_UP -> { val a = down; val b = local(event.x,event.y); down = null; pointer = null; if (a != null) slice(a,b); parent.requestDisallowInterceptTouchEvent(false); performClick() }
            MotionEvent.ACTION_CANCEL -> { down = null; pointer = null; parent.requestDisallowInterceptTouchEvent(false); invalidate() }
        }
        return true
    }
    override fun performClick(): Boolean { super.performClick(); return true }
    private fun slice(a: Vec, b: Vec) {
        if (state.cuts >= 6 || paused || hypot(b.x - a.x, b.y - a.y) < .22f) return
        val len = hypot(b.x - a.x,b.y - a.y)
        val normal = Vec(-(b.y-a.y)/len,(b.x-a.x)/len)
        var sliced = false
        val output = mutableListOf<SlicePiece>()
        for (piece in state.pieces) {
            val parts = SliceGeometry.split(piece.polygon, a-piece.offset, b-piece.offset)
            if (parts == null) output += piece else {
                sliced = true
                val gap = if (prefs.getBoolean("motion", false)) .018f else .026f
                output += SlicePiece(parts.first, piece.offset + normal * gap, normal * .26f)
                output += SlicePiece(parts.second, piece.offset - normal * gap, normal * -.26f)
            }
        }
        if (!sliced) return
        state.pieces.clear(); state.pieces.addAll(output); state.cuts++
        if (!prefs.getBoolean("motion",false)) repeat(10) {
            val t = .3f + Random.nextFloat() * .4f
            val p = a + (b-a)*t
            dust += Drop(p.x,p.y,(Random.nextFloat()-.5f)*.65f,(Random.nextFloat()-.5f)*.65f,.5f+Random.nextFloat()*.6f,.008f+Random.nextFloat()*.012f)
        }
        trail = a to b; trailTime = System.currentTimeMillis()
        onCut(); invalidate()
    }
    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.addAction(AccessibilityNodeInfo.AccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, "Make a slice"))
    }
    override fun performAccessibilityAction(action: Int, arguments: Bundle?): Boolean {
        if (action == AccessibilityNodeInfo.ACTION_CLICK) {
            val at = -.65f + state.cuts * .25f
            slice(Vec(-1.5f,at),Vec(1.5f,at)); return true
        }
        return super.performAccessibilityAction(action,arguments)
    }
}
