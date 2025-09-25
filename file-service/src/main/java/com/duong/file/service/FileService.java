package com.duong.file.service;


import com.duong.file.dto.response.FileData;
import com.duong.file.dto.response.FileResponse;
import com.duong.file.exception.AppException;
import com.duong.file.exception.ErrorCode;
import com.duong.file.mapper.FileMgmtMapper;
import com.duong.file.repository.FileMgmtRepository;
import com.duong.file.repository.FileRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FileService {

    FileRepository fileRepository;
    FileMgmtRepository fileMgmtRepository;
    FileMgmtMapper fileMgmtMapper;

    public FileResponse uploadFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.FILE_EMPTY);
        }
        if (file.getSize() > 5 * 1024 * 1024) { // >5MB
            throw new AppException(ErrorCode.FILE_TOO_LARGE);
        }
        List<String> allowed = List.of("image/png", "image/jpeg", "image/webp");
        if (!allowed.contains(file.getContentType())) {
            throw new AppException(ErrorCode.FILE_UNSUPPORTED_TYPE);
        }

        var fileInfo = fileRepository.store(file);

        var fileMgmt = fileMgmtMapper.toFileMgmt(fileInfo);
        fileMgmt.setId(fileInfo.getName()); // ID = tên file
        fileMgmt.setOwnerId(SecurityContextHolder.getContext().getAuthentication().getName());
        fileMgmtRepository.save(fileMgmt);

        return FileResponse.builder()
                .originalFilename(file.getOriginalFilename())
                .url(fileInfo.getUrl())
                .build();
    }

    public FileData download(String fileName) throws IOException {
        var fileMgmt =fileMgmtRepository.findById(fileName).orElseThrow(
                () -> new AppException(ErrorCode.FILE_NOT_FOUND));

        var resource = fileRepository.read(fileMgmt);

        return new FileData(fileMgmt.getContentType(),resource);
    }
}
