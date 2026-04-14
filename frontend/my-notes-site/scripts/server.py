#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
简单 HTTP 服务器，用于在本地预览知识图谱
"""

import http.server
import socketserver
import os
from pathlib import Path


class MyHTTPRequestHandler(http.server.SimpleHTTPRequestHandler):
    def end_headers(self):
        self.send_header('Cache-Control', 'no-store, no-cache, must-revalidate, max-age=0')
        self.send_header('Access-Control-Allow-Origin', '*')
        super().end_headers()
    
    def translate_path(self, path):
        """重写路径处理以支持跨目录访问"""
        # 如果请求的是 output 目录中的文件，从根目录找
        if path.startswith('/output/'):
            # 查找实际的输出文件
            file_path = Path(__file__).parent.parent / path.lstrip('/')
            if file_path.exists():
                return str(file_path)
        
        return super().translate_path(path)


def main():
    # 获取 web 目录的路径
    web_dir = Path(__file__).parent.parent / 'web'
    os.chdir(web_dir)
    
    PORT = 8000
    Handler = MyHTTPRequestHandler
    
    with socketserver.TCPServer(('', PORT), Handler) as httpd:
        print(f"🌐 服务器启动在: http://localhost:{PORT}")
        print(f"📁 静态文件目录: {web_dir}")
        print(f"📊 数据文件目录: {web_dir.parent / 'output'}")
        print("\n按 Ctrl+C 停止服务器\n")
        
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\n\n✅ 服务器已停止")


if __name__ == '__main__':
    main()
