package com.duong.file.repository;

import com.duong.file.dto.FileInfo;
import com.duong.file.entity.FileMgmt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Repository;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Objects;
import java.util.UUID;

@Repository
public class FileRepository {

    @Value(value = "${app.file.storage-dir}")
    String storageDir;
    @Value(value = "${app.file.download-prefix}")
    String urlPrefix;

    public FileInfo store(MultipartFile file) throws IOException {
        Path folder = Paths.get(storageDir);
        Files.createDirectories(folder);

        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String fileName = (ext == null)
                ? UUID.randomUUID().toString()
                : UUID.randomUUID() + "." + ext;

        Path filePath = folder.resolve(fileName).normalize().toAbsolutePath();

        byte[] bytes = file.getBytes(); // chỉ đọc 1 lần
        Files.write(filePath, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        return FileInfo.builder()
                .name(fileName)
                .size((long) bytes.length)
                .contentType(file.getContentType())
                .md5Checksum(DigestUtils.md5DigestAsHex(bytes))
                .path(filePath.toString())
                .url(urlPrefix + fileName)
                .build();
    }

    public Resource read(FileMgmt fileMgmt) throws IOException {
        var data = Files.readAllBytes(Path.of(fileMgmt.getPath()));
        return new ByteArrayResource(data);
    }
}
