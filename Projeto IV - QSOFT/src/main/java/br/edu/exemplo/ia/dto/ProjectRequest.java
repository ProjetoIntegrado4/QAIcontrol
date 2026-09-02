package br.edu.exemplo.ia.dto;
import jakarta.validation.constraints.NotBlank;
public record ProjectRequest(@NotBlank String name,@NotBlank String area){}