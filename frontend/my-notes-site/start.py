#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
启动脚本 - 用于初始化和启动项目
这个脚本会：
1. 生成知识图谱数据（如果需要）
2. 复制数据文件到 web 目录
3. 启动本地服务器
"""

import os
import sys
import json
import shutil
import subprocess
from pathlib import Path

def main():
    # 确保我们在项目根目录
    script_dir = Path(__file__).parent
    os.chdir(script_dir)
    project_root = Path.cwd()
    
    print("=" * 60)
    print("启动化学知识图谱服务")
    print("=" * 60)
    print(f"项目目录: {project_root}\n")
    
    # 第一步：确保知识图谱数据存在
    print("检查知识图谱数据...")
    output_dir = project_root / 'output'
    graph_file = output_dir / 'knowledge_graph.json'
    
    if not graph_file.exists():
        print("生成知识图谱数据...")
        parse_script = project_root / "scripts" / "parse_markdown.py"
        vault_dir = project_root / "vault"
        print(f"  运行: {parse_script}")
        subprocess.run([sys.executable, str(parse_script), str(vault_dir), str(graph_file)])
    
    # 验证数据文件
    if not graph_file.exists():
        print("无法生成知识图谱数据，请检查 Markdown 文件")
        sys.exit(1)
    
    # 读取并验证数据
    try:
        with open(graph_file, 'r', encoding='utf-8') as f:
            data = json.load(f)
        print(f"数据文件有效 ({len(data.get('nodes', []))} 节点)")
    except Exception as e:
        print(f"数据文件错误: {e}")
        sys.exit(1)
    
    # 第二步：复制数据文件到 web 目录
    print("\n配置 web 目录...")
    web_output_dir = project_root / 'web' / 'output'
    web_output_dir.mkdir(exist_ok=True)
    
    web_graph_file = web_output_dir / 'knowledge_graph.json'
    shutil.copy2(graph_file, web_graph_file)
    print(f"数据文件已复制到: {web_graph_file}")
    
    # 第三步：启动服务器
    print("\n" + "=" * 60)
    print("启动 HTTP 服务器...")
    print("=" * 60)
    
    web_dir = project_root / 'web'
    os.chdir(web_dir)
    
    print(f"\n服务器地址: http://localhost:8000")
    print(f"数据文件: {web_graph_file}")
    print("\n在浏览器中打开上面的地址查看知识图谱")
    print("按 Ctrl+C 停止服务器\n")
    
    # 启动服务器
    import http.server
    import socketserver
    
    class Handler(http.server.SimpleHTTPRequestHandler):
        def end_headers(self):
            self.send_header('Cache-Control', 'no-store, no-cache, must-revalidate, max-age=0')
            self.send_header('Access-Control-Allow-Origin', '*')
            super().end_headers()
    
    try:
        with socketserver.TCPServer(('', 8000), Handler) as httpd:
            httpd.serve_forever()
    except KeyboardInterrupt:
        print("\n\n✅ 服务器已停止")
    except OSError as e:
        if 'Address already in use' in str(e):
            print("❌ 端口 8000 已被占用")
            print("   请关闭其他使用该端口的程序，或修改端口号")
        else:
            print(f"❌ 错误: {e}")
        sys.exit(1)


if __name__ == '__main__':
    main()
