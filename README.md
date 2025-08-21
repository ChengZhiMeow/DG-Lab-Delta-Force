# DG-Lab-Delta-Force

## ⚙️ 环境要求

- Python 3.11+
- [DG-Lab-Coyote-Game-Hub服务器](https://github.com/hyperzlib/DG-Lab-Coyote-Game-Hub)

## 🚀 安装与设置

请按照以下步骤在您的计算机上设置并运行此工具。

1.  **下载项目**
    克隆并进入本项目的目录
    ```bash
    git clone https://github.com/ChengZhiMeow/DG-Lab-Delta-Force
    cd DG-Lab-Delta-Force
    ```

2.  **创建并激活Python虚拟环境**
    为了隔离项目依赖，避免与全局Python环境冲突，强烈建议使用虚拟环境。

    ```bash
    # 创建一个名为 venv 的虚拟环境
    python -m venv venv
    
    # 激活虚拟环境 (Windows)
    .\venv\Scripts\activate
    
    # 激活虚拟环境 (macOS / Linux)
    # source venv/bin/activate
    ```
    成功激活后，您会在命令行提示符前看到 `(venv)` 字样。

3.  **安装项目依赖**
    使用 `pip` 安装 `requirements.txt` 文件中列出的所有必需库。
    ```bash
    pip install -r requirements.txt
    ```

4. **必要设置**
    修改 `config.py` 下的 `SERVER_URL`与`CLIENT_ID` 为你 DG-Lab-Coyote-Game-Hub服务器的 地址与客户端ID

5. **运行程序**
    ```bash
    python main.py
    ```
