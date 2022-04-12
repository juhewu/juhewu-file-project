package org.juhewu.file.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;

import org.juhewu.file.core.aspect.DeleteAspectChain;
import org.juhewu.file.core.aspect.ExistsAspectChain;
import org.juhewu.file.core.aspect.FileStorageAspect;
import org.juhewu.file.core.aspect.UploadAspectChain;
import org.juhewu.file.core.exception.FileStorageException;
import org.juhewu.file.core.storage.FileStorage;
import org.springframework.web.multipart.MultipartFile;

import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@Setter
public class FileStorageTemplate {

    private FileStorageTemplate self;
    private FileStorageFactory fileStorageFactory;
    List<FileStorageAspect> aspectList;

    public FileStorageTemplate(FileStorageFactory fileStorageFactory,
            List<FileStorageAspect> aspectList) {
        this.fileStorageFactory = fileStorageFactory;
        this.aspectList = aspectList;
    }

    public FileInfo upload(UploadPretreatment uploadPretreatment) {
        MultipartFile file = uploadPretreatment.getFileWrapper();
        if (file == null)
            throw new FileStorageException("文件不允许为 null ！");
        if (uploadPretreatment.getStorageId() == null)
            throw new FileStorageException("platform 不允许为 null ！");

        FileInfo fileInfo = new FileInfo();
        fileInfo.setCreateTime(new Date());
        fileInfo.setSize(file.getSize());
        fileInfo.setOriginalFilename(file.getOriginalFilename());
        fileInfo.setExt(FileNameUtil.getSuffix(file.getOriginalFilename()));
        fileInfo.setObjectId(uploadPretreatment.getObjectId());
        fileInfo.setObjectType(uploadPretreatment.getObjectType());
        fileInfo.setPath(uploadPretreatment.getPath());
        fileInfo.setStorageId(uploadPretreatment.getStorageId());
        fileInfo.setMd5("");
        if (StrUtil.isNotBlank(uploadPretreatment.getSaveFilename())) {
            fileInfo.setFilename(uploadPretreatment.getSaveFilename());
        } else {
            fileInfo.setFilename(IdUtil.objectId() + (StrUtil.isEmpty(fileInfo.getExt()) ? StrUtil.EMPTY : "." + fileInfo.getExt()));
        }

        byte[] thumbnailBytes = uploadPretreatment.getThumbnailBytes();
        if (thumbnailBytes != null) {
            fileInfo.setThSize((long) thumbnailBytes.length);
            if (StrUtil.isNotBlank(uploadPretreatment.getSaveThFilename())) {
                fileInfo.setThFilename(uploadPretreatment.getSaveThFilename() + uploadPretreatment.getThumbnailSuffix());
            } else {
                fileInfo.setThFilename(fileInfo.getFilename() + uploadPretreatment.getThumbnailSuffix());
            }
        }
        FileStorage fileStorage = getFileStorage(uploadPretreatment.getStorageId());

        //处理切面
        return new UploadAspectChain(aspectList, (_fileInfo, _pre, _fileStorage) -> {
            //真正开始保存
            if (_fileStorage.upload(_fileInfo, _pre)) {
                return _fileInfo;
            }
            return null;
        }).next(fileInfo, uploadPretreatment, fileStorage);
    }

    /**
     * 根据条件
     */
    public boolean delete(FileInfo fileInfo) {
        return delete(fileInfo,null);
    }

    /**
     * 根据条件删除文件
     */
    public boolean delete(FileInfo fileInfo, Predicate<FileInfo> predicate) {
        if (fileInfo == null) return true;
        if (predicate != null && !predicate.test(fileInfo)) return false;
        FileStorage fileStorage = getFileStorage(fileInfo.getStorageId());
        if (fileStorage == null) throw new FileStorageException("没有找到对应的存储平台！");

        return new DeleteAspectChain(aspectList,(_fileInfo,_fileStorage) -> {
            return _fileStorage.delete(_fileInfo);   //删除文件
        }).next(fileInfo,fileStorage);
    }


    /**
     * 文件是否存在
     */
    public boolean exists(FileInfo fileInfo) {
        if (fileInfo == null) return false;
        return new ExistsAspectChain(aspectList,(_fileInfo,_fileStorage) ->
                _fileStorage.exists(_fileInfo)
        ).next(fileInfo,getFileStorage(fileInfo.getStorageId()));
    }


    /**
     * 获取文件下载器
     */
    public Downloader download(FileInfo fileInfo) {
        return new Downloader(fileInfo,aspectList,getFileStorage(fileInfo.getStorageId()),Downloader.TARGET_FILE);
    }


    /**
     * 获取缩略图文件下载器
     */
    public Downloader downloadTh(FileInfo fileInfo) {
        return new Downloader(fileInfo,aspectList,getFileStorage(fileInfo.getStorageId()),Downloader.TARGET_TH_FILE);
    }

    private FileStorage getFileStorage(String id) {
        return this.fileStorageFactory.getFileStorage(id);
    }

    /**
     * 创建上传预处理器
     */
    public UploadPretreatment of() {
        UploadPretreatment pre = new UploadPretreatment();
        pre.setFileStorageTemplate(this.self);
        return pre;
    }

    /**
     * 根据 MultipartFile 创建上传预处理器
     */
    public UploadPretreatment of(MultipartFile file) {
        UploadPretreatment pre = of();
        pre.setFileWrapper(new MultipartFileWrapper(file));
        return pre;
    }

    /**
     * 根据 byte[] 创建上传预处理器，name 为空字符串
     */
    public UploadPretreatment of(byte[] bytes) {
        UploadPretreatment pre = of();
        pre.setFileWrapper(new MultipartFileWrapper(new MockMultipartFile("", bytes)));
        return pre;
    }

    /**
     * 根据 InputStream 创建上传预处理器，originalFilename 为空字符串
     */
    public UploadPretreatment of(InputStream in) {
        try {
            UploadPretreatment pre = of();
            pre.setFileWrapper(new MultipartFileWrapper(new MockMultipartFile("", in)));
            return pre;
        } catch (Exception e) {
            throw new FileStorageException("根据 InputStream 创建上传预处理器失败！", e);
        }
    }

    /**
     * 根据 File 创建上传预处理器，originalFilename 为 file 的 name
     */
    public UploadPretreatment of(File file) {
        try {
            UploadPretreatment pre = of();
            pre.setFileWrapper(new MultipartFileWrapper(new MockMultipartFile(file.getName(), file.getName(), null, new FileInputStream(file))));
            return pre;
        } catch (Exception e) {
            throw new FileStorageException("根据 File 创建上传预处理器失败！", e);
        }
    }
}
