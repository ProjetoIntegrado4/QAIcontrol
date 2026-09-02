package br.edu.exemplo.ia.controller;
import br.edu.exemplo.ia.dto.*;
import br.edu.exemplo.ia.service.ProjectUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import br.edu.exemplo.ia.dto.*;


@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectUseCase useCase;
    public ProjectController(ProjectUseCase useCase){this.useCase=useCase;
    }
    @PostMapping
    public ProjectResponse create(@Valid @RequestBody ProjectRequest r){
        return useCase.create(r);
    }
    @GetMapping
    public List<ProjectResponse> list(){
        return useCase.list();}
}
