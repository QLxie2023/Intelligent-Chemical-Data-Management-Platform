#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
项目验证和统计脚本
检查所有文件的完整性和数据的正确性
"""

import os
import json
from pathlib import Path


def verify_project():
    """验证项目完整性"""
    print("=" * 60)
    print("🔍 项目验证工具")
    print("=" * 60)
    
    project_root = Path(__file__).parent
    
    # 检查关键目录
    print("\n📁 检查目录...")
    required_dirs = ['vault/elements', 'vault/groups', 'scripts', 'web', 'output']
    for dir_name in required_dirs:
        dir_path = project_root / dir_name
        if dir_path.exists():
            print(f"  ✅ {dir_name}/")
        else:
            print(f"  ❌ {dir_name}/ (缺失)")
    
    # 检查关键文件
    print("\n📄 检查文件...")
    required_files = [
        'scripts/parse_markdown.py',
        'scripts/server.py',
        'web/index.html',
        'output/knowledge_graph.json',
        'package.json',
        'README.md',
        'QUICKSTART.md'
    ]
    for file_name in required_files:
        file_path = project_root / file_name
        if file_path.exists():
            size = file_path.stat().st_size
            print(f"  ✅ {file_name} ({size:,} bytes)")
        else:
            print(f"  ❌ {file_name} (缺失)")
    
    # 统计 Markdown 文件
    print("\n📚 统计 Markdown 文件...")
    md_files = list((project_root / 'vault').glob('**/*.md'))
    print(f"  总数: {len(md_files)}")
    
    elements = list((project_root / 'vault/elements').glob('*.md'))
    groups = list((project_root / 'vault/groups').glob('*.md'))
    print(f"  元素: {len(elements)}")
    print(f"  分类: {len(groups)}")
    
    # 分析知识图谱数据
    print("\n📊 分析知识图谱...")
    graph_file = project_root / 'output/knowledge_graph.json'
    if graph_file.exists():
        try:
            with open(graph_file, 'r', encoding='utf-8') as f:
                graph = json.load(f)
            
            nodes = graph.get('nodes', [])
            links = graph.get('links', [])
            types = graph.get('metadata', {}).get('types', {})
            
            print(f"  节点总数: {len(nodes)}")
            print(f"  关系总数: {len(links)}")
            for node_type, count in types.items():
                print(f"    - {node_type}: {count}")
        except Exception as e:
            print(f"  ❌ 读取错误: {e}")
    else:
        print(f"  ⚠️  知识图谱文件未找到")
    
    # 检查 Python 环境
    print("\n🐍 检查 Python 环境...")
    import sys
    print(f"  Python 版本: {sys.version.split()[0]}")
    print(f"  Python 路径: {sys.executable}")
    
    # 总结
    print("\n" + "=" * 60)
    print("✅ 验证完成！")
    print("=" * 60)
    print("\n🚀 快速开始:")
    print("  python scripts/server.py")
    print("\n📖 查看文档:")
    print("  - README.md (完整文档)")
    print("  - QUICKSTART.md (快速开始)")
    print("  - DEPLOY.md (部署指南)")
    print("\n")


def show_stats():
    """显示项目统计"""
    print("=" * 60)
    print("📈 项目统计")
    print("=" * 60)
    
    project_root = Path(__file__).parent
    
    # 文件统计
    all_files = list(project_root.glob('**/*'))
    files_only = [f for f in all_files if f.is_file()]
    
    print(f"\n📁 文件统计:")
    print(f"  总文件数: {len(files_only)}")
    
    # 按类型统计
    type_count = {}
    for f in files_only:
        ext = f.suffix or 'no_extension'
        type_count[ext] = type_count.get(ext, 0) + 1
    
    for ext, count in sorted(type_count.items(), key=lambda x: x[1], reverse=True):
        print(f"    {ext}: {count}")
    
    # 代码行数
    print(f"\n💻 代码统计:")
    
    def count_lines(file_path):
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                return len(f.readlines())
        except:
            return 0
    
    py_lines = sum(count_lines(f) for f in project_root.glob('**/*.py'))
    js_files = list(project_root.glob('**/*.js')) + [project_root / 'web/index.html']
    js_lines = sum(count_lines(f) for f in js_files if f.exists())
    md_lines = sum(count_lines(f) for f in project_root.glob('**/*.md'))
    
    print(f"  Python 代码: {py_lines} 行")
    print(f"  JavaScript: {js_lines} 行")
    print(f"  Markdown: {md_lines} 行")
    print(f"  总代码: {py_lines + js_lines + md_lines} 行")
    
    # 数据大小
    print(f"\n💾 数据统计:")
    
    vault_size = sum(f.stat().st_size for f in project_root.glob('vault/**/*') if f.is_file())
    print(f"  Vault 数据: {vault_size:,} bytes ({vault_size/1024:.1f} KB)")
    
    json_size = (project_root / 'output/knowledge_graph.json').stat().st_size
    print(f"  JSON 图谱: {json_size:,} bytes ({json_size/1024:.1f} KB)")
    
    print("\n" + "=" * 60)


if __name__ == '__main__':
    import sys
    
    if len(sys.argv) > 1 and sys.argv[1] == '--stats':
        show_stats()
    else:
        verify_project()
        show_stats()
