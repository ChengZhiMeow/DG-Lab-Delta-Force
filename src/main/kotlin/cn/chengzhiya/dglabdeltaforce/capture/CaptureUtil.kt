package cn.chengzhiya.dglabdeltaforce.capture

import cn.chengzhiya.dglabdeltaforce.jna.Window
import cn.chengzhiya.dglabdeltaforce.jna.ext.Capture
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.PointerByReference
import java.awt.image.BufferedImage

object CaptureUtil {
    fun captureWindow(window: Window): BufferedImage {
        val bufferRef = PointerByReference()
        val widthRef = IntByReference()
        val heightRef = IntByReference()
        val bufferSizeRef = IntByReference()
        Capture.INSTANCE.CaptureByHwnd(
            window.hwnd,
            bufferRef,
            widthRef,
            heightRef,
            bufferSizeRef
        )

        val capturedBuffer = bufferRef.value
        val width = widthRef.value
        val height = heightRef.value
        val bufferSize = bufferSizeRef.value

        try {
            val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            val bgraData = capturedBuffer.getByteArray(0, bufferSize)
            val argbData = IntArray(width * height)

            for (i in argbData.indices) {
                val b = bgraData[i * 4].toInt() and 0xFF
                val g = bgraData[i * 4 + 1].toInt() and 0xFF
                val r = bgraData[i * 4 + 2].toInt() and 0xFF
                val a = bgraData[i * 4 + 3].toInt() and 0xFF
                argbData[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }

            image.setRGB(0, 0, width, height, argbData, 0, width)

            return image
        } finally {
            Capture.INSTANCE.FreeCaptureBuffer(capturedBuffer)
        }
    }
}