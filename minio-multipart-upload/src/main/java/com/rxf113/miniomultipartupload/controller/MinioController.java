package com.rxf113.miniomultipartupload.controller;

import com.rxf113.miniomultipartupload.minio.MinioClientService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping(value = "/minio/")
@AllArgsConstructor
public class MinioController {

    private final MinioClientService minioClientService;

    /**
     * 基本分片字节单位(5m)
     */
    private static final int FIX_FILE_BASE_UNIT = 5242880;

    /**
     * 获取urls
     */
    @GetMapping("urls")
    @CrossOrigin
    public ResponseEntity<Map<String, Object>> getUrls(@RequestParam Long fileSize, @RequestParam String fileName) throws Exception {
        int chunkNum = getChunkNum(fileSize);
        CompletableFuture<Map<String, Object>> completableFuture = minioClientService.getUrls("rxf113", fileName, chunkNum);
        Map<String, Object> map = completableFuture.get();
        //返回分块大小供客户端分块,方便扩展分块规则
        map.put("chunkSize", FIX_FILE_BASE_UNIT);
        return ResponseEntity.ok(map);
    }

    /**
     * 暂时固定按照 5m 为单位拆分, 可拓展
     * 获取分片数
     * egg: 16m -> 5m + 5m + 6m ： chunkNum = 3
     * egg: 3m -> 3m ： chunkNum = 1
     *
     * @param fileSize 文件字节数
     * @return chunkNum
     */
    private int getChunkNum(long fileSize) {
        return fileSize <= FIX_FILE_BASE_UNIT ? 1 : (int) (fileSize / FIX_FILE_BASE_UNIT);
    }

    /**
     * 聚合多个块
     *
     * @return
     */
    @GetMapping("agg")
    @CrossOrigin
    public ResponseEntity<Boolean> aggregate(@RequestParam String uploadId) {
        boolean res = minioClientService.aggMultipart(uploadId);
        return ResponseEntity.ok(res);
    }
}
