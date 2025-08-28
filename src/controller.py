import mss
import numpy as np
import cv2
import time
import requests
from typing import List, Dict

from src.utils import ocr
from src.utils import window

class GameController:
    def __init__(self, config):
        """
        初始化控制器，并从配置对象中加载所有设置。
        """
        self.config = config
        self.game_started = False
        self.last_sent_strength = None
        self.api_url = f"{self.config.SERVER_URL}/api/v2/game/{self.config.CLIENT_ID}/strength"
        self.headers = {'Content-Type': 'application/json'}
        self.no_results_count = 0

    def _preprocess_image(self, img_np: np.ndarray) -> np.ndarray:
        final_img = cv2.cvtColor(img_np, cv2.COLOR_BGRA2GRAY)

        if self.config.ENABLE_SHARPENING:
            sharpen_kernel = np.array([[0, -1, 0], [-1, 5, -1], [0, -1, 0]])
            final_img = cv2.filter2D(final_img, -1, sharpen_kernel)

        return final_img

    def _send_strength(self, strength_value: int):
        if strength_value == self.last_sent_strength:
            print(f"  - [API] 强度值 {strength_value} 未变化，跳过发送。")
            return

        payload = {"strength": {"set": strength_value}, "randomStrength": {"set": 1}}
        try:
            response = requests.post(self.api_url, json=payload, headers=self.headers, timeout=5)
            response.raise_for_status()
            self.last_sent_strength = strength_value
            print(f"  - [API] 强度值变化为 {strength_value}。")
            if self.config.ENABLE_API_DEBUG:
                print(f"  - [API] 请求成功: Body={payload}, 响应: {response.json()}")
        except requests.exceptions.RequestException as e:
            print(f"  - [API] 请求失败: {e}")

    def _check_color_percentage(self, full_image_np: np.ndarray):
        h, _, _ = full_image_np.shape
        config = self.config.COLOR_CHECK_REGION
        x1, y1_offset = config["coords"]["tl"]
        x2, y2_offset = config["coords"]["br"]
        
        abs_y1 = h + y1_offset
        abs_y2 = h + y2_offset
        roi = full_image_np[abs_y1:abs_y2, x1:x2]
        
        if roi.size == 0:
            print(f"  - [颜色检测] 警告: 区域 '{config['name']}' 裁剪后为空，请检查坐标。")
            return 0

        roi_bgr = cv2.cvtColor(roi, cv2.COLOR_BGRA2BGR)
        value = 0
        for color in config["color_bgr"]:
            target_color = np.array(color["color"])
            tolerance = color["tolerance"]
            lower_bound = np.clip(target_color - tolerance, 0, 255)
            upper_bound = np.clip(target_color + tolerance, 0, 255)
            mask = cv2.inRange(roi_bgr, lower_bound, upper_bound)
            match_count = cv2.countNonZero(mask)
            total_pixels = roi.shape[0] * roi.shape[1]
            percentage = (match_count / total_pixels) * 100 if total_pixels > 0 else 0
            if percentage > value:
                value = percentage
        return value

    def _stop_game(self):
        self.game_started = False
        self._send_strength(self.config.MIN_STRENGTH_VALUE)

    def _process_ocr_results(self, ocr_data: List[Dict], original_color_img: np.ndarray):
        results = {item['id']: item['text'] for item in ocr_data if item.get('id') and item.get('text')}
        
        if not results:
            self.no_results_count += 1
            if self.game_started and self.no_results_count >= 5:
                print("  - [判断] 超过5次未识别到游戏界面，判定游戏结束。")
                self._stop_game()
            print(f"  - [判断] 第 {self.no_results_count} 次找不到任何游戏界面组件。")
            return
        else: 
            self.no_results_count = 0
        
        map_time_text = results.get("地图时间")
        health_text = results.get("血量显示")
        has_map_time = bool(map_time_text and sum(c.isdigit() for c in map_time_text) / len(map_time_text) >= 0.75)
        has_health = "/" in health_text if health_text else False
        
        if map_time_text: print(f"  - [计算] 识别地图时间: {map_time_text}")
        if health_text: print(f"  - [计算] 识别血量文本: {health_text}")

        if not self.game_started and has_map_time and has_health:
            print("  - [判断] 检测到地图时间和血量，判定游戏开始。")
            self.game_started = True
            self.last_sent_strength = None
        
        if self.game_started and not has_map_time and not has_health:
            print(f"  - [判断] 检测不到地图时间与血量，判定游戏结束。")
            self._stop_game()
        
        if self.game_started:
            try:
                health_value = int(health_text.split('/')[0].strip())
            except (ValueError, IndexError, AttributeError):
                health_value = 100
                print(f"  - [警告] 无法从 '{health_text}' 中解析当前血量，默认100。")

            health_bar = int(self._check_color_percentage(original_color_img))
            print(f"  - [计算] 识别血量条比例: {health_bar}")
            
            final_health = health_value
            if abs(health_value - health_bar) >= 2:
                final_health = health_bar
                print(f"  - [警告] OCR血量与血条比例差距过大，使用比例值({health_bar})作为当前血量。")
            
            calculated_strength = (100 - final_health) * self.config.DAMAGE_MULTIPLIER
            current_strength = max(self.config.MIN_STRENGTH_VALUE, min(calculated_strength, self.config.MAX_STRENGTH_VALUE))
            self._send_strength(int(current_strength))

    def run(self):
        print("自动化任务已启动...")
        print(f"将以每 {self.config.LOOP_INTERVAL_MS} 毫秒的间隔进行截图和识别。")
        print(f"图像预处理: 锐化={'启用' if self.config.ENABLE_SHARPENING else '禁用'}")
        print("按 Ctrl+C 停止程序。")

        loop_counter = 0
        interval_seconds = self.config.LOOP_INTERVAL_MS / 1000.0

        with mss.mss() as sct:
            while True:
                try:
                    start_time = time.time()
                    loop_counter += 1

                    print(f"\n--- 使用本工具的第 {loop_counter} 秒 ---")
                    if window.get_active_process_name() == self.config.PROCESS_NAME:
                        monitor = sct.monitors[1]
                        sct_img = sct.grab(monitor)
                        img_np = np.array(sct_img)
                    
                        processed_img_np = self._preprocess_image(img_np)
                        is_success, img_bytes_buffer = cv2.imencode(".png", processed_img_np)
                    
                        if not is_success:
                            print("  - [错误] 截图编码失败。")
                            time.sleep(interval_seconds)
                            continue
                    
                        ocr_results = ocr.recognize_text_in_regions(
                            img_bytes_buffer.tobytes(), self.config.REGIONS_TO_SCAN
                        )
                    
                        self._process_ocr_results(ocr_results, img_np)
                    else:
                        print("  - [错误] 当前窗口句柄并不在三角洲，所以不做处理。")

                    processing_time = time.time() - start_time
                    sleep_time = interval_seconds - processing_time
                    if sleep_time > 0:
                        time.sleep(sleep_time)
                    else:
                        print(f"  - [警告] 循环 {loop_counter} 耗时 {processing_time:.2f}s, 超出设定间隔 {interval_seconds:.2f}s。")

                except KeyboardInterrupt:
                    print("\n程序被用户中断。正在退出...")
                    if self.game_started:
                        print("游戏正在运行，发送最低强度值以停止...")
                        self._stop_game()
                    break
                except Exception as e:
                    import traceback
                    print(f"  - [错误] 主循环发生未预料的错误: {e}")
                    traceback.print_exc()
                    print("  - [错误] 5秒后重试...")
                    time.sleep(5)