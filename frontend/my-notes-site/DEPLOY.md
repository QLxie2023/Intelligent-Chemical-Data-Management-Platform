# 📖 使用和部署指南

## 🎯 项目概述

这是一个完整的**化学元素知识图谱系统**，包含：
- ✅ 29 个常见元素的详细 Markdown 文档
- ✅ 6 个元素分类体系
- ✅ 9 个重要化合物
- ✅ 223 个相互关系链接
- ✅ 交互式 D3.js 知识图谱可视化

## 📦 文件结构说明

```
├── vault/                          # Obsidian 库（可在 Obsidian 中直接打开）
│   ├── elements/                   # 元素文档 (20 个文件)
│   └── groups/                     # 分类文档 (6 个文件)
├── web/                            # 网页可视化 (HTML+CSS+JS)
│   └── index.html                  # 交互式知识图谱界面
├── scripts/                        # Python 脚本
│   ├── parse_markdown.py           # Markdown 解析和图谱生成
│   └── server.py                   # 本地 HTTP 服务器
├── output/                         # 生成的数据
│   └── knowledge_graph.json        # 知识图谱数据（D3.js 使用）
└── 文档文件
    ├── README.md                   # 完整文档
    ├── QUICKSTART.md               # 快速开始
    ├── INVENTORY.md                # 项目清单
    └── DEPLOY.md                   # 本文件
```

## 🚀 快速启动 (3步)

### 1️⃣ 使用 Python 服务器（推荐）

```bash
# 进入项目目录
cd my-notes-site

# 启动服务器
python scripts/server.py

# 在浏览器打开
http://localhost:8000
```

### 2️⃣ 使用 npm 命令

```bash
# 启动
npm start

# 或仅生成数据
npm run generate

# 或完整构建
npm run build
```

### 3️⃣ 直接打开文件

```bash
# Windows: 双击 web/index.html
# Mac: 右键 web/index.html -> 用默认浏览器打开
# Linux: 点击 web/index.html 或使用 firefox web/index.html
```

## 🌐 部署选项

### 方案 A: GitHub Pages（免费）

1. **推送到 GitHub**
```bash
git init
git add .
git commit -m "Initial commit"
git remote add origin https://github.com/yourusername/chemistry-vault
git push -u origin main
```

2. **启用 GitHub Pages**
   - 进入 Settings → Pages
   - 选择 "Deploy from a branch"
   - 选择 main 分支和 /root 目录

3. **访问**
```
https://yourusername.github.io/chemistry-vault/
```

### 方案 B: Vercel（推荐）

1. **连接 GitHub 仓库到 Vercel**
   - 访问 vercel.com
   - 点击 "Import Project"
   - 选择 GitHub 仓库

2. **配置设置**
   - Build Command: `npm run generate` (可选)
   - Output Directory: `web`

3. **自动部署**
   - 每次推送到 main 分支时自动更新

### 方案 C: Netlify

1. **拖放部署**
   - 访问 netlify.com
   - 将 `web` 文件夹拖到部署区域
   - 自动获得 URL

2. **或连接 Git**
   - 类似 Vercel 的流程
   - 设置 Base directory: `web`

### 方案 D: 自有服务器

**Nginx 配置**
```nginx
server {
    listen 80;
    server_name yourdomain.com;
    
    location / {
        root /var/www/chemistry-vault/web;
        try_files $uri /index.html;
    }
}
```

**Apache 配置**
```apache
<VirtualHost *:80>
    ServerName yourdomain.com
    DocumentRoot /var/www/chemistry-vault/web
    
    <Directory /var/www/chemistry-vault/web>
        RewriteEngine On
        RewriteBase /
        RewriteCond %{REQUEST_FILENAME} !-f
        RewriteCond %{REQUEST_FILENAME} !-d
        RewriteRule . /index.html [L]
    </Directory>
</VirtualHost>
```

## 📱 多平台支持

### 桌面平台
- ✅ Windows (Python 3.6+)
- ✅ macOS (Python 3.6+)
- ✅ Linux (Python 3.6+)

### 浏览器支持
- ✅ Chrome/Edge 88+
- ✅ Firefox 85+
- ✅ Safari 14+
- ✅ Mobile browsers (iOS Safari, Chrome Mobile)

### 其他应用
- ✅ Obsidian（打开 vault 文件夹）
- ✅ VS Code（编辑 Markdown）
- ✅ 任何文本编辑器（编辑 MD 文件）

## 🔧 常见操作

### 生成/重新生成知识图谱

修改任何 Markdown 文件后，运行：

```bash
# Python 直接运行
python scripts/parse_markdown.py vault output/knowledge_graph.json

# 或使用 npm
npm run generate

# 或使用 build 命令
npm run build
```

### 添加新元素

1. 在 `vault/elements/` 创建新文件，如 `Bromine.md`
2. 参考现有元素格式编写
3. 在相关分类文件中添加链接
4. 运行 `npm run generate` 生成图谱
5. 刷新浏览器查看

### 编辑元素信息

直接编辑 `vault/elements/` 中的 Markdown 文件：

