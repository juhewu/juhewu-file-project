package org.juhewu.file.simple;

import javax.servlet.ServletResponse;

import org.juhewu.file.config.FileStorageProperties;
import org.juhewu.file.core.FileInfo;
import org.juhewu.file.core.FileStorageTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
     * curl -X POST 'localhost:8080/upload?url=https%3A%2F%2Fwww.baidu.com%2Fimg%2FPCfb_5bf082d29588c07f842ccde3f97243ea.png&originalFileName=baidu.png'
     *
     * @param url 文件地址
     * @return 文件信息
     */
    @PostMapping("upload")
    public FileInfo update(@RequestParam String url, @RequestParam String originalFileName) {
        return template.of(url).setOriginalFilename(originalFileName).setStorageId(properties.getDefaultStorageId()).upload();
    }

    @PostMapping
    @SneakyThrows
    public void down(MultipartFile file, ServletResponse response) {
        FileInfo upload = template.of(file).setStorageId(properties.getDefaultStorageId()).upload();
        template.download(upload).outputStream(response.getOutputStream());
    }
}
