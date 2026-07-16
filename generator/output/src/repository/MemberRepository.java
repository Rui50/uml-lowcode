package repository;

import models.Member;
import java.util.*;

public class MemberRepository {

    private Map<Long, Member> data = new HashMap<>();
    private Long nextId = 1L;

    public Member save(Member entity) {
        if (entity.getId() == null) {
            entity.setId(nextId++);
        }
        data.put(entity.getId(), entity);
        return entity;
    }

    public Member findById(Long id) {
        return data.get(id);
    }

    public List<Member> findAll() {
        return new ArrayList<>(data.values());
    }

    public boolean deleteById(Long id) {
        return data.remove(id) != null;
    }

    public boolean existsById(Long id) {
        return data.containsKey(id);
    }

}
