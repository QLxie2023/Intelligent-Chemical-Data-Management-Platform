#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
项目中文到英文翻译工具
自动将项目中的中文改为英文
"""

import os
import re
from pathlib import Path

# 翻译字典
TRANSLATION_DICT = {
    # 元素属性
    '符号': 'Symbol',
    '原子序数': 'Atomic Number',
    '相对原子质量': 'Relative Atomic Mass',
    
    # 基本信息
    '族': 'Group',
    '周期': 'Period',
    '电子构型': 'Electron Configuration',
    '常见价态': 'Common Oxidation States',
    
    # 部分标题
    '基本信息': 'Basic Information',
    '分类': 'Classification',
    '物理性质': 'Physical Properties',
    '化学性质': 'Chemical Properties',
    '用途': '用途',
    '应用': 'Applications',
    '相关化合物': 'Related Compounds',
    '重要性': 'Significance',
    '生成反应': 'Formation Reactions',
    '相关分类': 'Related Classifications',
    
    # 元素分类
    '非金属元素': 'Non-Metals',
    '金属元素': 'Metals',
    '卤素': 'Halogens',
    '稀有气体': 'Noble Gases',
    '碱金属': 'Alkali Metals',
    '过渡金属': 'Transition Metals',
    
    # 描述词汇
    '无色无味': 'Colorless and odorless',
    '无色': 'Colorless',
    '有色': 'Colored',
    '固体': 'Solid',
    '液体': 'Liquid',
    '气体': 'Gas',
    '密度': 'Density',
    '沸点': 'Boiling Point',
    '熔点': 'Melting Point',
    '反应性': 'Reactivity',
    '稳定': 'Stable',
    '易溶于水': 'Soluble in water',
    '不溶于水': 'Insoluble in water',
    '微溶于水': 'Slightly soluble in water',
    
    # 特性描述
    '极为稳定': 'Extremely stable',
    '强氧化剂': 'Strong oxidizing agent',
    '弱碱性': 'Weakly basic',
    '强酸': 'Strong acid',
    '强酸性': 'Strongly acidic',
    '还原性': 'Reducing property',
    '氧化性': 'Oxidizing property',
    '催化活性': 'Catalytic activity',
    '导电性': 'Conductivity',
    '导热性': 'Thermal conductivity',
    '延展性': 'Ductility',
    '可锻性': 'Malleability',
    
    # 应用领域
    '工业应用': 'Industrial Applications',
    '医疗用途': 'Medical Uses',
    '食品添加剂': 'Food Additive',
    '药物合成': 'Pharmaceutical Synthesis',
    '建筑材料': 'Building Materials',
    '珠宝和装饰品': 'Jewelry and Decoration',
    '电子和导体': 'Electronics and Conductors',
    '有机合成': 'Organic Synthesis',
    '冶金工业': 'Metallurgical Industry',
    
    # 生化相关
    '生命必需元素': 'Essential Element for Life',
    '人体必需元素': 'Essential Element for Human Body',
    '生物体内': 'In organisms',
    '蛋白质': 'Protein',
    '能量代谢': 'Energy Metabolism',
    '血液': 'Blood',
    '骨骼和牙齿': 'Bones and Teeth',
    '甲状腺': 'Thyroid',
    '大脑': 'Brain',
    
    # 大气和环境
    '大气': 'Atmosphere',
    '大气主要成分': 'Major Atmospheric Component',
    '大气重要成分': 'Important Atmospheric Component',
    '大气中含量': 'Atmospheric Content',
    '温室气体': 'Greenhouse Gas',
    '环境污染物': 'Environmental Pollutant',
    '全球气候': 'Global Climate',
    
    # 状态描述
    '第一天': 'Day One',
    '第二天': 'Day Two',
    '第三天': 'Day Three',
    '第四天': 'Day Four',
    
    # 其他常见词汇
    '元素': 'Element',
    '化合物': 'Compound',
    '分子': 'Molecule',
    '原子': 'Atom',
    '离子': 'Ion',
    '反应': 'Reaction',
    '燃烧': 'Combustion',
    '氧化': 'Oxidation',
    '还原': 'Reduction',
    '合成': 'Synthesis',
    '分解': 'Decomposition',
    '置换': 'Substitution',
    '复分解': 'Double displacement',
    
    # 成员 Members
    '成员': 'Members',
    '分类属于': 'Classification Belongs to',
    
    # 其他
    '历史': 'History',
    '未来': 'Future',
    '应用前景': 'Application Prospects',
    '化学工业基础': 'Foundation of Chemical Industry',
    '工业最重要': 'Most Important Industrial',
}

# 特殊的多行翻译
SECTION_TRANSLATIONS = {
    '## 基本信息': '## Basic Information',
    '## 分类': '## Classification',
    '## 物理性质': '## Physical Properties',
    '## 化学性质': '## Chemical Properties',
    '## 用途': '## Applications',
    '## 应用': '## Applications',
    '## 相关化合物': '## Related Compounds',
    '## 重要性': '## Significance',
    '## 生成反应': '## Formation Reactions',
    '## 成员': '## Members',
    '## 特性': '## Characteristics',
    '## 相关分类': '## Related Classifications',
    '## 分类属于': '## Classification Belongs to',
}

def translate_text(text):
    """翻译文本中的中文"""
    result = text
    
    # 首先处理特殊的多行翻译
    for chinese, english in SECTION_TRANSLATIONS.items():
        result = result.replace(chinese, english)
    
    # 然后处理单词翻译
    for chinese, english in TRANSLATION_DICT.items():
        # 使用 word boundary 来避免部分替换
        pattern = r'\b' + re.escape(chinese) + r'\b'
        result = re.sub(pattern, english, result)
    
    return result

def translate_file(file_path):
    """翻译单个文件"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # 检查是否有中文
        if not re.search(r'[\u4e00-\u9fff]', content):
            return False
        
        # 翻译内容
        translated = translate_text(content)
        
        # 写回文件
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(translated)
        
        return True
    except Exception as e:
        print(f"Error processing {file_path}: {e}")
        return False

def main():
    project_root = Path(__file__).parent.parent
    
    print("=" * 60)
    print(" Chinese to English Translation Tool")
    print("=" * 60)
    
    # 要翻译的文件类型
    file_patterns = ['**/*.md', 'web/index.html', 'scripts/*.py', '*.md']
    
    translated_files = []
    total_files = 0
    
    for pattern in file_patterns:
        for file_path in project_root.glob(pattern):
            if file_path.is_file():
                total_files += 1
                if translate_file(file_path):
                    translated_files.append(str(file_path.relative_to(project_root)))
    
    print(f"\n Translation completed!")
    print(f" Statistics:")
    print(f"   Total files processed: {total_files}")
    print(f"   Files translated: {len(translated_files)}")
    
    if translated_files:
        print(f"\n Translated files:")
        for file in sorted(translated_files):
            print(f"   - {file}")

if __name__ == '__main__':
    main()
