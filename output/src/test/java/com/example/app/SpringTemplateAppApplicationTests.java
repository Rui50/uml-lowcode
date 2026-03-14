package com.example.app;

import com.example.app.models.Aluno;
import com.example.app.models.Turno;
import com.example.app.repository.AlunoRepository;
import com.example.app.repository.TurnoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SpringTemplateAppApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private AlunoRepository alunoRepository;

	@Autowired
	private TurnoRepository turnoRepository;

	@BeforeEach
	void cleanDatabase() {
		alunoRepository.deleteAll();
		turnoRepository.deleteAll();
	}

	@Test
	void contextLoads() {
	}

	@Test
	void addAlunosAssignsAlunoToTurno() throws Exception {
		Turno turno = turnoRepository.save(Turno.builder().capacidade(10).build());
		Aluno aluno = alunoRepository.save(Aluno.builder()
			.nome("Alice")
			.email("alice@example.com")
			.numero(1)
			.build());

		mockMvc.perform(post("/api/Turnos/{id}/Alunos", turno.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsBytes(List.of(aluno.getId()))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.alunos[0].id").value(aluno.getId()))
			.andExpect(jsonPath("$.alunos[0].turno.id").value(turno.getId()));

		mockMvc.perform(get("/api/Turnos/{id}", turno.getId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.alunos[0].id").value(aluno.getId()));

		mockMvc.perform(get("/api/Alunos/{id}", aluno.getId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.turno.id").value(turno.getId()));
	}

	@Test
	void addAlunosReassignsAlunoFromPreviousTurno() throws Exception {
		Turno firstTurno = turnoRepository.save(Turno.builder().capacidade(10).build());
		Turno secondTurno = turnoRepository.save(Turno.builder().capacidade(20).build());
		Aluno aluno = alunoRepository.save(Aluno.builder()
			.nome("Bob")
			.email("bob@example.com")
			.numero(2)
			.turno(firstTurno)
			.build());

		mockMvc.perform(post("/api/Turnos/{id}/Alunos", secondTurno.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsBytes(List.of(aluno.getId()))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.alunos[0].id").value(aluno.getId()));

		mockMvc.perform(get("/api/Turnos/{id}", firstTurno.getId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.alunos").isEmpty());

		mockMvc.perform(get("/api/Turnos/{id}", secondTurno.getId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.alunos[0].id").value(aluno.getId()));

		mockMvc.perform(get("/api/Alunos/{id}", aluno.getId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.turno.id").value(secondTurno.getId()));
	}

	@Test
	void removeAlunosClearsTurnoAssociation() throws Exception {
		Turno turno = turnoRepository.save(Turno.builder().capacidade(10).build());
		Aluno aluno = alunoRepository.save(Aluno.builder()
			.nome("Carol")
			.email("carol@example.com")
			.numero(3)
			.turno(turno)
			.build());

		mockMvc.perform(delete("/api/Turnos/{id}/alunos", turno.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsBytes(List.of(aluno.getId()))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.alunos").isEmpty());

		mockMvc.perform(get("/api/Turnos/{id}", turno.getId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.alunos").isEmpty());

		mockMvc.perform(get("/api/Alunos/{id}", aluno.getId()))
			.andExpect(status().isOk())
			.andExpect(content().json("""
				{"id":%d,"turno":null}
				""".formatted(aluno.getId()), false));
	}

	@Test
	void addAlunosReturnsNotFoundWhenTurnoDoesNotExist() throws Exception {
		Aluno aluno = alunoRepository.save(Aluno.builder()
			.nome("Dana")
			.email("dana@example.com")
			.numero(4)
			.build());

		mockMvc.perform(post("/api/Turnos/{id}/Alunos", 999L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsBytes(List.of(aluno.getId()))))
			.andExpect(status().isNotFound())
			.andExpect(content().string("Turno not found: 999"));
	}

	@Test
	void addAlunosReturnsNotFoundWhenAlunoDoesNotExist() throws Exception {
		Turno turno = turnoRepository.save(Turno.builder().capacidade(10).build());

		mockMvc.perform(post("/api/Turnos/{id}/Alunos", turno.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsBytes(List.of(12345L))))
			.andExpect(status().isNotFound())
			.andExpect(content().string("Aluno not found: [12345]"));
	}

	@Test
	void addAlunosIsIdempotentForDuplicateIds() throws Exception {
		Turno turno = turnoRepository.save(Turno.builder().capacidade(10).build());
		Aluno aluno = alunoRepository.save(Aluno.builder()
			.nome("Eve")
			.email("eve@example.com")
			.numero(5)
			.build());

		mockMvc.perform(post("/api/Turnos/{id}/Alunos", turno.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsBytes(List.of(aluno.getId(), aluno.getId()))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.alunos.length()").value(1))
			.andExpect(jsonPath("$.alunos[0].id").value(aluno.getId()));
	}

}
