@file:Suppress("FunctionName", "SpellCheckingInspection")

package cn.chengzhiya.dglabdeltaforce.jna.win

import com.sun.jna.Native
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.win32.W32APIOptions

interface User32Ext : User32 {
    fun GetWindowDC(hwnd: WinDef.HWND?): WinDef.HDC?

    fun ClientToScreen(
        hwnd: WinDef.HWND?,
        point: WinDef.POINT
    ): Boolean

    companion object {
        val INSTANCE: User32Ext by lazy { Native.load("user32", User32Ext::class.java, W32APIOptions.DEFAULT_OPTIONS) }
    }
}