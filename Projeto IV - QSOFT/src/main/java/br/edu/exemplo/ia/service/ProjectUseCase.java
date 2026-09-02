package br.edu.exemplo.ia.service;
import br.edu.exemplo.ia.dto.*;
import java.util.*;

public interface ProjectUseCase {
    ProjectResponse create(ProjectRequest r);
    List<ProjectResponse> list(); boolean exists(UUID id);
}