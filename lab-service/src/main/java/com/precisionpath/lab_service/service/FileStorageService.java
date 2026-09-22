package com.precisionpath.lab_service.service;

import com.precisionpath.lab_service.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Stores prescriptions and reports on the local disk under app.storage.location.
 * Files are saved under random names; the original name is kept in the database.
 */
@Service
public class FileStorageService {

    public static final Set<String> DOCUMENT_TYPES = Set.of(
            "application/pdf", "image/jpeg", "image/png"
    );

    private static final Map<String, String> EXTENSIONS = Map.of(
            "application/pdf", ".pdf",
            "image/jpeg", ".jpg",
            "image/png", ".png"
    );

    private final Path root;

    public FileStorageService(@Value("${app.storage.location}") String location) {
        this.root = Path.of(location).toAbsolutePath().normalize();
    }

    public record StoredFile(
            String path,
            String originalName,
            String contentType,
            long size
    ) {
    }

    public StoredFile store(
            String folder,
            MultipartFile file,
            Set<String> allowedTypes
    ) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().toLowerCase(Locale.ROOT);

        if (!allowedTypes.contains(contentType)) {
            throw new IllegalArgumentException(
                    "Only PDF, JPG and PNG files are allowed"
            );
        }

        String originalName = StringUtils.hasText(file.getOriginalFilename())
                ? Path.of(StringUtils.cleanPath(file.getOriginalFilename())).getFileName().toString()
                : "document" + EXTENSIONS.get(contentType);

        String relativePath = folder + "/" + UUID.randomUUID() + EXTENSIONS.get(contentType);
        Path target = resolve(relativePath);

        try (InputStream input = file.getInputStream()) {
            Files.createDirectories(target.getParent());
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not store file", exception);
        }

        return new StoredFile(relativePath, originalName, contentType, file.getSize());
    }

    public Resource load(String relativePath) {

        try {
            Resource resource = new UrlResource(resolve(relativePath).toUri());

            if (!resource.exists()) {
                throw new ResourceNotFoundException(
                        "File not found"
                );
            }

            return resource;
        } catch (MalformedURLException exception) {
            throw new UncheckedIOException("Could not read file", exception);
        }
    }

    public void delete(String relativePath) {

        if (relativePath == null) {
            return;
        }

        try {
            Files.deleteIfExists(resolve(relativePath));
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not delete file", exception);
        }
    }

    private Path resolve(String relativePath) {

        Path path = root.resolve(relativePath).normalize();

        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("Invalid file path");
        }

        return path;
    }
}
