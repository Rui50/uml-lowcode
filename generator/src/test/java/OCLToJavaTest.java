import ocl.OCLToJavaVisitor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Testes de tradução OCL -> Java (OCLToJavaVisitor.toJava).
 * Cada caso fixa a tradução EXATA: protege contra regressões e documenta o subset suportado.
 * As traduções esperadas foram capturadas da própria ferramenta (transcritas, não inferidas).
 * self.<x> traduz SEMPRE para o getter (this.getX()), tal como o acesso a outros objetos.
 */
public class OCLToJavaTest {

    // expected = Java esperado; a mensagem do assert é a expressão OCL (para localizar falhas)
    private static void check(String ocl, String expectedJava) {
        assertEquals(expectedJava, OCLToJavaVisitor.toJava(ocl), ocl);
    }

    @Test
    void comparisonsAndEquality() {
        check("self.preco > 0", "this.getPreco() > 0");
        check("self.nome <> ''", "!java.util.Objects.equals(this.getNome(), \"\")");
        check("self.nome = 'x'", "java.util.Objects.equals(this.getNome(), \"x\")");
        check("self.estado = Estado::ATIVO", "java.util.Objects.equals(this.getEstado(), Estado.ATIVO)");
    }

    @Test
    void logicAndArithmetic() {
        check("self.a and self.b", "this.getA() && this.getB()");
        check("self.a or self.b", "this.getA() || this.getB()");
        check("self.premium implies self.desconto > 0", "!(this.getPremium()) || (this.getDesconto() > 0)");
        check("not self.ativo", "!this.getAtivo()");
        check("self.preco * 2 + 1", "this.getPreco() * 2 + 1");
        check("self.x mod 2 = 0", "java.util.Objects.equals(this.getX() % 2, 0)");
        // operadores lógicos desta sessão
        check("self.ativo xor self.suspenso", "this.getAtivo() ^ this.getSuspenso()");
        check("self.premium => self.desconto > 0", "!(this.getPremium()) || (this.getDesconto() > 0)");
    }

    @Test
    void collectionOps() {
        check("self.alunos->size() <= 30", "this.getAlunos().size() <= 30");
        check("self.alunos->isEmpty()", "this.getAlunos().isEmpty()");
        check("self.alunos->notEmpty()", "!this.getAlunos().isEmpty()");
        check("self.alunos->includes(self.x)", "this.getAlunos().contains(this.getX())");
        check("self.alunos->excludes(self.x)", "!this.getAlunos().contains(this.getX())");
        check("self.alunos->first()", "this.getAlunos().stream().findFirst().orElse(null)");
        check("self.alunos->last()", "this.getAlunos().stream().reduce((__a, __b) -> __b).orElse(null)");
        check("self.nums->sum() > 0", "this.getNums().stream().mapToInt(Integer::intValue).sum() > 0");
    }

    @Test
    void iterators() {
        check("self.alunos->forAll(a | a.numero > 0)",
                "this.getAlunos().stream().allMatch(a -> a.getNumero() > 0)");
        check("self.alunos->exists(a | a.ativo)",
                "this.getAlunos().stream().anyMatch(a -> a.getAtivo())");
        check("self.alunos->select(a | a.idade > 18)",
                "this.getAlunos().stream().filter(a -> a.getIdade() > 18).collect(java.util.stream.Collectors.toList())");
        check("self.alunos->reject(a | a.idade < 18)",
                "this.getAlunos().stream().filter(a -> !(a.getIdade() < 18)).collect(java.util.stream.Collectors.toList())");
        check("self.alunos->collect(a | a.nome)",
                "this.getAlunos().stream().map(a -> a.getNome()).collect(java.util.stream.Collectors.toList())");
    }

    @Test
    void conditional() {
        check("if self.a then self.b else self.c endif", "((this.getA()) ? (this.getB()) : (this.getC()))");
    }

    // ── adições desta sessão ──

    @Test
    void letBindings() {
        check("let dobro = self.preco * 2 in dobro >= self.stock", "(this.getPreco() * 2) >= this.getStock()");
        // binding sequencial: y usa x
        check("let x = self.preco, y = x + 1 in y > 0", "((this.getPreco()) + 1) > 0");
    }

    @Test
    void numericAggregates() {
        check("self.precos->max() > 0",
                "this.getPrecos().stream().max(java.util.Comparator.naturalOrder()).orElse(null) > 0");
        check("self.precos->min() >= self.minimo",
                "this.getPrecos().stream().min(java.util.Comparator.naturalOrder()).orElse(null) >= this.getMinimo()");
        check("self.preco->abs() >= 1", "Math.abs(this.getPreco()) >= 1");
        check("self.preco->floor() >= 1", "(long) Math.floor(this.getPreco()) >= 1");
        check("self.itens->count(self.alvo) = 1",
                "java.util.Objects.equals(java.util.Collections.frequency(this.getItens(), this.getAlvo()), 1)");
    }

    @Test
    void unsupportedOpsThrow() {
        // operações aceites pela gramática mas fora do subset suportado devem falhar
        // com IllegalArgumentException (apanhada pelo AnnotationService), nunca gerar Java inválido
        assertThrows(IllegalArgumentException.class,
                () -> OCLToJavaVisitor.toJava("self.turmas->union(self.antigas)->size() > 0"));
        assertThrows(IllegalArgumentException.class,
                () -> OCLToJavaVisitor.toJava("self.alunos->sortedBy(a | a.nome)"));
        // sintaxe inválida também falha (error listener)
        assertThrows(IllegalArgumentException.class,
                () -> OCLToJavaVisitor.toJava("self.preco >"));
    }

    @Test
    void stringOps() {
        check("self.nome->toUpperCase() = 'ABC'",
                "java.util.Objects.equals(this.getNome().toUpperCase(), \"ABC\")");
        check("self.codigo->toInteger() > 0", "Integer.parseInt(this.getCodigo()) > 0");
        check("self.valor->toReal() >= 1.5", "Double.parseDouble(this.getValor()) >= 1.5");
        check("self.flag->toBoolean()", "Boolean.parseBoolean(this.getFlag())");
        check("self.nome->characters()->size() <= 10",
                "java.util.Arrays.asList(this.getNome().split(\"\")).size() <= 10");
        check("self.nome->equalsIgnoreCase('admin')", "this.getNome().equalsIgnoreCase(\"admin\")");
    }
}