```markdown
# 元素名 (English Name)

**符号**: XX
**原子序数**: N

## 相关化合物
- [[CompoundName|化合物]]
```

### 自定义样式

编辑 `web/index.html` 中的 CSS 部分：

```css
/* 修改节点颜色 */
const colorMap = {
    'element': '#ff6b6b',    /* 红色元素 */
    'compound': '#ffa502',   /* 橙色化合物 */
    'group': '#4ecdc4'       /* 青色分类 */
};

/* 修改背景梯度 */
background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
```

## 🔍 高级配置

### 修改服务器端口

编辑 `scripts/server.py`：

```python
PORT = 8080  # 改为你需要的端口
```

### 调整物理模拟参数

编辑 `web/index.html` 中的力导向参数：

```javascript
.force('charge', d3.forceManyBody().strength(-300))  // 斥力强度
.force('link', d3.forceLink(graph.links)
    .distance(100))  // 链接长度
```

### 启用 CORS 跨域

如果在不同域名访问 JSON 数据，修改 `scripts/server.py`：

```python
def end_headers(self):
    self.send_header('Access-Control-Allow-Origin', '*')
    self.send_header('Cache-Control', 'no-store')
    super().end_headers()
```

## 🐛 故障排除

### 问题：Python 命令找不到

**解决方案：**
- Windows: 确保 Python 已添加到 PATH 环境变量
- Mac/Linux: 使用 `python3` 替代 `python`
- 或使用完整路径: `/usr/bin/python3 scripts/parse_markdown.py`

### 问题：端口被占用

```bash
# 查找占用端口的进程
# Windows
netstat -ano | findstr :8000

# Mac/Linux
lsof -i :8000

# 杀死进程并重启
```

### 问题：知识图谱不显示

检查清单：
- [ ] `output/knowledge_graph.json` 文件存在
- [ ] 浏览器控制台没有错误（F12）
- [ ] 网络连接正常
- [ ] 刷新页面并清除缓存（Ctrl+Shift+Delete）

### 问题：Markdown 链接无效

确保使用正确的格式：
```markdown
# 错误 ❌
[链接文本](Hydrogen.md)

# 正确 ✅
[[Hydrogen|链接文本]]
```

## 📊 性能优化

### 对于大型知识库（500+ 节点）

1. **分页加载**
   ```javascript
   // 修改 createGraph() 中的节点显示数量
   const visibleNodes = graph.nodes.slice(0, 100);
   ```

2. **优化物理模拟**
   ```javascript
   .force('charge', d3.forceManyBody().strength(-100))
   ```

3. **使用 WebGL 加速**
   - 考虑使用 Babylon.js 或 Three.js 替代 D3.js

### 对于移动设备

编辑 `web/index.html`：
```css
@media (max-width: 768px) {
    .sidebar { display: none; }
    .container { flex-direction: column; }
}
```

## 🔐 安全建议

### 部署前检查

- [ ] 移除敏感信息
- [ ] 设置正确的 CORS 策略
- [ ] 启用 HTTPS
- [ ] 添加访问控制

### 生产环境配置

```python
# scripts/server.py (生产版本)
import ssl

context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
context.load_cert_chain('path/to/cert.pem', 'path/to/key.pem')

httpd = socketserver.TCPServer(('', 443), Handler, ssl_context=context)
```

## 📈 监控和分析

### 添加访问统计

在 `web/index.html` 中添加 Google Analytics：

```html
<script async src="https://www.googletagmanager.com/gtag/js?id=GA_ID"></script>
<script>
  window.dataLayer = window.dataLayer || [];
  function gtag(){dataLayer.push(arguments);}
  gtag('js', new Date());
  gtag('config', 'GA_ID');
</script>
```

### 日志记录

编辑 `scripts/server.py`：

```python
import logging
logging.basicConfig(filename='server.log', level=logging.INFO)
logging.info(f'访问: {self.client_address}')
```

## 🎓 教学部署

### 学校局域网部署

1. **在教室计算机上启动服务器**
```bash
python scripts/server.py
# 注意本机 IP 地址
```

2. **学生访问**
```
http://<teacher-ip>:8000
```

3. **离线版本**
- 下载所有文件到 USB 驱动器
- 学生离线访问 HTML 文件

### 教室互动

- 师生共享屏幕讨论
- 学生实时探索知识图谱
- 收集学生反馈改进内容

## 📞 支持和反馈

### 报告问题
1. 检查 [常见问题](#️-故障排除)
2. 查看完整文档：README.md
3. 创建 GitHub Issue
4. 发送邮件到 support@example.com

### 贡献代码
```bash
git checkout -b feature/new-feature
git commit -m "Add new feature"
git push origin feature/new-feature
# 创建 Pull Request
```

## 📚 进一步阅读

- [README.md](README.md) - 完整文档
- [QUICKSTART.md](QUICKSTART.md) - 快速开始
- [INVENTORY.md](INVENTORY.md) - 项目清单
- [D3.js 文档](https://d3js.org/)
- [Obsidian 文档](https://help.obsidian.md/)

---

**祝你使用愉快！** 🎉

需要帮助？查看 README.md 或提交 Issue。
