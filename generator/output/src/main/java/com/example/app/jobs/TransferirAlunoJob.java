package com.example.app.jobs;

import com.example.app.models.Aluno;
import com.example.app.models.Turma;
import com.example.app.models.AlunoTE;
import com.example.app.models.Turno;
import com.example.app.models.testeClassYes;
import com.example.app.models.testeSubAb;
import com.example.app.models.ImplementInterface;
import com.example.app.models.EstadoAluno;
import com.example.app.repository.TurmaRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional
public class TransferirAlunoJob {

    private final TurmaRepository turmaRepository;

    @Scheduled(cron = "0 0 * * * *")
    public void run() {
        Turma turma = turmaRepository.findById(turmaId)
            .orElseThrow(() -> new RuntimeException("Turma not found"));
        Turma destino = turmaRepository.findById(destinoId)
            .orElseThrow(() -> new RuntimeException("Turma not found"));
        boolean sucesso = false;
        Aluno a = turma.getAlunos().get(numero);
        if (a != null) {
            turma.getAlunos().remove(a.getNumero());
            destino.getAlunos().put(a.getNumero(), a);
            sucesso = true;
        }
    }
}
