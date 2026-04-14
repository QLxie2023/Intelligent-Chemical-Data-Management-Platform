# 🧪 通用化学 Obsidian Vault 知识图谱

> **General Chemistry Obsidian Vault Repository** - 用于展示化学元素之间关系的交互式知识图谱

一个完整的化学教育项目，包含常见元素的 Markdown 文档、它们的相互关系，以及一个交互式的 HTML 知识图谱可视化。

## 📋 项目结构

```
my-notes-site/
├── vault/                           # Obsidian Vault 数据
│   ├── elements/                    # 元素文档
│   │   ├── Hydrogen.md             # 氢
│   │   ├── Carbon.md               # 碳
│   │   ├── Nitrogen.md             # 氮
│   │   ├── Oxygen.md               # 氧
│   │   ├── Sulfur.md               # 硫
│   │   ├── Phosphorus.md           # 磷
│   │   ├── Fluorine.md             # 氟
│   │   ├── Chlorine.md             # 氯
│   │   ├── Sodium.md               # 钠
│   │   ├── Calcium.md              # 钙
│   │   ├── Iron.md                 # 铁
│   │   ├── Copper.md               # 铜
│   │   ├── Silver.md               # 银
│   │   ├── Gold.md                 # 金
│   │   ├── Helium.md               # 氦
│   │   ├── Neon.md                 # 氖
│   │   ├── Argon.md                # 氩
│   │   ├── Lithium.md              # 锂
│   │   ├── Iodine.md               # 碘
│   │   ├── Bromine.md              # 溴
│   │   ├── Water.md                # 水
│   │   ├── CarbonDioxide.md        # 二氧化碳
│   │   ├── Ammonia.md              # 氨
│   │   ├── SulfuricAcid.md         # 硫酸
│   │   ├── NitricAcid.md           # 硝酸
│   │   ├── HydrogenChloride.md     # 氯化氢
│   │   ├── SodiumChloride.md       # 氯化钠
│   │   ├── IronOxides.md           # 铁的氧化物
│   │   └── MethaneCH4.md           # 甲烷
│   └── groups/                      # 分类文档
│       ├── NonMetals.md             # 非金属元素
│       ├── Metals.md                # 金属元素
│       ├── HaloGens.md              # 卤素
│       ├── NobleGases.md            # 稀有气体
│       ├── AlkaliMetals.md          # 碱金属
│       └── TransitionMetals.md      # 过渡金属
├── scripts/                         # 处理脚本
│   ├── parse_markdown.py            # Markdown 解析器和图谱生成器
│   └── server.py                    # 本地服务器
├── output/                          # 输出数据
│   └── knowledge_graph.json         # 生成的知识图谱 JSON
├── web/                             # 网页可视化
│   └── index.html                   # 交互式知识图谱网页
└── package.json                     # 项目配置

```

## 🎯 功能特性

### 1. **完整的元素文档** 📚
- 29 个常见化学元素的详细信息
- 包含原子序数、符号、分类等
- 元素间的相互关系（反应性、化合物等）
- 每个元素的物理性质、化学性质和应用

### 2. **逻辑分类系统** 🗂️
- 非金属元素
- 金属元素
- 卤素
- 稀有气体
- 碱金属
- 过渡金属

### 3. **丰富的化合物关系** 🧬
- 水、二氧化碳、氨等常见化合物
- 酸（硫酸、硝酸）
- 盐和氧化物
- 元素与化合物的关系映射

### 4. **交互式知识图谱** 📊
- 力导向图布局自动排列
- 拖拽节点调整位置
- 搜索和高亮功能
- 实时统计信息
- 节点信息面板
- 缩放和平移控制

### 5. **可视化特性** 🎨
- 不同类型节点用不同颜色区分
  - 🔴 元素（红色）
  - 🟠 化合物（橙色）
  - 🔵 分类（青色）
- 链接关系清晰展示
- 响应式设计支持各种屏幕

## 📊 数据统计

- **总节点数**: 35
- **总关系数**: 223
- **元素数**: 29
- **分类数**: 6
- **化合物数**: 9

## 🚀 快速开始

### 前置要求
- Python 3.6+
- 现代浏览器（Chrome, Firefox, Edge, Safari）
- 文本编辑器（VS Code, Obsidian 等）

