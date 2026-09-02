package br.edu.exemplo.ia;
import org.junit.jupiter.api.Test; import org.springframework.beans.factory.annotation.Autowired; import org.springframework.boot.test.context.SpringBootTest; import org.springframework.boot.test.web.client.TestRestTemplate; import org.springframework.boot.test.web.server.LocalServerPort; import org.springframework.test.context.DynamicPropertyRegistry; import org.springframework.test.context.DynamicPropertySource; import org.testcontainers.containers.PostgreSQLContainer; import org.testcontainers.junit.jupiter.Container; import org.testcontainers.junit.jupiter.Testcontainers; import static org.junit.jupiter.api.Assertions.*; import java.util.Map;
@Testcontainers @SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT) class ProjectIntegrationTest {
 @Container static PostgreSQLContainer<?> pg=new PostgreSQLContainer<>("postgres:17-alpine"); @DynamicPropertySource static void props(DynamicPropertyRegistry r){r.add("spring.datasource.url",pg::getJdbcUrl);r.add("spring.datasource.username",pg::getUsername);r.add("spring.datasource.password",pg::getPassword);} @LocalServerPort int port; @Autowired TestRestTemplate rest;
 @Test void deveCriarProjeto_AAA(){ // Arrange
  var body=Map.of("name","Tutor IA","area","Educação"); // Act
  var resp=rest.postForEntity("http://localhost:"+port+"/api/projects",body,String.class); // Assert
  assertTrue(resp.getStatusCode().is2xxSuccessful()); assertTrue(resp.getBody().contains("Tutor IA")); }
}
