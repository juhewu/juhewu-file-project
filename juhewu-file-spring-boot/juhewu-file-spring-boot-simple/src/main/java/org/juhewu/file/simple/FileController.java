package org.juhewu.file.simple;

import javax.servlet.ServletResponse;

import org.juhewu.file.config.FileStorageProperties;
import org.juhewu.file.core.FileInfo;
import org.juhewu.file.core.FileStorageTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import cn.hutool.core.io.FileUtil;
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

    @PostMapping
    @SneakyThrows
    public void down(MultipartFile file, ServletResponse response) {
        FileInfo upload = template.of(file).setStorageId(properties.getDefaultStorageId()).upload();
        template.download(upload).outputStream(response.getOutputStream());
    }
}
