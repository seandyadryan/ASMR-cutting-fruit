package com.deploydulupulangnanti.asmrcuttingfruit

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.lifecycle.Lifecycle
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView

@RunWith(AndroidJUnit4::class)
class StudioFlowTest {
    @get:Rule val scenario = ActivityScenarioRule(MainActivity::class.java)
    private lateinit var activity: MainActivity
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    @Before fun attach() { scenario.scenario.onActivity { activity = it } }
    private fun runTestOnUiThread(action: () -> Unit) { scenario.scenario.onActivity { activity=it; action() } }
    private fun views(): List<View> {
        val result=mutableListOf<View>()
        fun walk(v: View) { result+=v; if(v is ViewGroup) for(i in 0 until v.childCount) walk(v.getChildAt(i)) }
        walk(activity.window.decorView); return result
    }
    private fun click(label: String) {
        var found=false
        runTestOnUiThread {
            val view=views().filterIsInstance<TextView>().firstOrNull { it.text.toString()==label }
            assertNotNull("Missing control: $label",view)
            assertTrue("Disabled control: $label",view!!.isEnabled)
            found=view.performClick()
        }
        instrumentation.waitForIdleSync(); assertTrue(found)
    }
    private fun cutSixTimes() {
        repeat(6) {
            runTestOnUiThread {
                views().filterIsInstance<CuttingView>().single().performAccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK,null)
            }
            instrumentation.waitForIdleSync()
        }
    }
    @Test fun freeSlicePersistsProgressAndAdvancesIngredient() {
        click("Start slicing   →"); cutSixTimes()
        runTestOnUiThread { assertTrue(views().filterIsInstance<TextView>().any { it.text.toString().contains("6 / 6 cuts") }) }
        val count=activity.getSharedPreferences("studio",0).getInt("slices",0)
        assertTrue("Every successful cut should be saved",count>=6)
        click("Next ingredient   →")
        runTestOnUiThread { assertTrue(views().filterIsInstance<TextView>().any { it.text.toString()=="Kiwi" }) }
    }
    @Test fun dailyBasketRequiresSixCutsAndSavesCompletion() {
        val preferences=activity.getSharedPreferences("studio",0)
        val before=preferences.getInt("baskets",0)
        val day=java.time.LocalDate.now().toEpochDay().toString()
        val alreadyDone=day in preferences.getStringSet("days",emptySet())!!
        runTestOnUiThread { views().first { it.contentDescription?.toString()?.startsWith("Today's") == true }.performClick() }
        instrumentation.waitForIdleSync()
        repeat(5) { basket ->
            val label=if(basket==4) "Finish basket   →" else "Next ingredient   →"
            runTestOnUiThread { assertFalse(views().filterIsInstance<TextView>().single { it.text.toString()==label }.isEnabled) }
            cutSixTimes(); click(label)
        }
        assertTrue(day in preferences.getStringSet("days",emptySet())!!)
        assertEquals(before+if(alreadyDone) 0 else 1,preferences.getInt("baskets",0))
    }
    @Test fun focusPausesInBackgroundAndFinishesAtExpiry() {
        runTestOnUiThread { views().first { it.contentDescription?.toString()?.startsWith("One quiet") == true }.performClick() }
        instrumentation.waitForIdleSync()
        val remaining=MainActivity::class.java.getDeclaredField("secondsLeft").apply { isAccessible=true }
        scenario.scenario.moveToState(Lifecycle.State.STARTED)
        var before=0L
        runTestOnUiThread { before=remaining.getLong(activity) }
        Thread.sleep(1200)
        runTestOnUiThread { assertEquals(before,remaining.getLong(activity)) }
        scenario.scenario.moveToState(Lifecycle.State.RESUMED)
        runTestOnUiThread { remaining.setLong(activity,500L) }
        Thread.sleep(1500); instrumentation.waitForIdleSync()
        runTestOnUiThread { assertTrue(views().filterIsInstance<TextView>().any { it.text.toString()=="Small moments,\nwell spent." }) }
    }
}
