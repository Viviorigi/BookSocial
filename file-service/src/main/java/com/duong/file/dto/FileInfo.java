package com.duong.file.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

@Data
@Builder
@NonFinal
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FileInfo {
    String name;
    String contentType;
    long size;
    String md5Checksum;
    String path;
    String url;
}
