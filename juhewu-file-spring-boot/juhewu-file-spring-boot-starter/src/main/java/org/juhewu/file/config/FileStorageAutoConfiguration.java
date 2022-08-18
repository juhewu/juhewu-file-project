package org.juhewu.file.config;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.juhewu.file.core.FileStorageFactory;
import org.juhewu.file.core.FileStorageTemplate;
import org.juhewu.file.core.aspect.FileStorageAspect;
import org.juhewu.file.core.storage.AwsS3FileStorage;
import org.juhewu.file.core.storage.FastDFSFileStorage;
import org.juhewu.file.core.storage.FileStorage;
import org.juhewu.file.core.storage.LocalFileStorage;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration(proxyBeanMethods = false)
@Import(FileStorageProperties.class)
@AllArgsConstructor
public class FileStorageAutoConfiguration implements WebMvcConfigurer {

    private final FileStorageProperties properties;
    private final ApplicationContext applicationContext;

    /**
     * 配置本地存储的访问地址
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        for (LocalFileStorage.Local local : properties.getLocal()) {
            if (local.isEnableAccess()) {
                registry.addResourceHandler(local.getPathPatterns()).addResourceLocations("file:" + StrUtil.addSuffixIfNot(local.getBasePath(),
                        File.separator));
            }
        }
    }

    /**
     * 本地存储
     */
    @Bean
    public List<LocalFileStorage> localFileStorages() {
        return properties.getLocal().stream().map(local -> {
            if (local.isEnable()) {
                log.info("加载 local 存储平台：{}", local.getStorageId());
                return new LocalFileStorage(local);
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * 本地存储 Bean
     */
    @Bean
    public List<AwsS3FileStorage> awsS3FileStorages() {
        return properties.getAwsS3().stream().map(item -> {
            if (item.isEnable()) {
                log.info("加载 aws-s3 存储平台：{}", item.getStorageId());
                return new AwsS3FileStorage(item);
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * 本地存储 Bean
     */
    @Bean
    public List<FastDFSFileStorage> fastDFSFileStorages() {
        return properties.getFastDFS().stream().map(item -> {
            if (item.isEnable()) {
                log.info("加载 fast dfs 存储平台：{}", item.getStorageId());
                return new FastDFSFileStorage(item);
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }
    /**
     * 自己扩展的 storage
     *
     * @return
     */
    @Bean
    public List<FileStorage> otherStorages() {
        return properties.getOther().stream().map(item -> {
            if (item.isEnable()) {
                log.info("加载 other 存储平台：{}，对应的 storageClass：{}", item.getStorageId(), item.getStorageClass());
                Constructor<FileStorage> declaredConstructor;
                try {
                    Constructor[] declaredConstructors = item.getStorageClass().getDeclaredConstructors();
                    declaredConstructor = Arrays.stream(declaredConstructors).filter(obj -> obj.getParameterCount() == 1).findFirst()
                            .orElseThrow(NullPointerException::new);
                    return declaredConstructor.newInstance(item);
                } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * 文件存储工厂
     *
     * @param fileStorages
     * @return
     */
    @Bean
    public FileStorageFactory fileStorageFactory(List<List<? extends FileStorage>> fileStorages) {
        FileStorageFactory fileStorageFactory = new FileStorageFactory();
        Map<String, FileStorage> collect = fileStorages.stream().flatMap(Collection::stream).collect(Collectors.toMap(FileStorage::getStorageId, Function.identity()));
        fileStorageFactory.setFileStorageMap(collect);
        return fileStorageFactory;
    }

    /**
     * 文件存储服务
     */
    @Bean
    public FileStorageTemplate fileStorageTemplate(FileStorageFactory fileStorageFactory,
            List<FileStorageAspect> aspectList) {
        this.initDetect();
        FileStorageTemplate template = new FileStorageTemplate(fileStorageFactory, aspectList);
        template.setAspectList(new CopyOnWriteArrayList<>(aspectList));
        return template;
    }

    /**
     * 对 FileStorageTemplate 注入自己的代理对象，不然会导致针对 FileStorageTemplate 的代理方法不生效
     */
    @EventListener(ContextRefreshedEvent.class)
    public void onContextRefreshedEvent() {
        FileStorageTemplate template = applicationContext.getBean(FileStorageTemplate.class);
        template.setSelf(template);
    }

    public void initDetect() {
        String template = "检测到{}配置，但是没有找到对应的依赖库，所以无法加载此存储平台！配置参考地址：https://gitee.com/XYW1171736840/spring-file-storage";
        if (CollUtil.isNotEmpty(properties.getAwsS3()) && doesNotExistClass("com.amazonaws.services.s3.AmazonS3")) {
            log.warn(template, " AmazonS3 ");
        }
    }

    /**
     * 判断是否没有引入指定 Class
     */
    public static boolean doesNotExistClass(String name) {
        try {
            Class.forName(name);
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }
}
