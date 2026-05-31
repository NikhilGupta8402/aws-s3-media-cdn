package com.example.awslocal.controller;

import com.example.awslocal.services.S3Service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/files")
public class UploadController {

    private final S3Service s3Service;

    public UploadController(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    /*
     * Single Upload
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadSingleFile(
            @RequestParam("file")
            MultipartFile file
    ) throws IOException {

        String url;
        try {

            url = s3Service.uploadFile(file);

            return ResponseEntity.ok(
                    Map.of(
                            "message",
                            "File uploaded successfully",
                            "url",
                            url
                    )
            );

        } catch (Exception e) {

            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "message",
                            e.getMessage()
                    )
            );
        }
    }

    /*
     * Multiple Upload
     */
    @PostMapping("/upload/multiple")
    public ResponseEntity<?> uploadMultipleFiles(
            @RequestParam("files")
            MultipartFile[] files
    ) throws Exception {

        List<String> urls = null;
		try {
			urls = s3Service.uploadMultipleFiles(files);
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Files uploaded successfully",

                        "urls",
                        urls
                )
        );
    }
    
    
    /*
     * Delete File
     */
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteFile(
            @RequestParam("fileName")
            String fileName
    ) {

        try {

            s3Service.deleteFile(fileName);

            return ResponseEntity.ok(
                    Map.of(
                            "message",
                            "File deleted successfully"
                    )
            );

        } catch (Exception e) {

            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "message",
                            e.getMessage()
                    )
            );
        }
    }
    /*
     * Get All Files
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllFiles() {

        return ResponseEntity.ok(
                s3Service.getAllFiles()
        );
    }
}