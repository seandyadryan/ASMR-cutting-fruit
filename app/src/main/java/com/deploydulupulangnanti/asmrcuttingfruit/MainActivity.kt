package com.deploydulupulangnanti.asmrcuttingfruit

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.time.LocalDate
import java.util.Locale

class MainActivity : Activity() {
    private val cream = 0xFFF7F4ED.toInt()
    private val green = 0xFF254D3E.toInt()
    private val ink = 0xFF283D33.toInt()
    private val muted = 0xFF71796D.toInt()
    private val paper = 0xFFECEEE3.toInt()
    private val prefs by lazy { getSharedPreferences("studio", MODE_PRIVATE) }
    private val serif by lazy { resources.getFont(R.font.lora) }
    private val sans by lazy { resources.getFont(R.font.dm_sans) }
    private lateinit var audio: StudioAudio
    private lateinit var root: LinearLayout
    private lateinit var body: FrameLayout
    private var tab = "studio"
    private var mode = Mode.FREE
    private var game: CuttingState? = null
    private var cutting: CuttingView? = null
    private var sequence = emptyList<Int>()
    private var basket = 0
    private var sessionSlices = 0
    private var secondsLeft = 60_000L
    private var tickAt = 0L
    private var foreground = false
    private var dialogOpen = false
    private var sessionDay = LocalDate.now().toEpochDay()
    private var counter: TextView? = null
    private var timer: TextView? = null
    private var next: TextView? = null
    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            val now = SystemClock.elapsedRealtime()
            if (foreground && !dialogOpen && game != null && mode == Mode.FOCUS) {
                if (tickAt != 0L) secondsLeft -= now - tickAt
                if (secondsLeft <= 0) { secondsLeft = 0; finishSession(); return }
                timer?.text = formatRemaining(secondsLeft)
            }
            tickAt = now
            if (foreground) handler.postDelayed(this, 200)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audio = StudioAudio(this)
        if (savedInstanceState != null) {
            tab = savedInstanceState.getString("tab", "studio")
            if (savedInstanceState.getBoolean("playing")) {
                mode = Mode.entries[savedInstanceState.getInt("mode")]
                sequence = savedInstanceState.getIntArray("sequence")?.toList().orEmpty()
                basket = savedInstanceState.getInt("basket")
                sessionSlices = savedInstanceState.getInt("sessionSlices")
                secondsLeft = savedInstanceState.getLong("secondsLeft", 60000)
                sessionDay = savedInstanceState.getLong("sessionDay", LocalDate.now().toEpochDay())
                game = (lastNonConfigurationInstance as? CuttingState) ?: CuttingState(savedInstanceState.getInt("item"))
            }
        }
        if (game != null) showGame() else showTab(tab)
        if (android.os.Build.VERSION.SDK_INT >= 33) onBackInvokedDispatcher.registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT) { goBack() }
    }
    @Deprecated("Framework callback retained for native View state")
    override fun onRetainNonConfigurationInstance(): Any? = game
    override fun onSaveInstanceState(out: Bundle) {
        super.onSaveInstanceState(out)
        out.putString("tab",tab); out.putBoolean("playing",game != null)
        out.putInt("mode",mode.ordinal); out.putIntArray("sequence",sequence.toIntArray()); out.putInt("basket",basket)
        out.putInt("sessionSlices",sessionSlices); out.putLong("secondsLeft",secondsLeft); out.putLong("sessionDay",sessionDay)
        game?.let { out.putInt("item",it.item) }
    }
    override fun onResume() { super.onResume(); foreground = true; tickAt = SystemClock.elapsedRealtime(); audio.resume(); cutting?.setPaused(dialogOpen); handler.removeCallbacks(ticker); handler.post(ticker) }
    override fun onPause() { foreground = false; cutting?.setPaused(true); audio.pause(); handler.removeCallbacks(ticker); tickAt = 0; super.onPause() }
    override fun onDestroy() { handler.removeCallbacksAndMessages(null); audio.close(); super.onDestroy() }
    @Deprecated("Compatibility for API 26–32")
    @android.annotation.SuppressLint("GestureBackNavigation") // API 33+ uses the native OnBackInvokedDispatcher registered in onCreate.
    override fun onBackPressed() { goBack() }
    private fun goBack() { if (game != null) confirmLeave() else if (tab != "studio") showTab("studio") else finish() }
    private fun dp(v: Int) = (v * resources.displayMetrics.density + .5f).toInt()
    private fun rounded(color: Int, radius: Int = 22, border: Int? = null) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat(); border?.let { setStroke(dp(1),it) }
    }
    private fun text(value: String, size: Float = 15f, color: Int = ink, editorial: Boolean = false, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color); typeface = if (editorial) serif else if (bold) Typeface.create(sans,Typeface.BOLD) else sans
        includeFontPadding = false
    }
    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun row() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private fun LinearLayout.gap(height: Int = 16) { addView(View(this@MainActivity), LinearLayout.LayoutParams(1,dp(height))) }
    private fun LinearLayout.label(value: String, size: Float = 15f, color: Int = ink, editorial: Boolean = false, bold: Boolean = false): TextView = text(value,size,color,editorial,bold).also { addView(it,LinearLayout.LayoutParams(-1,-2)) }
    private fun action(value: String, primary: Boolean = true, click: () -> Unit): TextView = text(value,15f,if (primary) cream else green,bold=true).apply {
        gravity = Gravity.CENTER; minHeight = dp(54); setPadding(dp(16),dp(14),dp(16),dp(14))
        background = rounded(if(primary) green else paper,16)
        isClickable = true; isFocusable = true; contentDescription = value
        foreground = getDrawable(android.R.drawable.list_selector_background)
        setOnClickListener { click() }
    }
    private fun image(index: Int): ImageView = ImageView(this).apply {
        setImageBitmap(BitmapFactory.decodeStream(assets.open("produce/${Pantry.items[index].id}.png")))
        scaleType = ImageView.ScaleType.FIT_CENTER; importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    private fun shell(withNav: Boolean) {
        cutting = null; counter = null; timer = null; next = null
        root = column().apply { setBackgroundColor(cream) }
        root.setOnApplyWindowInsetsListener { v, insets ->
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(android.view.WindowInsets.Type.systemBars() or android.view.WindowInsets.Type.displayCutout())
                v.setPadding(bars.left,bars.top,bars.right,bars.bottom)
            } else {
                @Suppress("DEPRECATION") v.setPadding(insets.systemWindowInsetLeft,insets.systemWindowInsetTop,insets.systemWindowInsetRight,insets.systemWindowInsetBottom)
            }
            insets
        }
        body = FrameLayout(this)
        root.addView(body,LinearLayout.LayoutParams(-1,0,1f))
        if (withNav) {
            val nav = row().apply { setPadding(dp(12),dp(6),dp(12),dp(8)); background = rounded(cream,0) }
            listOf("studio" to "◒  Studio", "pantry" to "▦  Pantry", "journal" to "◷  Journal").forEach { (key,value) ->
                val button = action(value,false) { showTab(key) }.apply {
                    textSize = 13f; minHeight = dp(50); setPadding(dp(4),dp(12),dp(4),dp(12))
                    background = rounded(if (tab == key) paper else cream,14)
                    setTextColor(if (tab == key) green else muted)
                    isSelected = tab == key
                }
                nav.addView(button,LinearLayout.LayoutParams(0,-2,1f).apply { setMargins(dp(3),0,dp(3),0) })
            }
            root.addView(nav)
        }
        setContentView(root)
    }
    private fun scrollContent(): LinearLayout {
        val scroll = ScrollView(this).apply { isFillViewport = true; clipToPadding = false }
        val center = FrameLayout(this)
        val content = column().apply { setPadding(dp(24),dp(20),dp(24),dp(24)) }
        val available = resources.displayMetrics.widthPixels / resources.displayMetrics.density
        center.addView(content,FrameLayout.LayoutParams(if(available > 650) dp(650) else -1,-2,Gravity.TOP or Gravity.CENTER_HORIZONTAL))
        scroll.addView(center,ViewGroup.LayoutParams(-1,-2)); body.addView(scroll)
        return content
    }
    private fun header(content: LinearLayout, title: String, back: Boolean = false) {
        val line = row()
        line.addView(text(title,11f,green,bold=true).apply { letterSpacing = .16f },LinearLayout.LayoutParams(0,-2,1f))
        line.addView(action(if(back) "←" else "⚙",false) { if(back) goBack() else settings() }.apply {
            contentDescription = if(back) "Back" else "Open settings"; minHeight = dp(48); minWidth = dp(48); setPadding(dp(10),dp(10),dp(10),dp(10)); background = rounded(paper,24)
        },LinearLayout.LayoutParams(dp(48),dp(48)))
        content.addView(line)
    }
    private fun showTab(name: String) {
        tab = name; shell(true)
        when(name) { "pantry" -> pantry(); "journal" -> journal(); else -> studio() }
    }
    private fun studio() {
        val c = scrollContent(); header(c,"THE QUIET KITCHEN")
        c.gap(22); c.label("A little slice\nof quiet.",39f,editorial=true); c.gap(12)
        c.label("Fresh produce. Gentle sounds. Your pace.",14f,muted); c.gap(22)
        val hero = ImageView(this).apply {
            setImageBitmap(BitmapFactory.decodeStream(assets.open("kitchen-hero.webp"))); scaleType = ImageView.ScaleType.CENTER_CROP
            background = rounded(paper,24); clipToOutline = true
            contentDescription = "Watermelon, cucumber and orange on a maple cutting board"
        }
        c.addView(hero,LinearLayout.LayoutParams(-1,dp(195)))
        c.gap(18); c.addView(action("Start slicing   →") { start(Mode.FREE) }); c.gap(22)
        val section = row(); section.addView(text("Make a moment",23f,editorial=true),LinearLayout.LayoutParams(0,-2,1f)); section.addView(text("JUST FOR YOU",9f,muted,bold=true)); c.addView(section); c.gap(14)
        val modes = row(); modes.gravity = Gravity.TOP
        modeCard("01  /  FOCUS", "One quiet\nminute", "A gentle 60-second pause.",paper) { start(Mode.FOCUS) }.also { modes.addView(it,LinearLayout.LayoutParams(0,-1,1f).apply { marginEnd = dp(6) }) }
        modeCard("02  /  DAILY", "Today's\nbasket", "Five fresh picks to slice.",0xFFF1E6D5.toInt()) { start(Mode.DAILY) }.also { modes.addView(it,LinearLayout.LayoutParams(0,-1,1f).apply { marginStart = dp(6) }) }
        c.addView(modes,LinearLayout.LayoutParams(-1,dp(162))); c.gap(18)
        c.label("OFFLINE BY NATURE  ·  NO ADS  ·  NO RUSH",9f,muted,bold=true).gravity = Gravity.CENTER
    }
    private fun modeCard(kicker: String, title: String, note: String, color: Int, click: () -> Unit): LinearLayout = column().apply {
        background = rounded(color,20); setPadding(dp(16),dp(18),dp(14),dp(16)); isClickable = true; isFocusable = true; contentDescription = "$title. $note"; setOnClickListener { click() }
        label(kicker,9f,green,bold=true).letterSpacing = .09f; gap(12); label(title,23f,editorial=true); gap(10); label(note,11f,muted)
    }
    private fun pantry() {
        val c = scrollContent(); header(c,"THE PANTRY"); c.gap(22)
        c.label("Fresh things.\nLovely textures.",33f,editorial=true); c.gap(12); c.label("Twelve little reasons to slow down.\nChoose anything. Everything is ready to slice.",14f,muted); c.gap(24)
        val filters = row()
        var filter = "All"
        val grid = column()
        fun populate() {
            grid.removeAllViews()
            val list = Pantry.items.indices.filter { filter == "All" || (filter == "Vegetables") == Pantry.items[it].vegetable }
            list.chunked(2).forEach { pair ->
                val line = row(); line.gravity = Gravity.TOP
                pair.forEach { i ->
                    val card = column().apply {
                        background = rounded(Color.WHITE,20); setPadding(dp(14),dp(14),dp(14),dp(16)); isClickable = true; isFocusable = true
                        contentDescription = "Slice ${Pantry.items[i].name}. ${Pantry.items[i].note}"
                        addView(image(i),LinearLayout.LayoutParams(-1,dp(112))); gap(10); label(Pantry.items[i].name,19f,editorial=true); gap(6); label(Pantry.items[i].note,11f,muted)
                        setOnClickListener { start(Mode.FREE,i) }
                    }
                    line.addView(card,LinearLayout.LayoutParams(0,-2,1f).apply { setMargins(dp(4),0,dp(4),dp(10)) })
                }
                if(pair.size == 1) line.addView(View(this),LinearLayout.LayoutParams(0,1,1f))
                grid.addView(line)
            }
        }
        listOf("All","Fruits","Vegetables").forEach { title ->
            val b = action(title,false) {
                filter = title
                for (j in 0 until filters.childCount) (filters.getChildAt(j) as TextView).apply { background = rounded(if(text == filter) green else paper,24); setTextColor(if(text == filter) cream else green); isSelected = text == filter }
                populate()
            }.apply { textSize = 12f; minHeight = dp(48); setPadding(dp(10),dp(12),dp(10),dp(12)); if(title == "All") { background = rounded(green,24); setTextColor(cream); isSelected = true } }
            filters.addView(b,LinearLayout.LayoutParams(0,-2,1f).apply { setMargins(dp(3),0,dp(3),0) })
        }
        c.addView(filters); c.gap(18); c.addView(grid); populate()
    }
    private fun journal() {
        val c = scrollContent(); header(c,"YOUR LITTLE RITUAL"); c.gap(22); c.label("Small moments,\nwell spent.",34f,editorial=true); c.gap(12)
        c.label("A quiet record of your time in the kitchen.",14f,muted); c.gap(24)
        val stats = row()
        listOf(prefs.getInt("slices",0).toString() to "SLICES MADE", prefs.getStringSet("tried",emptySet())!!.size.toString() to "TEXTURES TRIED",prefs.getInt("baskets",0).toString() to "DAILY BASKETS").forEach { (n,label) ->
            stats.addView(column().apply { gravity = Gravity.CENTER; label(n,32f,green,editorial=true).gravity = Gravity.CENTER; gap(8); label(label,8f,muted,bold=true).gravity = Gravity.CENTER },LinearLayout.LayoutParams(0,dp(110),1f))
        }
        stats.background = rounded(paper,22); c.addView(stats); c.gap(28)
        c.label("Your tasting notes",24f,editorial=true); c.gap(10); c.label("Slice each ingredient to fill your collection.",13f,muted); c.gap(16)
        val tried = prefs.getStringSet("tried",emptySet())!!
        Pantry.items.indices.chunked(4).forEach { batch ->
            val line = row()
            batch.forEach { i ->
                line.addView(column().apply {
                    gravity = Gravity.CENTER; alpha = if(Pantry.items[i].id in tried) 1f else .35f
                    addView(image(i),LinearLayout.LayoutParams(dp(58),dp(58))); gap(8)
                    label(Pantry.items[i].name,9f,ink).gravity = Gravity.CENTER
                    contentDescription = "${Pantry.items[i].name}: ${if(Pantry.items[i].id in tried) "tried" else "not tried yet"}"
                },LinearLayout.LayoutParams(0,dp(98),1f))
            }
            c.addView(line)
        }
        c.gap(20); c.addView(action("Discover another texture   →") { showTab("pantry") }); c.gap(16)
        c.label("Your progress stays on this device.",11f,muted).gravity = Gravity.CENTER
    }
    private fun start(selected: Mode, index: Int = 0) {
        mode = selected; basket = 0; sessionSlices = 0; secondsLeft = 60000; sessionDay = LocalDate.now().toEpochDay()
        sequence = if (mode == Mode.DAILY) Pantry.dailyIndices(sessionDay) else Pantry.items.indices.toList().let { it.drop(index)+it.take(index) }
        game = CuttingState(sequence.first()); tickAt = SystemClock.elapsedRealtime(); showGame()
    }
    private fun showGame() {
        val state = game ?: return
        shell(false)
        val c = column().apply { setPadding(dp(20),dp(12),dp(20),dp(12)) }
        body.addView(c,FrameLayout.LayoutParams(-1,-1,Gravity.CENTER))
        val top = row()
        top.addView(action("←",false) { confirmLeave() }.apply { contentDescription = "Leave session"; minWidth=dp(48); minHeight=dp(48); setPadding(dp(10),dp(10),dp(10),dp(10)) },LinearLayout.LayoutParams(dp(48),dp(48)))
        top.addView(text(mode.label.uppercase(Locale.US),11f,green,bold=true).apply { gravity=Gravity.CENTER; letterSpacing=.13f },LinearLayout.LayoutParams(0,-2,1f))
        val pause = action("Ⅱ",false) { pauseSession() }.apply { contentDescription = "Pause session"; setPadding(dp(10),dp(10),dp(10),dp(10)); minHeight=dp(48) }
        top.addView(pause,LinearLayout.LayoutParams(dp(48),dp(48))); c.addView(top); c.gap(16)
        val wide = resources.configuration.screenWidthDp >= 700 || resources.configuration.screenWidthDp > resources.configuration.screenHeightDp
        val area = if(wide) row() else column()
        val details = column()
        val heading = row(); heading.addView(text(Pantry.items[state.item].name,30f,editorial=true),LinearLayout.LayoutParams(0,-2,1f))
        timer = text(if(mode==Mode.FOCUS) formatRemaining(secondsLeft) else if(mode==Mode.DAILY) "${basket+1} / 5" else "${basket%12+1} / 12",13f,green,bold=true)
        heading.addView(timer); details.addView(heading); details.gap(8); details.label(Pantry.items[state.item].note,13f,muted)
        val gameView = CuttingView(this,state) { sliced() }; cutting = gameView
        val bottom = column()
        counter = bottom.label(if(state.cuts >= 6) "Beautifully sliced." else "Swipe slowly. Find your rhythm.",18f,editorial=true).apply { gravity=Gravity.CENTER }
        bottom.gap(8)
        val progress = TextView(this).apply { textSize=13f; setTextColor(muted); typeface=sans; gravity=Gravity.CENTER; tag="progress" }
        bottom.addView(progress); bottom.gap(16)
        next = action(if(mode==Mode.DAILY && basket==4) "Finish basket   →" else "Next ingredient   →") { nextProduce() }
        bottom.addView(next); bottom.gap(8)
        bottom.addView(action("End session",false) { finishSession() }.apply { minHeight=dp(48); textSize=12f; background=rounded(cream,12); setPadding(dp(8),dp(10),dp(8),dp(10)) })
        if(wide) {
            val sidebar = column().apply { setPadding(dp(8),dp(8),dp(24),dp(8)) }
            sidebar.addView(details); sidebar.addView(View(this),LinearLayout.LayoutParams(1,0,1f)); sidebar.addView(bottom)
            area.addView(sidebar,LinearLayout.LayoutParams(0,-1,.42f)); area.addView(gameView,LinearLayout.LayoutParams(0,-1,.58f))
        } else {
            area.addView(details); area.gap(8); area.addView(gameView,LinearLayout.LayoutParams(-1,0,1f)); area.gap(16); area.addView(bottom)
        }
        c.addView(area,LinearLayout.LayoutParams(-1,0,1f)); updateProgress()
        // Standard native controls remain reachable by keyboard and screen readers.
    }
    private fun updateProgress() {
        val state = game ?: return
        body.findViewWithTag<TextView>("progress")?.text = (0 until 6).joinToString("  ") { if(it < state.cuts) "●" else "○" } + "    ${state.cuts} / 6 cuts"
        counter?.text = if(state.cuts>=6) "Beautifully sliced." else "Swipe slowly. Find your rhythm."
        next?.isEnabled = mode != Mode.DAILY || state.cuts >= 6
        next?.alpha = if(next?.isEnabled == true) 1f else .4f
    }
    private fun sliced() {
        val state = game ?: return
        sessionSlices++
        val tried = prefs.getStringSet("tried", emptySet())!!.toMutableSet().apply { add(Pantry.items[state.item].id) }
        prefs.edit().putInt("slices",prefs.getInt("slices",0)+1).putStringSet("tried",tried).apply()
        audio.cut(Pantry.items[state.item].sound)
        if(prefs.getBoolean("haptics",true)) cutting?.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        updateProgress()
        if(state.cuts == 6) cutting?.announceForAccessibility("Beautifully sliced. Next ingredient is ready.")
    }
    private fun nextProduce() {
        val state = game ?: return
        if(mode==Mode.DAILY && state.cuts<6) return
        if(mode==Mode.DAILY && basket==4) {
            val completed = prefs.getStringSet("days",emptySet())!!.toMutableSet()
            if(completed.add(sessionDay.toString())) prefs.edit().putStringSet("days",completed).putInt("baskets",prefs.getInt("baskets",0)+1).apply()
            finishSession(true); return
        }
        basket++; state.reset(sequence[basket%sequence.size]); showGame()
    }
    private fun prepareDialog() { dialogOpen=true; cutting?.setPaused(true); audio.pause() }
    private fun resumeDialog() { dialogOpen=false; tickAt=SystemClock.elapsedRealtime(); if(foreground) { cutting?.setPaused(false); audio.resume() } }
    private fun pauseSession() {
        prepareDialog()
        AlertDialog.Builder(this).setTitle("Take your time.").setMessage("Your kitchen is right here when you're ready.")
            .setPositiveButton("Keep slicing") { _,_-> }.setNegativeButton("End session") { _,_-> finishSession() }
            .create().apply { setOnDismissListener { if(game != null) resumeDialog() }; show() }
    }
    private fun confirmLeave() {
        prepareDialog()
        AlertDialog.Builder(this).setTitle("Leave the cutting board?").setMessage("Your slices and tasting notes are already saved.")
            .setPositiveButton("End session") { _,_-> finishSession() }.setNegativeButton("Keep slicing") { _,_-> }
            .create().apply { setOnDismissListener { if(game != null) resumeDialog() }; show() }
    }
    private fun finishSession(completed: Boolean = false) {
        if(game == null) return
        val wasFocus = mode == Mode.FOCUS
        game = null; dialogOpen=false; cutting=null
        showTab("journal"); audio.resume()
        AlertDialog.Builder(this).setTitle(if(completed) "A basket, beautifully done." else if(wasFocus && secondsLeft<=0) "A minute well spent." else "A lovely little pause.")
            .setMessage("$sessionSlices satisfying slices.\nYour progress is saved on this device.")
            .setPositiveButton("Lovely") { _,_-> }.setNegativeButton("Slice again") { _,_-> start(mode) }.show()
    }
    private fun settings() {
        val content = column().apply { setPadding(dp(24),dp(8),dp(24),dp(8)) }
        fun toggle(label: String, key: String, default: Boolean) {
            content.addView(Switch(this).apply {
                text=label; textSize=15f; typeface=sans; setTextColor(ink); minHeight=dp(56); isChecked=prefs.getBoolean(key,default)
                setOnCheckedChangeListener { _,checked -> prefs.edit().putBoolean(key,checked).apply(); audio.updateAmbience() }
            },LinearLayout.LayoutParams(-1,-2))
        }
        toggle("Cutting sounds","sound",true); toggle("Kitchen ambience","ambience",false); toggle("Gentle haptics","haptics",true); toggle("Reduce motion","motion",false)
        content.gap(12); content.label("Sound volume",13f,muted)
        content.addView(SeekBar(this).apply {
            max=100; progress=prefs.getInt("volume",75); contentDescription="Sound volume"; minimumHeight=dp(48)
            setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seek: SeekBar?, value: Int, user: Boolean) { if(user) { prefs.edit().putInt("volume",value).apply(); audio.updateAmbience() } }
                override fun onStartTrackingTouch(seek: SeekBar?) {}
                override fun onStopTrackingTouch(seek: SeekBar?) {}
            })
        })
        content.gap(8); content.label("Your cutting board",13f,muted); content.gap(8)
        val boards = RadioGroup(this).apply { orientation=RadioGroup.HORIZONTAL }
        listOf("Maple","Walnut","Sage").forEachIndexed { i,name ->
            boards.addView(RadioButton(this).apply { id=100+i; text=name; textSize=12f; typeface=sans; minHeight=dp(48) },LinearLayout.LayoutParams(0,-2,1f))
        }
        boards.check(100+prefs.getInt("board",0)); boards.setOnCheckedChangeListener { _,id -> prefs.edit().putInt("board",id-100).apply() }; content.addView(boards)
        content.gap(12); content.addView(action("Privacy policy",false) { showPrivacy() }); content.gap(8)
        content.addView(action("Contact support",false) {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:seandyadryan2@gmail.com?subject=ASMR%20Cutting%20Fruit%20support"))
            try { startActivity(intent) } catch(_: android.content.ActivityNotFoundException) { AlertDialog.Builder(this).setMessage("Email us at seandyadryan2@gmail.com").setPositiveButton("OK",null).show() }
        }); content.gap(16); content.label("ASMR Cutting Fruit  •  1.0.0\nPT Deploy Dulu Pulang Nanti",11f,muted).gravity=Gravity.CENTER
        val scroll = ScrollView(this).apply { addView(content) }
        AlertDialog.Builder(this).setTitle("Make yourself at home").setView(scroll).setPositiveButton("Done",null).show()
    }
    private fun showPrivacy() {
        val copy = "ASMR Cutting Fruit works entirely offline. We do not collect, transmit, share or sell personal data. No account, ads, analytics, tracking or in-app purchases are included.\n\nYour slice count, tried ingredients, completed daily baskets and sound, haptic and board preferences are stored only on your device. Android backup is disabled. Use Android Settings → Apps → ASMR Cutting Fruit → Storage → Clear storage, or uninstall, to erase them.\n\nVibration is used only for optional haptic feedback. The app does not request internet, camera, microphone, location or storage permissions. Contacting support opens your email app; the information you choose to email is used to answer your request.\n\nDeveloper: PT Deploy Dulu Pulang Nanti\nSupport: seandyadryan2@gmail.com\nEffective: November 8, 2026"
        AlertDialog.Builder(this).setTitle("Privacy, kept simple").setMessage(copy).setPositiveButton("Close",null).show()
    }
}
