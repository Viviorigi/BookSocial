package com.duong.file.dto.response;


import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

@Data
@Builder
@NonFinal
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FileResponse {
    String originalFilename;
    String url;
}
