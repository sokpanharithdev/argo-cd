package com.argo.cd.controller;

import com.argo.cd.service.MinioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class MinioController {
  private final MinioService minioService;

  @PostMapping("/upload")
  public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) {
    return new ResponseEntity<>(this.minioService.upload(file), HttpStatus.OK);
  }

  @GetMapping("/{name}")
  public ResponseEntity<String> download(@PathVariable String name) {
    return new ResponseEntity<>(this.minioService.getPresignedUrl(name), HttpStatus.OK);
  }
}
