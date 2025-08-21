import cv2

drawing = False
start_point = (-1, -1)
end_point = (-1, -1)
original_image = None
image_to_show = None

def select_and_get_coords(image_path):
    """主函数，加载图像并处理用户交互。"""
    global original_image, image_to_show

    original_image = cv2.imread(image_path)
    if original_image is None:
        print(f"错误：无法加载图像，请检查路径：'{image_path}'")
        return

    window_name = "OCR Region Selector - Drag to draw, 'r' to reset, 'q' to quit"
    cv2.namedWindow(window_name, cv2.WINDOW_NORMAL)
    cv2.resizeWindow(window_name, 1280, 720) 
    cv2.setMouseCallback(window_name, draw_rectangle)
    
    print("--- 操作指南 ---")
    print("1. 在图片上按住鼠标左键并拖动来选择一个区域。")
    print("2. 松开鼠标左键完成选择，坐标将会打印在控制台。")
    print("3. 如果图片过大，可以使用窗口的滚动条。")
    print("4. 按 'r' 键可以重置选择框。")
    print("5. 按 'q' 键或 'ESC' 键退出程序。")
    print("-----------------")

    image_to_show = original_image.copy()

    while True:
        cv2.imshow(window_name, image_to_show)
        key = cv2.waitKey(1) & 0xFF

        if key == ord('r'):
            print("\n...已重置选框...")
            image_to_show = original_image.copy()
            reset_points()

        elif key == ord('q') or key == 27:
            break
            
    cv2.destroyAllWindows()

    if start_point != (-1, -1) and end_point != (-1, -1):
        print("\n--- 最终选定的区域坐标 ---")
        calculate_and_print_coordinates()
    else:
        print("\n程序已退出，未选择任何区域。")

def draw_rectangle(event, x, y, flags, param):
    """鼠标回调函数，处理鼠标事件。"""
    global start_point, end_point, drawing, image_to_show

    if event == cv2.EVENT_LBUTTONDOWN:
        drawing = True
        start_point = (x, y)
        end_point = (-1, -1) 
        image_to_show = original_image.copy()
    elif event == cv2.EVENT_MOUSEMOVE:
        if drawing:
            temp_image = image_to_show.copy()
            cv2.rectangle(temp_image, start_point, (x, y), (0, 255, 0), 2)
            cv2.imshow("OCR Region Selector - Drag to draw, 'r' to reset, 'q' to quit", temp_image)
    elif event == cv2.EVENT_LBUTTONUP:
        drawing = False
        end_point = (x, y)
        cv2.rectangle(image_to_show, start_point, end_point, (0, 255, 0), 2)
        calculate_and_print_coordinates()

def calculate_and_print_coordinates():
    """计算并打印选定框相对于画面五个固定点的像素偏移坐标。"""
    if start_point == (-1, -1) or end_point == (-1, -1): return

    img_height, img_width, _ = original_image.shape
    screen_positions = {
        "画面左上角": (0, 0),
        "画面右上角": (img_width, 0),
        "画面左下角": (0, img_height),
        "画面右下角": (img_width, img_height),
        "画面中心": (img_width // 2, img_height // 2)
    }

    x1, y1 = start_point
    x2, y2 = end_point
    box_tl = (min(x1, x2), min(y1, y2))
    box_tr = (max(x1, x2), min(y1, y2))
    box_bl = (min(x1, x2), max(y1, y2))
    box_br = (max(x1, x2), max(y1, y2))
    
    box_corners = {
        "选框-左上角": box_tl, "选框-右上角": box_tr,
        "选框-左下角": box_bl, "选框-右下角": box_br
    }

    print(f"\n--- 选定框信息 (绝对像素坐标) ---")
    print(f"图像尺寸: 宽={img_width}, 高={img_height}")
    for name, coord in box_corners.items():
        print(f"  {name}: {coord}")
    print("-" * 30)

    for screen_name, screen_coord in screen_positions.items():
        print(f"\n--- 相对于 [{screen_name}] 的偏移坐标 (dx, dy) ---")
        ref_x, ref_y = screen_coord
        for box_name, box_coord in box_corners.items():
            box_x, box_y = box_coord
            relative_coord = (box_x - ref_x, box_y - ref_y)
            print(f"  {box_name}: {relative_coord}")
    print("\n" + "="*40 + "\n")

def reset_points():
    """重置全局坐标点。"""
    global start_point, end_point, drawing
    drawing = False
    start_point = (-1, -1)
    end_point = (-1, -1)