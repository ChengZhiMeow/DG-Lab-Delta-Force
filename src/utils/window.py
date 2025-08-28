import win32gui
import win32process
import psutil

def get_active_process_name():
    """
    专门用于 Windows 系统，获取当前活跃窗口的进程名称
    :return: 进程名称字符串，如 "chrome.exe"
    """
    try:
        hwnd = win32gui.GetForegroundWindow()
        _, pid = win32process.GetWindowThreadProcessId(hwnd)
    
        process = psutil.Process(pid)
        return process.name()
    except Exception as _:
        return None