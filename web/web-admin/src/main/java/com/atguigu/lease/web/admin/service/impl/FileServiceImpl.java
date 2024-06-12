package com.atguigu.lease.web.admin.service.impl;

import com.atguigu.lease.common.minio.MinioProperties;
import com.atguigu.lease.web.admin.service.FileService;
import io.minio.*;
import io.minio.errors.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {

    @Autowired
    private MinioProperties minioProperties;
    @Autowired
    private MinioClient minioClient;


    @Override
    public String upload(MultipartFile file) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        boolean bucketExists = minioClient.
                bucketExists(BucketExistsArgs.builder().
                        bucket(minioProperties.getBucketName()).build());
        // 如果没有这个存储桶就创建
        if (!bucketExists) {
            minioClient.
                    makeBucket(MakeBucketArgs.builder().
                            bucket(minioProperties.getBucketName()).build());
            // 设置存储桶策略
            minioClient.
                    setBucketPolicy(SetBucketPolicyArgs.builder().
                            bucket(minioProperties.getBucketName()).
                            config(createBucketPolicyConfig(minioProperties.getBucketName())).
                            build());
        }

        String fileName = new SimpleDateFormat("yyyyMMdd").format(new Date()) + "/" +
                UUID.randomUUID() + "-" + file.getOriginalFilename();
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(minioProperties.getBucketName())
                .object(fileName)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build());

        return String.join("/", minioProperties.getEndpoint(), minioProperties.getBucketName(), fileName);

    }

    /**
     * 生成用于描述指定bucket访问权限的JSON字符串。
     *
     * @param bucketName
     * @return
     */
    private String createBucketPolicyConfig(String bucketName) {
        return """
                {
                "Statement" : [ {
                    "Action" : "s3:GetObject",
                    "Effect" : "Allow",
                    "Principal" : "*",
                    "Resource" : "arn:aws:s3:::%s/*"
                  } ],
                  "Version" : "2012-10-17"
                }
                """.formatted(bucketName);
    }
}
