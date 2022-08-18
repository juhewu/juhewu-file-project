package org.juhewu.file.core.storage;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;

import org.juhewu.file.core.FileInfo;
import org.juhewu.file.core.UploadPretreatment;
import org.juhewu.file.core.exception.FileStorageException;
import org.juhewu.file.core.storage.config.BaseConfig;

import com.luhuiguo.fastdfs.conn.ConnectionManager;
import com.luhuiguo.fastdfs.conn.ConnectionPoolConfig;
import com.luhuiguo.fastdfs.conn.FdfsConnectionPool;
import com.luhuiguo.fastdfs.conn.PooledConnectionFactory;
import com.luhuiguo.fastdfs.conn.TrackerConnectionManager;
import com.luhuiguo.fastdfs.domain.StorePath;
import com.luhuiguo.fastdfs.service.DefaultFastFileStorageClient;
import com.luhuiguo.fastdfs.service.DefaultTrackerClient;
import com.luhuiguo.fastdfs.service.FastFileStorageClient;
import com.luhuiguo.fastdfs.service.TrackerClient;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;

/**
 * fastDFS 存储
 *
 * @author duanjw
 */
public class FastDFSFileStorage implements FileStorage {

    final private FastFileStorageClient fastFileStorageClient;
    final private FastDFS fastDFS;

    public FastDFSFileStorage(FastDFS fastDFS) {
        this.fastDFS = fastDFS;
        FdfsConnectionPool pool = new FdfsConnectionPool(getPooledConnectionFactory(), getConnectionPoolConfig());
        TrackerConnectionManager tcm = new TrackerConnectionManager(pool, fastDFS.getTrackerList());
        TrackerClient trackerClient = new DefaultTrackerClient(tcm);
        ConnectionManager cm = new ConnectionManager(pool);
        fastFileStorageClient = new DefaultFastFileStorageClient(trackerClient, cm);
    }

    /**
     * 连接池工厂配置
     *
     * @return
     */
    private PooledConnectionFactory getPooledConnectionFactory() {
        PooledConnectionFactory pooledConnectionFactory = new PooledConnectionFactory();
        pooledConnectionFactory.setSoTimeout(this.fastDFS.getSoTimeout());
        pooledConnectionFactory.setConnectTimeout(this.fastDFS.getConnectTimeout());
        return pooledConnectionFactory;
    }

    /**
     * 获取连接池
     *
     * @return
     */
    public ConnectionPoolConfig getConnectionPoolConfig() {
        FastDFS.Pool pool = this.fastDFS.getPool();
        ConnectionPoolConfig connectionPoolConfig = new ConnectionPoolConfig();
        connectionPoolConfig.setMaxTotal(pool.getMaxTotal());
        connectionPoolConfig.setMaxWaitMillis(pool.getMaxWaitMillis());
        connectionPoolConfig.setMaxTotalPerKey(pool.getMaxTotalPerKey());
        connectionPoolConfig.setMaxIdlePerKey(pool.getMaxIdlePerKey());
        connectionPoolConfig.setMinIdlePerKey(pool.getMinIdlePerKey());
        return connectionPoolConfig;
    }

    /**
     * 获取存储 id
     *
     * @return
     */
    @Override
    public String getStorageId() {
        return fastDFS.getStorageId();
    }

    /**
     * 上传文件
     *
     * @param fileInfo
     * @param uploadPretreatment
     * @return
     */
    @SneakyThrows
    @Override
    public boolean upload(FileInfo fileInfo, UploadPretreatment uploadPretreatment) {
        StorePath storePath = fastFileStorageClient.uploadFile(uploadPretreatment.getFileWrapper().getBytes(), fileInfo.getExt());
        fileInfo.setPath(storePath.getFullPath());
        return true;
    }

    @Override
    public boolean delete(FileInfo fileInfo) {
        fastFileStorageClient.deleteFile(fileInfo.getBasePath(), fileInfo.getPath());
        return true;
    }

    @Override
    public boolean exists(FileInfo fileInfo) {
        return fastFileStorageClient.queryFileInfo(fileInfo.getBasePath(), fileInfo.getPath()) != null;
    }

    @Override
    public void download(FileInfo fileInfo, Consumer<InputStream> consumer) {
        // 通过 path 解析 groupName 和 path
        FileDTO fileDTO = new FileDTO(fileInfo.getPath());
        try (InputStream inputStream = new ByteArrayInputStream(fastFileStorageClient.downloadFile(fileDTO.getFileGroup(), fileDTO.getFilePath()))) {
            consumer.accept(inputStream);
        } catch (Exception e) {
            throw new FileStorageException("文件下载失败！platform：" + fileInfo, e);
        }
    }

    @Override
    public void downloadTh(FileInfo fileInfo, Consumer<InputStream> consumer) {

    }

    /**
     * FastDFS
     */
    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class FastDFS extends BaseConfig {

        /**
         * true path-style nginx 反向代理和S3默认支持 pathStyle {http://endpoint/bucketname} false
         * supports virtual-hosted-style 阿里云等需要配置为 virtual-hosted-style
         * 模式{http://bucketname.endpoint}
         */
        private boolean pathStyleAccess = true;
        /**
         * 访问域名
         */
        private String domain = "";

        /**
         * bucket 不存在自动创建
         */
        private boolean bucketAutoCreate = true;

        private String groupName = "group1";
        private List<String> trackerList;

        private int soTimeout = 1000;
        private int connectTimeout = 1000;
        /**
         * 连接池
         */
        private Pool pool;

        @Data
        public static class Pool {

            /**
             * 从池中借出的对象的最大数目（配置为-1表示不限制）
             */
            private int maxTotal = -1;
            /**
             * 获取连接时的最大等待毫秒数(默认配置为5秒)
             */
            private long maxWaitMillis = 5 * 1000;
            /**
             * 每个key最大连接数
             */
            private int maxTotalPerKey = 50;
            /**
             * 每个key对应的连接池最大空闲连接数
             */
            private int maxIdlePerKey = 10;
            /**
             * 每个key对应的连接池最小空闲连接数
             */
            private int minIdlePerKey = 5;

        }
    }

    @Data
    public static class FileDTO {

        /**
         * 文件ID
         */
        private String fileId;
        /**
         * 文件名称
         */
        private String fileName;
        /**
         * 文件后缀
         */
        private String fileSuffix;
        /**
         * 文件字节数组
         */
        private byte[] fileByte;
        /**
         * 文件所属Group
         */
        private String fileGroup;
        /**
         * 文件路径
         */
        private String filePath;

        public FileDTO(String fileId) {
            this.fileId = fileId;
            int index = this.fileId.indexOf("/");
            int lastIndex = this.fileId.lastIndexOf("/");
            this.fileGroup = this.fileId.substring(0, index);
            this.filePath = this.fileId.substring(index + 1, this.fileId.length());
            this.fileName = this.fileId.substring(lastIndex + 1, this.fileId.length());
            String[] fileNameArr = this.fileId.split("\\.");
            this.fileSuffix = fileNameArr[fileNameArr.length > 0 ? fileNameArr.length - 1 : 0];
        }
    }

}
