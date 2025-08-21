import argparse
import os
from src.controller import GameController
from src.utils.zone_selector import select_and_get_coords
import config

def run_controller():
    """启动游戏自动化控制器"""
    print("正在初始化游戏控制器...")
    controller = GameController(config)
    controller.run()

def run_zone_selector(image_path):
    """启动区域选择工具"""
    if not os.path.exists(image_path):
        print(f"错误：找不到指定的图片文件 '{image_path}'")
        print("请确保图片存在，或使用 --image 参数指定正确的路径。")
        return
    
    print(f"正在为图片 '{image_path}' 启动区域选择工具...")
    select_and_get_coords(image_path)

if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description="郊狼-三角洲联动",
        formatter_class=argparse.RawTextHelpFormatter
    )
    
    parser.add_argument(
        'tool',
        choices=['run', 'zone'],
        nargs='?',
        default='run',
        help=(
            "选择要执行的工具:\n"
            "  run   - (默认) 运行主游戏自动化控制器。\n"
            "  zone  - 运行区域选择工具来获取坐标。"
        )
    )
    
    parser.add_argument(
        '--image',
        type=str,
        default='test_image.png',
        help="当使用 'zone' 工具时，指定要分析的图片路径。"
    )

    args = parser.parse_args()

    if args.tool == 'run':
        run_controller()
    elif args.tool == 'zone':
        run_zone_selector(args.image)