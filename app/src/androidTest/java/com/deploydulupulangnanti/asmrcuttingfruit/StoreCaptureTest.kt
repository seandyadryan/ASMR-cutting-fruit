package com.deploydulupulangnanti.asmrcuttingfruit

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Store art uses real native View rendering on a running Android device, without OS overlays. */
@RunWith(AndroidJUnit4::class)
class StoreCaptureTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private lateinit var scenario: ActivityScenario<MainActivity>
    private lateinit var activity: MainActivity
    private val kind get() = InstrumentationRegistry.getArguments().getString("captureKind", "phone")
    private fun fresh() {
        if(::scenario.isInitialized) scenario.close()
        scenario=ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity=it }
        instrumentation.waitForIdleSync()
    }
    private fun views(): List<View> {
        val all=mutableListOf<View>()
        fun visit(view: View) { all+=view; if(view is ViewGroup) for(i in 0 until view.childCount) visit(view.getChildAt(i)) }
        visit(activity.window.decorView); return all
    }
    private fun click(label: String, description: Boolean=false) {
        scenario.onActivity {
            val view=views().first { if(description) it.contentDescription?.toString()?.startsWith(label)==true else it is TextView && it.text.toString()==label }
            check(view.isEnabled); view.performClick()
        }
        instrumentation.waitForIdleSync()
    }
    private fun slice() {
        scenario.onActivity { views().filterIsInstance<CuttingView>().single().performAccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK,null) }
        instrumentation.waitForIdleSync()
    }
    private fun capture(name: String, scale: Float=1f) {
        var bitmap: Bitmap?=null
        scenario.onActivity {
            val view=activity.window.decorView
            check(view.width>0 && view.height>0)
            bitmap=Bitmap.createBitmap((view.width*scale).toInt(),(view.height*scale).toInt(),Bitmap.Config.ARGB_8888).also {
                val canvas=Canvas(it); canvas.scale(scale,scale); view.draw(canvas)
            }
        }
        val folder=File(activity.filesDir,"store-captures").apply { mkdirs() }
        File(folder,"$name.png").outputStream().use { bitmap!!.compress(Bitmap.CompressFormat.PNG,100,it) }
        bitmap!!.recycle()
    }
    @Test fun captureNativeScreens() {
        try {
            if(InstrumentationRegistry.getArguments().getString("screen")=="focus") {
                fresh(); click("One quiet",true); capture("$kind-focus"); return
            }
            fresh(); capture("$kind-studio")
            click("Start slicing   →"); repeat(3) { slice() }; capture("$kind-cutting")
            if(kind=="phone") {
                click("Next ingredient   →")
                repeat(60) { frame ->
                    if(frame%10==0) slice()
                    capture("gameplay-frame-${frame.toString().padStart(3,'0')}",.5f)
                    Thread.sleep(100)
                }
            }
            fresh(); click("▦  Pantry"); capture("$kind-pantry")
            fresh(); click("One quiet",true); capture("$kind-focus")
            if(kind=="phone") {
                fresh(); click("◷  Journal"); capture("phone-journal")
                fresh(); click("Today's",true); capture("phone-daily")
            }
        } finally { if(::scenario.isInitialized) scenario.close() }
    }
}
