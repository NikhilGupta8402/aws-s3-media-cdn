package com.example.awslocal.services;

import jakarta.annotation.PostConstruct;

import net.coobird.thumbnailator.Thumbnails;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class S3Service {

    private final S3Client s3Client;

    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.endpoint}")
    private String endpoint;
    
    @Value("${cdn.url}")
    private String cdnUrl;

    @PostConstruct
    public void createBucketIfNotExists() {

        try {

            s3Client.headBucket(
                    HeadBucketRequest.builder()
                            .bucket(bucketName)
                            .build()
            );

            System.out.println("Bucket already exists");

        } catch (Exception e) {

            s3Client.createBucket(
                    CreateBucketRequest.builder()
                            .bucket(bucketName)
                            .build()
            );

            System.out.println("Bucket created");
        }
    }

    /*
     * Compress + Convert to WebP
     */
    private byte[] optimizeImage(MultipartFile file)
            throws IOException {

        BufferedImage bufferedImage =
                ImageIO.read(file.getInputStream());

        ByteArrayOutputStream outputStream =
                new ByteArrayOutputStream();

        Thumbnails.of(bufferedImage)
                .size(1920, 1080)
                .outputFormat("webp")
                .outputQuality(0.9f)
                .toOutputStream(outputStream);

        return outputStream.toByteArray();
    }
    

//    private byte[] optimizeImage(MultipartFile file) throws IOException {
//
//        BufferedImage bufferedImage =
//                ImageIO.read(file.getInputStream());
//
//        ByteArrayOutputStream outputStream =
//                new ByteArrayOutputStream();
//
//        Thumbnails.of(bufferedImage)
//                .scale(1.0) // keep original dimensions
//                .outputFormat("webp")
//                .outputQuality(1.0f) // maximum quality
//                .toOutputStream(outputStream);
//
//        return outputStream.toByteArray();
//    }
    /*
     * Single Upload
     */
    public String uploadFile(MultipartFile file)
            throws Exception {

        String contentType = file.getContentType();

        String originalFileName =
                file.getOriginalFilename();

        String cleanFileName = originalFileName
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-zA-Z0-9._-]", "");

        String fileName;

        byte[] uploadBytes;

        String uploadContentType;

        /*
         * IMAGE
         */
        if (contentType != null &&
                contentType.startsWith("image")) {

            fileName =
                    UUID.randomUUID() + "_" +
                            cleanFileName
                                    .replaceAll("\\.[^.]+$", "")
                            + ".webp";

            uploadBytes = optimizeImage(file);

            uploadContentType = "image/webp";
        }

        /*
         * VIDEO
         */
        else if (contentType != null &&
                contentType.startsWith("video")) {

            fileName =
                    UUID.randomUUID() + "_" +
                            cleanFileName;

            uploadBytes = file.getBytes();

            uploadContentType = contentType;
        }
        /*
         * OTHER FILES
         */
        else {

            fileName =
                    UUID.randomUUID() + "_" +
                            cleanFileName;

            uploadBytes = file.getBytes();

            uploadContentType = contentType;
        }

        PutObjectRequest putObjectRequest =
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(fileName)
                        .contentType(uploadContentType)
                        .contentDisposition("inline")
                        .build();

        s3Client.putObject(
                putObjectRequest,
                RequestBody.fromBytes(uploadBytes)
        );

        return cdnUrl +
                "/" +
                bucketName +
                "/" +
                fileName;
    }

    /*
     * Multiple Upload
     */
    public List<String> uploadMultipleFiles(
            MultipartFile[] files
    ) throws Exception {

        List<String> uploadedUrls =
                new ArrayList<>();

        for (MultipartFile file : files) {

            uploadedUrls.add(
                    uploadFile(file)
            );
        }

        return uploadedUrls;
    }
    
    public void deleteFile(String fileName) {

        DeleteObjectRequest deleteObjectRequest =
                DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(fileName)
                        .build();

        s3Client.deleteObject(deleteObjectRequest);
    }
    
    public List<Map<String, String>> getAllFiles() {

        List<Map<String, String>> files =
                new ArrayList<>();

        s3Client.listObjectsV2Paginator(builder ->
                builder.bucket(bucketName))
                .contents()
                .forEach(object -> {

                    String fileUrl =
                            cdnUrl +
                            "/" +
                            bucketName +
                            "/" +
                            object.key();

                    /*
                     * Skip thumbnails
                     */
                    if (!object.key().startsWith("thumb_")) {

                        String fileType = "image";

                        if (object.key().endsWith(".mp4") ||
                                object.key().endsWith(".mkv") ||
                                object.key().endsWith(".webm")) {

                            fileType = "video";
                        }

                        /*
                         * File size
                         */
                        long sizeInBytes = object.size();

                        String formattedSize;

                        if (sizeInBytes < 1024) {

                            formattedSize =
                                    sizeInBytes + " B";

                        } else if (sizeInBytes < 1024 * 1024) {

                            formattedSize =
                                    String.format(
                                            "%.2f KB",
                                            sizeInBytes / 1024.0
                                    );

                        } else {

                            formattedSize =
                                    String.format(
                                            "%.2f MB",
                                            sizeInBytes / (1024.0 * 1024.0)
                                    );
                        }

                        files.add(
                                Map.of(
                                        "imageUrl",
                                        fileUrl,

                                        "type",
                                        fileType,

                                        "size",
                                        formattedSize,

                                        "fileName",
                                        object.key()
                                )
                        );
                    }
                });

        return files;
    }
}