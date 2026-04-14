package chem_data_platform.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class XunfeiService {

    @Value("${spark.api-key:}")
    private String sparkApiKey;

    @Value("${analysis.api.baseUrl:https://spark-api-open.xf-yun.com/v2}")
    private String baseUrl;

    @Value("${analysis.api.model:spark-x}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 将文件编码并调用讯飞星火 v2 接口，返回原始响应 JSON 字符串（或错误信息）
     */
    public String generateJsonFromFile(File file) throws IOException {
        byte[] data = Files.readAllBytes(file.toPath());
        String b64 = Base64.getEncoder().encodeToString(data);

        Map<String, Object> inputObj = new HashMap<>();
        inputObj.put("type", "file");
        inputObj.put("format", "base64");
        inputObj.put("data", b64);

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", model);
        payload.put("input", new Object[]{inputObj});

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + sparkApiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        ResponseEntity<String> resp = restTemplate.postForEntity(baseUrl + "/models/" + model + "/invoke", entity, String.class);
        return resp.getBody();
    }
}
