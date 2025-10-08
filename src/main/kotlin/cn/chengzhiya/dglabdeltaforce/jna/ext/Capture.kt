package cn.chengzhiya.dglabdeltaforce.jna.ext

import cn.chengzhiya.dglabdeltaforce.Main
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinNT.HRESULT
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.PointerByReference
import com.sun.jna.win32.StdCallLibrary

interface Capture : StdCallLibrary {
    fun InitializeCapture()

    fun DeinitializeCapture()

    fun CaptureByHwnd(
        hwnd: HWND?,
        outBuffer: PointerByReference?,
        outWidth: IntByReference?,
        outHeight: IntByReference?,
        outBufferSize: IntByReference?
    ): HRESULT

    fun FreeCaptureBuffer(buffer: Pointer?)

    companion object {
        val INSTANCE by lazy { Native.load(Main.captureDll.absolutePath, Capture::class.java)!! }
    }
}