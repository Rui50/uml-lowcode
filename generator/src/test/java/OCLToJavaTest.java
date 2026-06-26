import ocl.OCLToJavaVisitor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Testes de tradução OCL -> Java (OCLToJavaVisitor.toJava).
 * Cada caso fixa a tradução EXATA: protege contra regressões e documenta o subset suportado.
 * As traduções esperadas foram capturadas da própria ferramenta (transcritas, não inferidas).
 */
public class OCLToJavaTest {

    // expected = Java esperado; a mensagem do assert é a expressão OCL (para localizar falhas)
    private static void check(String ocl, String expectedJava) {
        assertEquals(expectedJava, OCLToJavaVisitor.toJava(ocl), ocl);
    }

    @Test
    void comparisonsAndEquality() {
        check("self.preco > 0", "this.preco > 0");
        check("self.nome <> ''", "!java.util.Objects.equals(this.nome, \"\")");
        check("self.nome = 'x'", "java.util.Objects.equals(this.nome, \"x\")");
        check("self.estado = Estado::ATIVO", "java.util.Objects.equals(this.estado, Estado.ATIVO)");
    }

    @Test
    void logicAndArithmetic() {
        check("self.a and self.b", "this.a && this.b");
        check("self.a or self.b", "this.a || this.b");
        check("self.premium implies self.desconto > 0", "!(this.premium) || (this.desconto > 0)");
        check("not self.ativo", "!this.ativo");
        check("self.preco * 2 + 1", "this.preco * 2 + 1");
        check("self.x mod 2 = 0", "java.util.Objects.equals(this.x % 2, 0)");
        // operadores lógicos desta sessão
        check("self.ativo xor self.suspenso", "this.ativo ^ this.suspenso");
        check("self.premium => self.desconto > 0", "!(this.premium) || (this.desconto > 0)");
    }

    @Test
    void collectionOps() {
        check("self.alunos->size() <= 30", "this.alunos.size() <= 30");
        check("self.alunos->isEmpty()", "this.alunos.isEmpty()");
        check("self.alunos->notEmpty()", "!this.alunos.isEmpty()");
        check("self.alunos->includes(self.x)", "this.alunos.contains(this.x)");
        check("self.alunos->excludes(self.x)", "!this.alunos.contains(this.x)");
        check("self.alunos->first()", "this.alunos.stream().findFirst().orElse(null)");
        check("self.alunos->last()", "this.alunos.stream().reduce((__a, __b) -> __b).orElse(null)");
        check("self.nums->sum() > 0", "this.nums.stream().mapToInt(Integer::intValue).sum() > 0");
    }

    @Test
    void iterators() {
        check("self.alunos->forAll(a | a.numero > 0)",
                "this.alunos.stream().allMatch(a -> a.getNumero() > 0)");
        check("self.alunos->exists(a | a.ativo)",
                "this.alunos.stream().anyMatch(a -> a.getAtivo())");
        check("self.alunos->select(a | a.idade > 18)",
                "this.alunos.stream().filter(a -> a.getIdade() > 18).collect(java.util.stream.Collectors.toList())");
        check("self.alunos->reject(a | a.idade < 18)",
                "this.alunos.stream().filter(a -> !(a.getIdade() < 18)).collect(java.util.stream.Collectors.toList())");
        check("self.alunos->collect(a | a.nome)",
                "this.alunos.stream().map(a -> a.getNome()).collect(java.util.stream.Collectors.toList())");
    }

    @Test
    void conditional() {
        check("if self.a then self.b else self.c endif", "((this.a) ? (this.b) : (this.c))");
    }

    // ── adições desta sessão ──

    @Test
    void letBindings() {
        check("let dobro = self.preco * 2 in dobro >= self.stock", "(this.preco * 2) >= this.stock");
        // binding sequencial: y usa x
        check("let x = self.preco, y = x + 1 in y > 0", "((this.preco) + 1) > 0");
    }

    @Test
    void numericAggregates() {
        check("self.precos->max() > 0",
                "this.precos.stream().max(java.util.Comparator.naturalOrder()).orElse(null) > 0");
        check("self.precos->min() >= self.minimo",
                "this.precos.stream().min(java.util.Comparator.naturalOrder()).orElse(null) >= this.minimo");
        check("self.preco->abs() >= 1", "Math.abs(this.preco) >= 1");
        check("self.preco->floor() >= 1", "(long) Math.floor(this.preco) >= 1");
        check("self.itens->count(self.alvo) = 1",
                "java.util.Objects.equals(java.util.Collections.frequency(this.itens, this.alvo), 1)");
    }

    @Test
    void stringOps() {
        check("self.nome->toUpperCase() = 'ABC'",
                "java.util.Objects.equals(this.nome.toUpperCase(), \"ABC\")");
        check("self.codigo->toInteger() > 0", "Integer.parseInt(this.codigo) > 0");
        check("self.valor->toReal() >= 1.5", "Double.parseDouble(this.valor) >= 1.5");
        check("self.flag->toBoolean()", "Boolean.parseBoolean(this.flag)");
        check("self.nome->characters()->size() <= 10",
                "java.util.Arrays.asList(this.nome.split(\"\")).size() <= 10");
        check("self.nome->equalsIgnoreCase('admin')", "this.nome.equalsIgnoreCase(\"admin\")");
    }
}
