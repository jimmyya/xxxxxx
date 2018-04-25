package com.chen.dao;

import com.chen.model.FileModel;

import java.util.List;

public interface FileMapper {
    void insertFileModel(FileModel file);

    List<FileModel> queryFileModel(FileModel file);

    void deleteFileModel(FileModel file);
}