### 安装步骤

1. **克隆或下载项目**
```bash
git clone <repository-url>
cd my-notes-site
```

2. **生成知识图谱数据** (如果需要)
```bash
python scripts/parse_markdown.py vault output/knowledge_graph.json
```

3. **启动本地服务器**
```bash
python scripts/server.py
```

4. **打开浏览器访问**
```
http://localhost:8000
```

## 📖 使用指南

### 在 Obsidian 中打开 Vault
1. 打开 Obsidian
2. 选择 "打开文件夹作为库"
3. 选择 `vault` 文件夹
4. 开始浏览和编辑

### 查看知识图谱
1. 启动服务器后访问 `http://localhost:8000`
2. 在左侧侧边栏查看统计信息
3. 使用搜索框查找元素
4. 点击节点查看详细信息
5. 使用控制按钮：
   - 🔄 **重置视图** - 回到初始视图
   - ⏸ **暂停/继续** - 控制图谱动画
   - 📸 **导出图片** - 保存为 PNG

### Markdown 链接格式
文档使用 Obsidian 的双括号链接格式：

```markdown
# 氧 (Oxygen)
...
## 相关化合物
- [[Water|水 (H₂O)]]
- [[CarbonDioxide|二氧化碳 (CO₂)]]
```

## 🔧 自定义扩展

### 添加新元素
1. 在 `vault/elements/` 中创建新的 `.md` 文件
2. 按照现有元素的格式编写内容
3. 添加相关链接到其他元素/分类
4. 运行 `python scripts/parse_markdown.py` 重新生成图谱

### 添加新化合物
1. 在 `vault/elements/` 中创建化合物文件
2. 在 "相关化合物" 部分添加 `[[]]` 链接
3. 重新生成图谱数据

### 自定义样式
编辑 `web/index.html` 中的 `<style>` 部分：

```css
/* 修改元素颜色 */
const colorMap = {
    'element': '#ff6b6b',    /* 元素颜色 */
    'compound': '#ffa502',   /* 化合物颜色 */
    'group': '#4ecdc4'       /* 分类颜色 */
};
```

## 📝 Markdown 文件模板

```markdown
# 元素名称 (English Name)

**符号**: XX  
**原子序数**: N  
**相对原子质量**: M

## 基本信息
- **族**: 第X族
- **周期**: 第X周期
- **电子构型**: ...
- **常见价态**: ...

## 分类
- [[GroupName|分类名称]]

## 物理性质
- 描述...

## 化学性质
- 与[[Element|元素]]反应
- ...

## 用途
- ...

## 相关化合物
- [[Compound|化合物名]]
```

## 🌐 部署到网络

### GitHub Pages 部署
1. 推送项目到 GitHub
2. 在 Settings 中启用 GitHub Pages
3. 选择 `web` 目录作为源

### 其他服务器部署
1. 上传 `web` 文件夹到服务器
2. 配置 Web 服务器指向 `index.html`
3. 确保 `output/knowledge_graph.json` 可访问

## 📚 技术栈

- **文档**: Markdown + Obsidian Wiki Links
- **数据处理**: Python 3
- **可视化**: D3.js v7
- **前端**: HTML5 + CSS3 + JavaScript
- **服务**: Python SimpleHTTPServer

## 🎓 教育价值

这个项目适合：
- 化学教学和学习
- 科学知识的关系建模
- 复杂系统的可视化
- 知识管理系统的示例

## 📄 许可证

MIT License - 自由使用和修改

## 🤝 贡献

欢迎提交问题和改进建议！

### 贡献方式
1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 💡 改进建议

可能的未来改进：
- [ ] 添加更多元素（稀有元素、合成元素）
- [ ] 支持更复杂的化合物
- [ ] 3D 知识图谱可视化
- [ ] 实时元素搜索功能
- [ ] 反应方程式的动画展示
- [ ] 温度/压力对反应的影响可视化
- [ ] 多语言支持
- [ ] 移动端优化

## 📞 联系方式

如有问题或建议，请提交 Issue 或 PR。

## 🙏 致谢

感谢所有化学教育工作者的启发！

---

**最后更新**: 2025-12-03

**项目主页**: [我的笔记站点](http://localhost:8000)
