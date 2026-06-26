package sss.thirdpartyservice.infrastructure.controller;


import org.springframework.web.bind.annotation.*;
import sss.thirdpartyservice.application.dto.CreateThirdPartyRequest;
import sss.thirdpartyservice.domain.port.in.IThirdPartyUseCase;

@RestController
@RequestMapping("/third-parties")

public class ThirdPartyController {

    private final IThirdPartyUseCase useCase;

    public ThirdPartyController(IThirdPartyUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    public void create(
            @RequestBody CreateThirdPartyRequest request
    ) {

    }

    @GetMapping("/document/{documentNumber}")
    public boolean existsDocument(
            @PathVariable String documentNumber
    ) {
        return useCase.existsDocument(documentNumber);
    }
}