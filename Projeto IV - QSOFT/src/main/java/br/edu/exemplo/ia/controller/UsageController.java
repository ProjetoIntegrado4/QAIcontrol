package br.edu.exemplo.ia.controller; import br.edu.exemplo.ia.dto.*;
import br.edu.exemplo.ia.service.UsageUseCase;
import br.edu.exemplo.ia.dto.UsageRequest;
import br.edu.exemplo.ia.dto.UsageResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController

@RequestMapping("/api/usages")
public class UsageController {
    private final UsageUseCase useCase;
    public UsageController(UsageUseCase useCase){
        this.useCase=useCase;
    }

    @PostMapping
    public UsageResponse register(@Valid @RequestBody UsageRequest r){
        return useCase.register(r);
    }
}