package com.biodex.upload;

import com.biodex.controller.upload.UploadController;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
import java.io.FileOutputStream;


// test for upload exceeding 10mb
public class UploadTest {
    private UploadController controller;

    @BeforeEach
    void setUp() {
        controller = new UploadController();
    }

    private File createTempFile(String name, long size) throws Exception {
        Path tempFile = Files.createTempFile(name, "");
        Files.write(tempFile, new byte[(int) size]);
        return tempFile.toFile();
    }

    @Test
    void EmptyImageList() {
        assertTrue(controller.selectedImages.isEmpty());
    }

    @Test
    void acceptsUnder10MB() throws Exception{
        File acceptedFile = createTempFile("acceptedPhoto.png", 2*1024*1024);
        boolean result = controller.uploadValidation(acceptedFile);
        assertTrue(result);
        assertEquals(1, controller.selectedImages.size());
    }

    @Test
    void rejectsOver10MB() throws Exception{
        File rejectedFile = createTempFile("rejectedPhoto.png", 11*1024*1024);
        boolean result = controller.uploadValidation(rejectedFile);

        assertFalse(result);
        assertTrue(controller.selectedImages.isEmpty());
    }

    @Test
    void dupePreventionTest() throws Exception{
        File file = createTempFile("dupeImage.png", 1024);
        controller.selectedImages.add(file);
        boolean result = controller.uploadValidation(file);
        assertFalse(result);
        assertEquals(1, controller.selectedImages.size());
    }

}
