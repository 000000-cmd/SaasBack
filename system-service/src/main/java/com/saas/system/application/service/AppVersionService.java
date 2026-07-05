package com.saas.system.application.service;

import com.saas.common.exception.BusinessException;
import com.saas.common.exception.ResourceNotFoundException;
import com.saas.common.service.GenericCrudService;
import com.saas.system.domain.model.AppVersion;
import com.saas.system.domain.model.Constant;
import com.saas.system.domain.port.in.IAppVersionUseCase;
import com.saas.system.domain.port.in.IConstantUseCase;
import com.saas.system.domain.port.out.IAppVersionRepositoryPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Distribución del APK. El binario vive en disco ({@code app.apk.storage-dir});
 * la metadata (histórico, checksum, vigente) en BD.
 *
 * <p>Reglas de publicación: una sola vigente; {@code versionCode} estrictamente
 * creciente (Android solo actualiza en sitio con code mayor y la misma firma —
 * así la data local del usuario se conserva). Publicar sincroniza la constante
 * {@code VERAPP}, que es el contrato de exigencia de versión del APK.</p>
 */
@Service
@Slf4j
public class AppVersionService extends GenericCrudService<AppVersion, UUID> implements IAppVersionUseCase {

    private static final Pattern SEMVER = Pattern.compile("^\\d+\\.\\d+\\.\\d+$");
    private static final String VERAPP_CODE = "VERAPP";

    private final IAppVersionRepositoryPort repo;
    private final IConstantUseCase constantUseCase;
    private final Path storageDir;

    public AppVersionService(IAppVersionRepositoryPort repo,
                             IConstantUseCase constantUseCase,
                             @Value("${app.apk.storage-dir:./storage/apk}") String storageDir) {
        super(repo);
        this.repo = repo;
        this.constantUseCase = constantUseCase;
        this.storageDir = Path.of(storageDir).toAbsolutePath().normalize();
    }

    @Override protected String getResourceName() { return "Version del APK"; }

    /** Entidad de administración: cada cambio queda auditado siempre. */
    @Override protected boolean alwaysAudit() { return true; }

    @Override
    protected void applyChanges(AppVersion existing, AppVersion incoming) {
        // Version, code y binario son inmutables; solo las notas se editan.
        if (incoming.getNotes() != null) existing.setNotes(incoming.getNotes());
    }

    @Override
    @Transactional
    public AppVersion upload(String version, Integer versionCode, String notes,
                             boolean publish, InputStream content) {
        if (version == null || !SEMVER.matcher(version).matches()) {
            throw new BusinessException("La version debe tener formato x.y.z (p.ej. 1.0.1)");
        }
        if (versionCode == null || versionCode <= 0) {
            throw new BusinessException("El versionCode debe ser un entero positivo");
        }
        if (repo.findByVersion(version).isPresent()) {
            throw new BusinessException("Ya existe una version " + version);
        }
        if (repo.existsByVersionCode(versionCode)) {
            throw new BusinessException("Ya existe una version con versionCode " + versionCode);
        }

        String fileName = "saas-app-" + version + ".apk";
        StoredFile stored = store(content, fileName);
        if (stored.sizeBytes() == 0) {
            throw new BusinessException("El archivo del APK llego vacio");
        }

        AppVersion created = create(AppVersion.builder()
                .version(version)
                .versionCode(versionCode)
                .fileName(fileName)
                .checksum(stored.checksum())
                .sizeBytes(stored.sizeBytes())
                .notes(notes)
                .isCurrent(false)
                .build());

        return publish ? publish(created.getId()) : created;
    }

    @Override
    @Transactional
    public AppVersion publish(UUID id) {
        AppVersion target = getById(id);
        Optional<AppVersion> current = repo.findCurrent();

        if (current.isPresent() && !current.get().getId().equals(id)
                && target.getVersionCode() <= current.get().getVersionCode()) {
            throw new BusinessException(
                    "El versionCode (" + target.getVersionCode() + ") debe ser mayor al vigente ("
                            + current.get().getVersionCode() + ") para que los telefonos actualicen en sitio");
        }

        current.filter(c -> !c.getId().equals(id)).ifPresent(c -> {
            c.setIsCurrent(false);
            repo.update(c);
        });
        target.setIsCurrent(true);
        AppVersion published = repo.update(target);

        syncVerapp(published.getVersion());
        log.info("Version del APK publicada: {} (code {})", published.getVersion(), published.getVersionCode());
        return published;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AppVersion> findCurrent() {
        return repo.findCurrent();
    }

    @Override
    public Path binaryPath(AppVersion version) {
        Path path = storageDir.resolve(version.getFileName()).normalize();
        if (!path.startsWith(storageDir) || !Files.exists(path)) {
            throw new ResourceNotFoundException(getResourceName(), "archivo", version.getFileName());
        }
        return path;
    }

    /**
     * La vigente no se elimina: primero publicar otra. El borrado es FÍSICO
     * (registro + binario): Version/VersionCode son UNIQUE y un soft-delete
     * dejaría fantasmas que impiden re-subir esa versión.
     */
    @Override
    @Transactional
    public void delete(UUID id) {
        AppVersion target = getById(id);
        if (Boolean.TRUE.equals(target.getIsCurrent())) {
            throw new BusinessException("No se puede eliminar la version vigente; publica otra primero");
        }
        repo.hardDeleteById(id);
        try {
            Files.deleteIfExists(storageDir.resolve(target.getFileName()).normalize());
        } catch (IOException e) {
            log.warn("Registro eliminado pero no se pudo borrar el binario {}: {}", target.getFileName(), e.getMessage());
        }
    }

    // ---------------------------------------------------------------

    /** El contrato de exigencia del APK es la constante VERAPP: publicar la sincroniza. */
    private void syncVerapp(String version) {
        try {
            Constant verapp = constantUseCase.getByCode(VERAPP_CODE);
            verapp.setValue(version);
            constantUseCase.update(verapp.getId(), verapp);
        } catch (ResourceNotFoundException ex) {
            constantUseCase.create(Constant.builder()
                    .code(VERAPP_CODE)
                    .name("Version vigente del APK")
                    .value(version)
                    .description("Si la version instalada difiere, el APK exige actualizar")
                    .build());
        }
    }

    /** Escribe el binario en streaming calculando el SHA-256 en el mismo pase. */
    private StoredFile store(InputStream content, String fileName) {
        try {
            Files.createDirectories(storageDir);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            Path target = storageDir.resolve(fileName);
            long size;
            try (DigestInputStream in = new DigestInputStream(content, digest)) {
                size = Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return new StoredFile(HexFormat.of().formatHex(digest.digest()), size);
        } catch (IOException e) {
            throw new BusinessException("No se pudo guardar el APK: " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    private record StoredFile(String checksum, long sizeBytes) {}
}
