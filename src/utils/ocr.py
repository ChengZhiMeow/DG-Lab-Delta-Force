import cv2
import numpy as np
import easyocr
from typing import Union, List, Dict

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
            reader = False
    return reader

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

def recognize_text_in_regions(image_bytes: bytes, regions: List[Dict]) -> List[Dict]:
    """
    在图像的指定区域上执行OCR，并返回结构化结果。
    """
    local_reader = _initialize_reader()
    if not local_reader:
        print("OCR功能不可用，请检查初始化错误。")
        return []

    image = load_image_from_bytes(image_bytes)
    if image is None:
        return []

    img_height, img_width, _ = image.shape
    ocr_results = []

    for region in regions:
        id = region.get('id', '无ID')
        ref_point = region.get('ref_point')
        coords = region.get('coords')

        if not all([ref_point, coords]):
            print(f"警告: 跳过区域 '{id}'，因其定义不完整。")
            continue

        x1, y1, x2, y2 = 0, 0, 0, 0
        if ref_point == 'top-left':
            x1, y1 = coords['tl']
            x2, y2 = coords['br']
        elif ref_point == 'bottom-left':
            ref_x, ref_y = 0, img_height
            x1 = ref_x + coords['tl'][0]
            y1 = ref_y + coords['tl'][1]
            x2 = ref_x + coords['br'][0]
            y2 = ref_y + coords['br'][1]
        else:
            print(f"警告: 跳过区域 '{id}'，因为参考点 '{ref_point}' 未知。")
            continue
            
        x1, y1, x2, y2 = int(x1), int(y1), int(x2), int(y2)
        if not (0 <= y1 < y2 <= img_height and 0 <= x1 < x2 <= img_width):
            print(f"错误: 区域 '{id}' 的计算后坐标 [{x1},{y1} -> {x2},{y2}] 超出图像边界!")
            continue

        cropped_image = image[y1:y2, x1:x2]
        result_list = local_reader.readtext(cropped_image, detail=0, paragraph=True, allowlist='0123456789/')
        recognized_text = " ".join(result_list) if result_list else ""
        
        ocr_results.append({
            "id": id,
            "zone": (x1, y1, x2, y2),
            "text": recognized_text
        })

    return ocr_results
