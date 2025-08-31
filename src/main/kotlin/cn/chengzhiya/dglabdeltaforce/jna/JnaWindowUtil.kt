package cn.chengzhiya.dglabdeltaforce.jna

import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.Psapi
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.ptr.IntByReference

object JnaWindowUtil {
    fun getWindowByName(name: String): Window? {
        var result: Window? = null
        val user32 = User32.INSTANCE

        user32.EnumWindows({ hwnd, data ->
            if (!user32.IsWindowVisible(hwnd) || user32.GetWindowTextLength(hwnd) == 0) return@EnumWindows true

            val pidRef = IntByReference()
            user32.GetWindowThreadProcessId(hwnd, pidRef)
            val processHandle =
                Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_QUERY_LIMITED_INFORMATION, false, pidRef.value)
                    ?: return@EnumWindows true

            try {
                val filenameBuffer = CharArray(1024)
                val length =
                    Psapi.INSTANCE.GetModuleFileNameExW(processHandle, null, filenameBuffer, filenameBuffer.size)

                val processPath = String(filenameBuffer, 0, length).replace("/", "\\")
                val processName = processPath.substring(processPath.lastIndexOf("\\") + 1)

                if (!processName.equals(name, ignoreCase = true)) return@EnumWindows true

                result = Window(hwnd, processPath, processName)
                return@EnumWindows false
            } finally {
                Kernel32.INSTANCE.CloseHandle(processHandle)
            }
        }, null)

        return result
    }
}