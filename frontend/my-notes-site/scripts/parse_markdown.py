#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Markdown 文件解析器
从 Obsidian Vault 中提取元素和它们的关系
"""

import os
import re
import json
from pathlib import Path
from typing import Dict, List, Set, Tuple


class MarkdownParser:
    """解析 Markdown 文件并提取链接关系"""
    
    def __init__(self, vault_dir: str):
        """
        初始化解析器
        
        Args:
            vault_dir: Vault 目录路径
        """
        self.vault_dir = Path(vault_dir)
        self.files = {}
        self.nodes = {}
        self.links = []
        self.relationships = {}
        
    def load_all_files(self):
        """加载所有 MD 文件"""
        for md_file in self.vault_dir.glob('**/*.md'):
            with open(md_file, 'r', encoding='utf-8') as f:
                content = f.read()
                relative_path = md_file.relative_to(self.vault_dir)
                self.files[str(relative_path)] = content
                
    def extract_title(self, content: str) -> str:
        """从 Markdown 内容中提取标题"""
        match = re.search(r'^# (.+)$', content, re.MULTILINE)
        return match.group(1).strip() if match else "Unknown"
    
    def extract_links(self, content: str) -> List[str]:
        """从 Markdown 内容中提取所有 [[]] 格式的链接"""
        pattern = r'\[\[([^\]|]+)(?:\|([^\]]+))?\]\]'
        matches = re.findall(pattern, content)
        return [match[0] for match in matches]
    
    def extract_element_info(self, content: str) -> Dict:
        """从 Markdown 内容中提取元素信息"""
        info = {}
        
        # 提取符号和原子序数
        symbol_match = re.search(r'\*\*符号\*\*:\s*(\w+)', content)
        atomic_match = re.search(r'\*\*原子序数\*\*:\s*(\d+)', content)
        
        if symbol_match:
            info['symbol'] = symbol_match.group(1)
        if atomic_match:
            info['atomic_number'] = int(atomic_match.group(1))
            
        # 提取分类
        classification = []
        classifications_match = re.search(r'## 分类\s+\n(.*?)(?=##|\Z)', content, re.DOTALL)
        if classifications_match:
            links = self.extract_links(classifications_match.group(1))
            classification.extend(links)
        
        info['classification'] = classification
        return info
    
    def build_graph(self):
        """构建知识图谱"""
        self.load_all_files()
        
        # 创建节点
        for file_path, content in self.files.items():
            title = self.extract_title(content)
            node_id = Path(file_path).stem
            
            info = self.extract_element_info(content)
            
            self.nodes[node_id] = {
                'id': node_id,
                'title': title,
                'file': file_path,
                'info': info,
                'type': self._determine_type(file_path, content)
            }
        
        # 创建链接
        for file_path, content in self.files.items():
            source_id = Path(file_path).stem
            links = self.extract_links(content)
            
            for link in links:
                # 尝试匹配到实际的文件
                target_id = self._find_node_id(link)
                if target_id and target_id != source_id:
                    self.links.append({
                        'source': source_id,
                        'target': target_id,
                        'type': 'reference'
                    })
    
    def _determine_type(self, file_path: str, content: str) -> str:
        """确定节点类型"""
        if 'elements' in file_path:
            return 'element'
        elif 'groups' in file_path:
            return 'group'
        else:
            return 'compound'
    
    def _find_node_id(self, link_text: str) -> str:
        """根据链接文本查找节点ID"""
        # 直接匹配
        for node_id in self.nodes.keys():
            if node_id.lower() == link_text.lower():
                return node_id
        
        # 根据标题匹配
        for node_id, node_info in self.nodes.items():
            if node_info['title'].lower() == link_text.lower():
                return node_id
        
        return None
    
    def export_to_json(self, output_path: str):
        """导出为 JSON 格式"""
        data = {
            'nodes': list(self.nodes.values()),
            'links': self.links,
            'metadata': {
                'total_nodes': len(self.nodes),
                'total_links': len(self.links),
                'types': {}
            }
        }
        
        # 统计类型
        for node in self.nodes.values():
            node_type = node['type']
            if node_type not in data['metadata']['types']:
                data['metadata']['types'][node_type] = 0
            data['metadata']['types'][node_type] += 1
        
        with open(output_path, 'w', encoding='utf-8') as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
    
    def get_summary(self) -> Dict:
        """获取图谱摘要"""
        elements = [n for n in self.nodes.values() if n['type'] == 'element']
        groups = [n for n in self.nodes.values() if n['type'] == 'group']
        compounds = [n for n in self.nodes.values() if n['type'] == 'compound']
        
        return {
            'total_nodes': len(self.nodes),
            'total_links': len(self.links),
            'elements': len(elements),
            'groups': len(groups),
            'compounds': len(compounds),
        }


def main():
    """主函数"""
    import sys
    
    # 获取 vault 目录
    if len(sys.argv) > 1:
        vault_dir = sys.argv[1]
    else:
        vault_dir = 'vault'
    
    if len(sys.argv) > 2:
        output_file = sys.argv[2]
    else:
        output_file = 'output/knowledge_graph.json'
    
    print(f"📚 Parsing Vault: {vault_dir}")
    
    parser = MarkdownParser(vault_dir)
    parser.build_graph()
    
    summary = parser.get_summary()
    print(f"\n📊 Knowledge Graph Summary:")
    print(f"   Total Nodes: {summary['total_nodes']}")
    print(f"   Total Links: {summary['total_links']}")
    print(f"   Elements: {summary['elements']}")
    print(f"   Groups: {summary['groups']}")
    print(f"   Compounds: {summary['compounds']}")
    
    # 确保输出目录存在
    os.makedirs(os.path.dirname(output_file) or '.', exist_ok=True)
    
    parser.export_to_json(output_file)
    print(f"\n✅ Graph exported to: {output_file}")


if __name__ == '__main__':
    main()
