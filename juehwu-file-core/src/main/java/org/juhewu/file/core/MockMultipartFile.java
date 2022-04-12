package org.juhewu.file.core;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import org.springframework.web.multipart.MultipartFile;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import lombok.Getter;

/**
 * 一个模拟 MultipartFile 的类
 */
@Getter
public class MockMultipartFile implements MultipartFile {

    /**
     * 文件名
     */
    private final String name;

    /**
     * 原始文件名
     */
    private final String originalFilename;

    /**
     * 内容类型
     */
    private final String contentType;

    /**
     * 文件内容
     */
    private final byte[] bytes;

    public MockMultipartFile(String name, InputStream in) {
        this(name, "", null, IoUtil.readBytes(in));
    }

    public MockMultipartFile(String name, byte[] bytes) {
        this(name, "", null, bytes);
    }

    public MockMultipartFile(String name, String originalFilename, String contentType, InputStream in) {
        this(name, originalFilename, contentType, IoUtil.readBytes(in));
    }

    public MockMultipartFile(String name, String originalFilename, String contentType, byte[] bytes) {
        this.name = (name != null ? name : "");
        this.originalFilename = (originalFilename != null ? originalFilename : "");
        this.contentType = contentType;
        this.bytes = (bytes != null ? bytes : new byte[0]);
    }

    @Override
    public boolean isEmpty() {
        return (this.bytes.length == 0);
    }

    @Override
    public long getSize() {
        return this.bytes.length;
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(this.bytes);
    }

    @Override
    public void transferTo(File dest) {
        FileUtil.writeBytes(bytes, dest);
    }

}