from flask import Flask, request
import cv2
import numpy as np
import easyocr
from typing import Union

import config

app = Flask(__name__)

reader = None
def _initialize_reader():
    """
    初始化EasyOCR Reader。
    这会确保模型只在第一次需要时加载一次。
    """
    global reader
    if reader is None:
        try:
            import config
            print(f"正在加载EasyOCR模型到 '{config.MODEL_DIR}'... (首次运行可能需要下载)")
            reader = easyocr.Reader(['en'], gpu=False, model_storage_directory=config.MODEL_DIR)
            print("模型加载完毕。")
        except Exception as e:
            print(f"初始化EasyOCR失败: {e}")

def load_image_from_bytes(image_bytes: bytes) -> Union[np.ndarray, None]:
    """从bytes加载图像为OpenCV格式。"""
    try:
        image_np_array = np.frombuffer(image_bytes, np.uint8)
        image = cv2.imdecode(image_np_array, cv2.IMREAD_COLOR)
        if image is None:
            print("错误: 传入的bytes数据无法解码为图像。")
            return None
        return image
    except Exception as e:
        print(f"加载图像时发生错误: {e}")
        return None

@app.route('/ocr', methods=['POST'])
def ocr():
    """
    处理 OCR 请求。
    """
    data = request.data
    if not data:
        return None, 200

    image = load_image_from_bytes(data)
    
    if image.size == 0:
        return None, 200

    result_list = reader.readtext(image, detail=0, paragraph=True, allowlist=config.WHITELIST)
    text = " ".join(result_list) if result_list else ""

    print(f"识别成功, 内容: {text}!")
    return text, 200

def start():
    _initialize_reader()
    app.run(host='0.0.0.0', port=config.SERVER_PORT, debug=True)