@ECHO OFF
chcp 65001 >nul
title 安装OCR服务器
cd ".\ocrServer"
pip install -r requirements.txt
echo 安装完成，按下任意键关闭此窗口
pause