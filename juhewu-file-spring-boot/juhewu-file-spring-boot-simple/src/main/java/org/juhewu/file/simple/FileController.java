package org.juhewu.file.simple;

import javax.servlet.ServletResponse;

import org.juhewu.file.config.FileStorageProperties;
import org.juhewu.file.core.FileInfo;
import org.juhewu.file.core.FileStorageTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

@RestController
@RequestMapping
@AllArgsConstructor
public class FileController {

    private final FileStorageTemplate template;
    private final FileStorageProperties properties;

    /**
     * 上传文件到默认的存储
     *
     * @param file 文件
     * @return 文件信息
     */
    @PostMapping
    public FileInfo update(MultipartFile file) {
        return template.of(file).setStorageId(properties.getDefaultStorageId()).upload();
    }

    /**
     * 上传文件到指定的存储
     *
     * @param id 存储 id
     * @param file 文件
     * @return 文件信息
     */
    @PostMapping("{id}")
    public FileInfo update(@PathVariable String id, MultipartFile file) {
        return template.of(file).setStorageId(id).upload();
    }

    /**
     * 根据文件地址将文件上传到默认存储
     *
     * @param url 文件地址
     * @param originalFileName 文件名
     * @return 文件信息
     */
    @PostMapping("upload")
    public FileInfo update(@RequestParam String url, @RequestParam String originalFileName) {
        return template.of(url).setOriginalFilename(originalFileName).setStorageId(properties.getDefaultStorageId()).upload();
    }

    /**
     * 下载文件
     * curl -X POST 'localhost:8080/download' \
     * -H 'Content-Type: application/json' \
     * -d '{ "path": "group1/M00/00/00/ClIaoGL9rsaANRKnAAAAApiu_FM394.txt", "storageId": "fastdfs-1"}'
     * @param fileInfo
     * @param response
     */
    @PostMapping("download")
    @SneakyThrows
    public void download(@RequestBody FileInfo fileInfo, ServletResponse response) {
        template.download(fileInfo).outputStream(response.getOutputStream());
    }
}
