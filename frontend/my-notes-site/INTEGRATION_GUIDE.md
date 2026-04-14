# 知识图谱组件集成指南

## 最简单的集成方法（3 步完成）

### 步骤 1：复制文件到 chem-front 项目

```bash
# 复制这两个文件到你的 chem-front 项目
cp web/knowledge-graph-component.html chem-front/components/
cp output/knowledge_graph.json chem-front/public/data/
```

### 步骤 2：在你的页面中引入组件

在 `chem-front` 项目的知识图谱模块页面中添加：

```html
<!DOCTYPE html>
<html>
<head>
    <title>知识图谱模块</title>
    <!-- 你的其他样式 -->
</head>
<body>
    <!-- 你的页面头部 -->
    
    <!-- 嵌入知识图谱组件 -->
    <div style="width: 100%; height: 800px;">
        <script>
            // 配置数据文件路径（如果不在默认位置）
            window.KnowledgeGraphConfig = {
                dataUrl: '/data/knowledge_graph.json'  // 根据实际路径调整
            };
        </script>
        <script src="components/knowledge-graph-component.html"></script>
    </div>
    
    <!-- 你的页面底部 -->
</body>
</html>
```

### 步骤 3：确保数据文件可访问

确保 `knowledge_graph.json` 能被正确访问：
- 如果用 React/Vue：放在 `public/` 目录
- 如果用纯 HTML：放在可访问的静态资源目录

---

## 方法 2：使用 iframe（零改动）

如果不想修改任何代码，直接用 iframe：

```html
<iframe 
    src="http://localhost:8000/web/index.html" 
    style="width: 100%; height: 800px; border: none;"
    title="知识图谱">
</iframe>
```

**优点**：完全隔离，零改动  
**缺点**：需要保持 Python 服务器运行

---

## 方法 3：React 组件方式（如果用 React）

在 `chem-front/src/components/KnowledgeGraph.jsx` 创建：

```jsx
import React, { useEffect } from 'react';

export function KnowledgeGraph() {
  useEffect(() => {
    // 动态加载组件脚本
    const script = document.createElement('script');
    script.src = '/components/knowledge-graph-component.html';
    document.body.appendChild(script);
    
    return () => {
      document.body.removeChild(script);
    };
  }, []);
  
  return (
    <div style={{ width: '100%', height: '800px' }}>
      <div id="knowledge-graph-container"></div>
    </div>
  );
}
```

使用：
```jsx
import { KnowledgeGraph } from './components/KnowledgeGraph';

function App() {
  return (
    <div>
      <h1>化学知识图谱</h1>
      <KnowledgeGraph />
    </div>
  );
}
```

---

## 文件路径配置

组件会自动尝试以下路径加载数据：
1. 你配置的 `window.KnowledgeGraphConfig.dataUrl`
2. `./output/knowledge_graph.json`
3. `../output/knowledge_graph.json`
4. `/output/knowledge_graph.json`

建议在引入前配置：
```javascript
window.KnowledgeGraphConfig = {
    dataUrl: '/your-path/knowledge_graph.json'
};
```

---

## 常见问题

### Q: 数据加载失败？
**A**: 检查浏览器控制台，确认 JSON 文件路径正确，检查是否有 CORS 问题

### Q: 样式冲突？
**A**: 所有样式都加了 `#knowledge-graph-container` 前缀，不会污染全局

### Q: 如何自定义尺寸？
**A**: 修改外层容器的 `style`：
```html
<div style="width: 100%; height: 600px;">
    <!-- 组件代码 -->
</div>
```

### Q: 能否动态更新数据？
**A**: 当前版本不支持，重新加载页面会刷新数据

---

## 推荐方案（根据项目类型）

| 项目类型 | 推荐方案 | 难度 |
|---------|---------|------|
| 纯 HTML | 方法 1 | ⭐ |
| React/Vue | 方法 3 | ⭐⭐ |
| 临时演示 | 方法 2 (iframe) | ⭐ |

**最简单**：直接用方法 1，复制 `knowledge-graph-component.html` 到项目，引入即可！
