# --- OCR 和图像处理配置 ---
# EasyOCR 模型存放目录
MODEL_DIR = './models'
# 是否在预处理时对图像进行锐化
ENABLE_SHARPENING = True

# --- API 和网络配置 ---
SERVER_URL = "http://127.0.0.1:8920"
CLIENT_ID = "65489a26-09f8-4638-bafe-f4f8fadd371c"
# 是否打印详细的API请求和响应信息
ENABLE_API_DEBUG = False

# --- 核心逻辑配置 ---
# 主循环的间隔时间（毫秒）
LOOP_INTERVAL_MS = 1000
# 强度值的范围
MIN_STRENGTH_VALUE = 3
MAX_STRENGTH_VALUE = 100
# 伤害转换乘数 (每损失1%血量，增加多少强度)
DAMAGE_MULTIPLIER = 1

# --- 扫描区域定义 ---
# 每个区域包含:
#   id: 唯一标识符
#   ref_point: 参考点 ('top-left' 或 'bottom-left')
#   coords: 包含 'tl' (左上) 和 'br' (右下) 的相对坐标
REGIONS_TO_SCAN = [
    {"id": "地图时间", "ref_point": "top-left", "coords": {"tl": (182, 454), "br": (252, 482)}},
    {"id": "血量显示", "ref_point": "bottom-left", "coords": {"tl": (118, -155), "br": (254, -119)}},
]

# --- 颜色检测区域定义 ---
COLOR_CHECK_REGION = {
    "name": "血量条",
    "ref_point": "bottom-left",
    "coords": {"tl": (127, -115), "br": (452, -108)},
    "color_bgr": [
        {"color": (205, 205, 205), "tolerance": 80},
        {"color": (135, 135, 135), "tolerance": 5}
    ]
}