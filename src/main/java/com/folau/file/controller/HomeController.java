package com.folau.file.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;

import static org.springframework.http.HttpStatus.OK;

@Slf4j
@Controller
@RequestMapping("/files")
public class HomeController {

    @Operation(summary = "Download File")
    @GetMapping(value = "/download")
    public void downloadFile(@RequestParam Long id, HttpServletRequest request, HttpServletResponse response) throws IOException {

        String fileName = null;

        if(id == null) {
            log.error("File ID is required");
            fileName = "superman.jpeg";
        }else {
            fileName = "image"+id+".jpeg";
        }

        ClassPathResource resource = new ClassPathResource(fileName);

        File file = resource.getFile();

        log.info("fileName: {}, filePath: {}", fileName, file.getAbsolutePath());

        // Send the file
        FileInputStream fis;
        BufferedInputStream in = null;
        ServletOutputStream out = null;
        try {
            out = response.getOutputStream();
            String contentType = Files.probeContentType(file.toPath());
            log.info("Content Type: {}", contentType);
            response.setContentType(contentType);
            response.addHeader("Content-disposition", "attachment;filename="+fileName);
            // Helps with caching
            String eTag = generateETag(file);
            response.setHeader("ETag", eTag);
            log.info("ETag: {}", eTag);
            // Helps with caching
            long lastModified = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
            response.setDateHeader("Last-Modified", lastModified);
            fileName = file.getAbsolutePath();
            fis = new FileInputStream(fileName);
            in = new BufferedInputStream(fis);
            byte[] buffer = new byte[1024]; // 1KB buffer
            while (true) {
                int amountRead = in.read(buffer);
                if (amountRead == -1) {
                    break;
                }
                out.write(buffer, 0, amountRead);
            }
        } catch(Exception e) {
            String name = e.getClass().getName();
            if (name.equals("org.apache.catalina.connector.ClientAbortException")) {
                // Ignore
            } else {
                log.info("Error sending file to client: {}", fileName, e);
            }
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }

        log.info("File downloaded successfully");
    }

    private String generateETag(File file) {
        try {
            // Use file's last modified time and size to generate a simple hash
            String data = file.lastModified() + ":" + file.length();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes());
            return "\"" + Base64.getEncoder().encodeToString(hash) + "\"";
        } catch (Exception e) {
            log.error("Failed to generate ETag", e);
            return "";
        }
    }
}
