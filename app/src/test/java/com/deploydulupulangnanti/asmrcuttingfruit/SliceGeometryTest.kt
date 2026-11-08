package com.deploydulupulangnanti.asmrcuttingfruit

import org.junit.Assert.*
import org.junit.Test

class SliceGeometryTest {
    private val square = listOf(Vec(-1f,-1f), Vec(1f,-1f), Vec(1f,1f), Vec(-1f,1f))
    @Test fun splitConservesAreaAndSupportsEitherDirection() {
        for ((a,b) in listOf(Vec(-2f,0f) to Vec(2f,0f), Vec(0f,2f) to Vec(0f,-2f), Vec(-2f,-2f) to Vec(2f,2f))) {
            val (left,right) = SliceGeometry.split(square,a,b)!!
            assertEquals(4f, SliceGeometry.area(left) + SliceGeometry.area(right), .0001f)
        }
    }
    @Test fun gesturesOutsideFruitDoNotSlice() {
        assertNull(SliceGeometry.split(square, Vec(2f,0f), Vec(3f,0f)))
        assertNull(SliceGeometry.split(square, Vec(-2f,2f), Vec(2f,2f)))
        assertNull(SliceGeometry.split(square, Vec(0f,0f), Vec(0f,0f)))
    }
    @Test fun tinySliversAreRejected() {
        assertNull(SliceGeometry.split(square, Vec(-2f,.999f), Vec(2f,.999f)))
    }
    @Test fun dailyBasketIsStableDistinctAndChanges() {
        assertEquals(Pantry.dailyIndices(100), Pantry.dailyIndices(100))
        assertEquals(5, Pantry.dailyIndices(100).distinct().size)
        assertNotEquals(Pantry.dailyIndices(100), Pantry.dailyIndices(101))
    }
    @Test fun timerFormatsMinuteBoundariesWithoutSixtySeconds() {
        assertEquals("1:00",formatRemaining(60000))
        assertEquals("1:00",formatRemaining(59999))
        assertEquals("0:59",formatRemaining(59000))
        assertEquals("0:01",formatRemaining(1))
        assertEquals("0:00",formatRemaining(0))
        assertEquals("0:00",formatRemaining(-1))
    }
}
