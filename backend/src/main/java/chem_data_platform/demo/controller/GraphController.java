package chem_data_platform.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import chem_data_platform.demo.entity.FileInfo;
import chem_data_platform.demo.repository.FileInfoRepository;
import chem_data_platform.demo.utils.JwtUtil;
import chem_data_platform.demo.vo.ApiResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/graphs")
public class GraphController {

    @Autowired
    private FileInfoRepository fileInfoRepository;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 获取文件的知识图谱数据
     * GET /api/v1/graphs/{fileId}/visualization
     * 前端点击"View Knowledge Graph"按钮时调用
     */
    @GetMapping("/{fileId}/visualization")
    public ResponseEntity<ApiResponse<?>> getFileKnowledgeGraph(
            @PathVariable Long fileId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // 验证认证信息
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.unauthorized("未提供有效的认证令牌"));
            }

            String token = authHeader.substring(7);
            if (!jwtUtil.isTokenValid(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.unauthorized("无效的认证令牌"));
            }

            // 从数据库查询文件信息
            Optional<FileInfo> fileOpt = fileInfoRepository.findById(fileId);
            if (!fileOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("文件不存在"));
            }

            // 生成模拟的知识图谱数据
            Map<String, Object> graphData = new HashMap<>();

            // 模拟节点数据
            List<Map<String, String>> nodes = new ArrayList<>();
            
            // 添加文件节点
            Map<String, String> fileNode = new HashMap<>();
            fileNode.put("id", fileOpt.get().getFileName());
            fileNode.put("type", "file");
            nodes.add(fileNode);

            // 添加一些模拟的实体节点
            Map<String, String> node1 = new HashMap<>();
            node1.put("id", "Document");
            node1.put("type", "document");
            nodes.add(node1);

            Map<String, String> node2 = new HashMap<>();
            node2.put("id", "Content");
            node2.put("type", "content");
            nodes.add(node2);

            Map<String, String> node3 = new HashMap<>();
            node3.put("id", "Analysis");
            node3.put("type", "analysis");
            nodes.add(node3);

            // 模拟关系数据
            List<Map<String, String>> edges = new ArrayList<>();

            Map<String, String> edge1 = new HashMap<>();
            edge1.put("source", fileOpt.get().getFileName());
            edge1.put("target", "Document");
            edge1.put("relation", "is a");
            edges.add(edge1);

            Map<String, String> edge2 = new HashMap<>();
            edge2.put("source", "Document");
            edge2.put("target", "Content");
            edge2.put("relation", "contains");
            edges.add(edge2);

            Map<String, String> edge3 = new HashMap<>();
            edge3.put("source", "Content");
            edge3.put("target", "Analysis");
            edge3.put("relation", "has");
            edges.add(edge3);

            graphData.put("nodes", nodes);
            graphData.put("edges", edges);

            return ResponseEntity.ok(ApiResponse.success("获取知识图谱数据成功", graphData));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }
}