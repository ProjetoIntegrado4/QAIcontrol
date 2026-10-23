package br.edu.exemplo.ia.repository.csv;

import br.edu.exemplo.ia.config.BaseDadosConfig;
import br.edu.exemplo.ia.repository.AdminRepository;
import br.edu.exemplo.ia.repository.EmpresaRepository;
import br.edu.exemplo.ia.repository.FuncionarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/** O Spring monta os repositórios em CSV com a pasta vinda de QSOFT_DADOS_DIR / qsoft.dados.dir. */
class BaseDadosContextoTest {

    @TempDir
    Path pasta;

    @Test
    void deveMontarRepositoriosComAPastaConfigurada() {
        new ApplicationContextRunner()
                .withUserConfiguration(BaseDadosConfig.class, BaseDadosCsv.class, EmpresaCsvRepository.class,
                        AdminCsvRepository.class, FuncionarioCsvRepository.class)
                .withPropertyValues("qsoft.dados.dir=" + pasta)
                .run(ctx -> {
                    assertEquals(pasta, ctx.getBean(BaseDadosCsv.class).pasta());
                    assertInstanceOf(EmpresaCsvRepository.class, ctx.getBean(EmpresaRepository.class));
                    assertInstanceOf(AdminCsvRepository.class, ctx.getBean(AdminRepository.class));
                    assertEquals(0, ctx.getBean(FuncionarioRepository.class).findAll().size());
                });
    }

    @Test
    void semConfiguracaoUsaAPastaPadrao() {
        new ApplicationContextRunner()
                .withUserConfiguration(BaseDadosConfig.class, BaseDadosCsv.class)
                .run(ctx -> assertEquals(Path.of("dados/csv"), ctx.getBean(BaseDadosCsv.class).pasta()));
    }
}
