@ECHO OFF
chcp 65001 >nul
title 启动服务
cd ".\ocrServer"
python main.py
cd..
java -jar DG-Lab-Delta-Force-1.0.0.jar
pause