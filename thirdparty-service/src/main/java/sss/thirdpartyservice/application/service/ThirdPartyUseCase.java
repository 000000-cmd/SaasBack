package sss.thirdpartyservice.application.service;

import com.saas.common.service.CodeCrudService;
import com.saas.common.service.GenericCrudService;
import com.saas.system.domain.model.Menu;
import com.saas.system.domain.port.out.IMenuRepositoryPort;
import org.springframework.stereotype.Service;
import sss.thirdpartyservice.domain.model.ThirdParty;
import sss.thirdpartyservice.domain.port.in.IThirdPartyUseCase;
import sss.thirdpartyservice.domain.port.out.IThirdPartyRepositoryPort;

import java.util.Optional;
import java.util.UUID;

@Service
public class ThirdPartyUseCase extends GenericCrudService<ThirdParty, UUID> implements IThirdPartyUseCase {

    private final IThirdPartyRepositoryPort repositoryPort;

    public ThirdPartyUseCase(IThirdPartyRepositoryPort repo) {
        super(repo);
        this.repositoryPort = repo;
    }

    @Override protected String getResourceName() { return "ThirdParty"; }

    @Override
    protected void applyChanges(ThirdParty existing, ThirdParty incoming) {

        if (incoming.getCode() != null)             existing.setCode(incoming.getCode());
        if (incoming.getType() != null)             existing.setType(incoming.getType());
        if (incoming.getDocumentTypeId() != null)   existing.setDocumentTypeId(incoming.getDocumentTypeId());
        if (incoming.getDocumentNumber() != null)   existing.setDocumentNumber(incoming.getDocumentNumber());
        if (incoming.getUserID() != null)           existing.setUserID(incoming.getUserID());
        if (incoming.getFirstName() != null)        existing.setFirstName(incoming.getFirstName());
        if (incoming.getSecondName() != null)       existing.setSecondName(incoming.getSecondName());
        if (incoming.getFirstLastName() != null)    existing.setFirstLastName(incoming.getFirstLastName());
        if (incoming.getSecondLastName() != null)   existing.setSecondLastName(incoming.getSecondLastName());
        if (incoming.getEmail() != null)            existing.setEmail(incoming.getEmail());
        if (incoming.getPhone() != null)            existing.setPhone(incoming.getPhone());
        if (incoming.getBusinessName() != null)     existing.setBusinessName(incoming.getBusinessName());
        if (incoming.getTradeName() != null)        existing.setTradeName(incoming.getTradeName());

        existing.setActive(incoming.isActive());
    }

    @Override
    public Optional<ThirdParty> findByDocument(String documentNumber) {
        return repositoryPort.findByDocument(documentNumber);
    }

    @Override
    public boolean existsDocument(String documentNumber) {
        return repositoryPort.existsByDocument(documentNumber);
    }
}
