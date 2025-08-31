package cn.chengzhiya.dglabdeltaforce.jna

import com.sun.jna.platform.win32.WinDef

data class Window(
    val hwnd: WinDef.HWND,
    val path: String,
    val name: String
)
