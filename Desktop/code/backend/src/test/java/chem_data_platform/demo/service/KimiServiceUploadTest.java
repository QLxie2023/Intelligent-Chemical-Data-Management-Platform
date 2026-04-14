package chem_data_platform.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kimi 第一步（Upload）单元测试
 * 演示如何在 Java 代码中调用文件上传 API
 */
@SpringBootTest
@DisplayName("Kimi Upload（第一步）测试")
class KimiServiceUploadTest {

    @Autowired(required = false)
    private KimiService kimiService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        
        // 如果 KimiService 为 null，说明未初始化（可能是配置问题）
        if (kimiService == null) {
            System.out.println("⚠ KimiService 未初始化，跳过测试");
        }
    }

    /**
     * 测试 1: 验证 API Key 配置
     * 预期：如果配置了有效的 API Key，isConfigured() 返回 true
     */
    @Test
    @DisplayName("测试 API Key 配置状态")
    void testApiKeyConfiguration() {
        if (kimiService == null) {
            System.out.println("⚠ 跳过：KimiService 未初始化");
            return;
        }

        boolean configured = kimiService.isConfigured();
        System.out.println("API Key 配置状态: " + (configured ? "已配置" : "未配置"));
        
        if (!configured) {
            System.out.println("提示：请在 application.properties 中设置 kimi.api-key=sk-YOUR_API_KEY");
        }
    }

    /**
     * 测试 2: 验证模型配置
     * 预期：能够读取配置的模型名称
     */
    @Test
    @DisplayName("测试模型配置")
    void testModelConfiguration() {
        if (kimiService == null) {
            System.out.println("⚠ 跳过：KimiService 未初始化");
            return;
        }

        String model = kimiService.getModel();
        System.out.println("配置的模型: " + model);
        
        assertNotNull(model, "模型名称不应为 null");
        assertTrue(model.contains("moonshot"), "应使用 moonshot 系列模型");
    }

    /**
     * 测试 3: 模拟文件上传请求
     * 演示如何创建 MockMultipartFile
     */
    @Test
    @DisplayName("测试文件创建（模拟）")
    void testCreateMockFile() {
        // 创建一个模拟的 PDF 文件
        String filename = "test_document.pdf";
        byte[] content = "这是一份测试 PDF 文件的内容".getBytes();
        String contentType = "application/pdf";

        MultipartFile mockFile = new MockMultipartFile(
            "file",              // 参数名称
            filename,            // 原始文件名
            contentType,         // 文件类型
            content              // 文件内容
        );

        System.out.println("创建的模拟文件:");
        System.out.println("  - 参数名: file");
        System.out.println("  - 文件名: " + mockFile.getOriginalFilename());
        System.out.println("  - 类型: " + mockFile.getContentType());
        System.out.println("  - 大小: " + mockFile.getSize() + " 字节");

        assertNotNull(mockFile);
        assertEquals(filename, mockFile.getOriginalFilename());
        assertEquals(contentType, mockFile.getContentType());
        assertEquals(content.length, mockFile.getSize());
    }

    /**
     * 测试 4: 集成测试 - 实际上传文件
     * 注意：此测试需要有效的 API Key 和网络连接
     * 如果 API Key 未配置或网络不可用，测试会被跳过
     */
    @Test
    @DisplayName("集成测试：实际上传文件到 Kimi")
    void testUploadFileTokimi() throws Exception {
        if (kimiService == null) {
            System.out.println("⚠ 跳过：KimiService 未初始化");
            return;
        }

        if (!kimiService.isConfigured()) {
            System.out.println("⚠ 跳过：API Key 未配置");
            System.out.println("提示：请设置环境变量或在 application.properties 中配置");
            return;
        }

        System.out.println("\n========== 开始上传测试 ==========");

        // 创建测试文件
        String testContent = "这是一份化学实验报告示例\n\n"
                + "实验条件：\n"
                + "温度：80°C\n"
                + "溶剂：乙醇\n"
                + "反应时间：2 小时\n\n"
                + "化合物信息：\n"
                + "苯 (C6H6) - 起始物\n"
                + "产率：85%";

        MultipartFile testFile = new MockMultipartFile(
            "file",
            "experiment_report.txt",
            "text/plain",
            testContent.getBytes()
        );

        System.out.println("待上传文件:");
        System.out.println("  - 文件名: " + testFile.getOriginalFilename());
        System.out.println("  - 大小: " + testFile.getSize() + " 字节");
        System.out.println("  - 类型: " + testFile.getContentType());

        try {
            // 调用 API - 第一步：上传
            System.out.println("\n正在调用 KimiService.uploadFileToKimi()...");
            String fileId = kimiService.uploadFileToKimi(testFile);

            System.out.println("✓ 上传成功！");
            System.out.println("\n返回的 File ID:");
            System.out.println("  " + fileId);
            System.out.println("\n该 ID 用于第二步分析，有效期约 24 小时");

            // 验证 file_id 格式
            assertNotNull(fileId, "file_id 不应为 null");
            assertTrue(fileId.startsWith("file-"), "file_id 应以 'file-' 开头");
            assertTrue(fileId.length() > 5, "file_id 长度应 > 5");

            // 保存 file_id 供后续测试使用
            System.out.println("\n========== 上传完成 ==========");
            System.out.println("\n后续步骤：使用返回的 file_id 进行第二步分析");
            System.out.println("参考代码示例：");
            System.out.println("  String result = kimiService.analyzeFileWithKimi(");
            System.out.println("    \"" + fileId + "\",");
            System.out.println("    \"请分析这份文档\"");
            System.out.println("  );");

        } catch (Exception e) {
            System.out.println("✗ 上传失败");
            System.out.println("错误信息: " + e.getMessage());
            
            // 如果是 401 错误，可能是 API Key 问题
            if (e.getMessage().contains("401")) {
                System.out.println("可能原因：API Key 无效或已过期");
                System.out.println("解决方案：");
                System.out.println("  1. 访问 https://console.moonshot.cn");
                System.out.println("  2. 重新生成 API Key");
                System.out.println("  3. 更新 application.properties 中的 kimi.api-key");
            }
            
            throw e;
        }
    }

    /**
     * 测试 5: 演示如何使用上传后的 file_id 进行分析
     */
    @Test
    @DisplayName("演示：使用 file_id 进行分析")
    void demonstrateAnalysisUsage() {
        if (kimiService == null) {
            System.out.println("⚠ 跳过：KimiService 未初始化");
            return;
        }

        System.out.println("\n========== 第二步：分析演示 ==========\n");

        // 假设我们已经有一个 file_id（来自第一步上传）
        String fileId = "file-cnm91qo68gch7vr1e3g0";  // 示例 ID

        System.out.println("已获得 File ID: " + fileId);
        System.out.println("\n现在可以调用分析 API:");
        System.out.println();
        System.out.println("String prompt = \"请分析这份化学文献，提取所有实验条件和产率\";");
        System.out.println("String result = kimiService.analyzeFileWithKimi(fileId, prompt);");
        System.out.println();
        System.out.println("分析结果格式：JSON 字符串，包含 Kimi 的分析内容");
    }

    /**
     * 测试 6: 演示一步式操作（上传+分析）
     */
    @Test
    @DisplayName("演示：一步式上传和分析")
    void demonstrateOneStepOperation() {
        if (kimiService == null) {
            System.out.println("⚠ 跳过：KimiService 未初始化");
            return;
        }

        System.out.println("\n========== 一步式操作演示 ==========\n");

        System.out.println("如果你想在一个调用中完成上传+分析，可以使用：");
        System.out.println();
        System.out.println("MultipartFile file = ...; // 你的文件");
        System.out.println("String prompt = \"请分析这份文档\";");
        System.out.println();
        System.out.println("String result = kimiService.uploadAndAnalyzeFile(file, prompt);");
        System.out.println();
        System.out.println("优点：");
        System.out.println("  • 代码简洁");
        System.out.println("  • 自动处理 file_id 传递");
        System.out.println("  • 适合简单场景");
        System.out.println();
        System.out.println("缺点：");
        System.out.println("  • 无法重复使用同一个 file_id（若需多次分析）");
        System.out.println("  • 错误处理较复杂");
    }

    /**
     * 测试 7: 错误处理演示
     */
    @Test
    @DisplayName("演示：错误处理")
    void demonstrateErrorHandling() {
        System.out.println("\n========== 错误处理演示 ==========\n");

        System.out.println("可能的错误情况：");
        System.out.println();
        System.out.println("1. API Key 无效");
        System.out.println("   HTTP Status: 401 Unauthorized");
        System.out.println("   处理: 检查并重新生成 API Key");
        System.out.println();
        System.out.println("2. 文件格式不支持");
        System.out.println("   HTTP Status: 400 Bad Request");
        System.out.println("   处理: 使用支持的格式 (PDF, Word, TXT 等)");
        System.out.println();
        System.out.println("3. 文件太大");
        System.out.println("   HTTP Status: 413 Payload Too Large");
        System.out.println("   处理: 减小文件大小或分割文件");
        System.out.println();
        System.out.println("4. 网络超时");
        System.out.println("   异常: SocketTimeoutException");
        System.out.println("   处理: 增加超时时间或重试");
        System.out.println();
        System.out.println("推荐的错误处理代码：");
        System.out.println();
        System.out.println("try {");
        System.out.println("    String fileId = kimiService.uploadFileToKimi(file);");
        System.out.println("} catch (IOException e) {");
        System.out.println("    log.error(\"文件读取失败\", e);");
        System.out.println("} catch (RuntimeException e) {");
        System.out.println("    if (e.getMessage().contains(\"401\")) {");
        System.out.println("        // 处理认证错误");
        System.out.println("    } else if (e.getMessage().contains(\"400\")) {");
        System.out.println("        // 处理请求错误");
        System.out.println("    } else {");
        System.out.println("        // 处理其他错误");
        System.out.println("    }");
        System.out.println("}");
    }
}
