## 文件存储
在 Spring Boot 项目中简单的将文件存储到：aws-s3、服务器本地、自定义的渠道，同时支持多个存储方式，上传文件时指定存储方式。

## 快速开始

1. 引入依赖
```xml
        <dependency>
            <groupId>org.juhewu</groupId>
            <artifactId>juhewu-file-spring-boot-starter</artifactId>
            <version>1.0.0</version>
        </dependency>
```

2. 添加配置

```yaml
juhewu:
  file-storage:
    # 默认的存储
    default-storage-id: local-1
    # 本地存储
    local:
      - storageId: local-1
        basePath: /Users/duanjw/Downloads/file
        domain: http://localhost:8080/test
        # 启用 http 访问（不建议使用，线上建议使用 Nginx 代理，效率更高）
        enable-access: true
        # 访问路径，开启 enable-access 后，通过 domain + 此路径 可以访问到上传的文件
        path-patterns: /test/**
      - storageId: local-2
        basePath: /Users/duanjw/Downloads/file1
        domain: http://localhost:8080/test1
        enable-access: true
        path-patterns: /test1/**
    # s3 协议的通用存储
    aws-s3:
      - storageId: aws-1
        accessKey: minioadmin
        secretKey: minioadmin
        endPoint: http://localhost:9000
        bucketName: 100
        # 根目录，默认是 /，如果设置，则文件会上传到该目录
        basePath: test
        pathStyleAccess: true
      - storageId: aws-2
        accessKey: minioadmin
        secretKey: minioadmin
        endPoint: http://localhost:9000
        bucketName: 200
        # 根目录，默认是 /
        basePath: pic
        domain: http://localhost:9000/200
        # true path-style nginx 反向代理和S3默认支持 pathStyle {http://endpoint/bucketname} false
        # supports virtual-hosted-style 阿里云等需要配置为 virtual-hosted-style
        # 模式{http://bucketname.endpoint}
        pathStyleAccess: true
    # 自定义存储
    other:
      - storageId: other-1
        # 自定义存储的实现类
        storageClass: org.juhewu.file.core.storage.CustomerFileStorage
        params:
          accessKey: test

```

2. 注入 oss 对象

```java
    private final FileStorageTemplate template;
```
## 功能说明

1. 上传文件到默认的存储

配置文件需要配置`juhewu.file-storage.default-storage-id`。

```java
    template.of(file).setStorageId(storageId).upload();
```

2. 上传文件到指定的存储

```java
    template.of(file).setStorageId(storageId).upload();
```

3. 下载对象

```java
    template.download(fileinfo).outputStream(response.getOutputStream());
```


参考：https://github.com/1171736840/spring-file-storage.git
