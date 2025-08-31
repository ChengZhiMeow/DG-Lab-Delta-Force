@file:Suppress("FunctionName", "SpellCheckingInspection")

package cn.chengzhiya.dglabdeltaforce.jna.win

import com.sun.jna.Native
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.platform.win32.WinUser
import com.sun.jna.ptr.IntByReference
import com.sun.jna.win32.StdCallLibrary
import com.sun.jna.win32.W32APIOptions

internal interface Shcore : StdCallLibrary, WinNT {
    fun GetDpiForMonitor(hmonitor: WinUser.HMONITOR?, dpiType: Int?, dpiX: IntByReference?, dpiY: IntByReference?): Int

    companion object {
        val INSTANCE: Shcore by lazy { Native.load("shcore", Shcore::class.java, W32APIOptions.DEFAULT_OPTIONS) }
    }
}