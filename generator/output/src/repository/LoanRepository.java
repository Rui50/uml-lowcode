package repository;

import models.Loan;
import java.util.*;

public class LoanRepository {

    private Map<Long, Loan> data = new HashMap<>();
    private Long nextId = 1L;

    public Loan save(Loan entity) {
        if (entity.getId() == null) {
            entity.setId(nextId++);
        }
        data.put(entity.getId(), entity);
        return entity;
    }

    public Loan findById(Long id) {
        return data.get(id);
    }

    public List<Loan> findAll() {
        return new ArrayList<>(data.values());
    }

    public boolean deleteById(Long id) {
        return data.remove(id) != null;
    }

    public boolean existsById(Long id) {
        return data.containsKey(id);
    }

}
