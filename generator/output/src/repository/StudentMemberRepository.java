package repository;

import models.StudentMember;
import models.Member;
import java.util.*;
import java.util.stream.Collectors;

public class StudentMemberRepository {

    private final MemberRepository root;

    public StudentMemberRepository(MemberRepository root) {
        this.root = root;
    }

    public StudentMember save(StudentMember entity) {
        root.save(entity);
        return entity;
    }

    public StudentMember findById(Long id) {
        Member found = root.findById(id);
        return (found instanceof StudentMember) ? (StudentMember) found : null;
    }

    public List<StudentMember> findAll() {
        return root.findAll().stream()
                .filter(e -> e instanceof StudentMember)
                .map(e -> (StudentMember) e)
                .collect(Collectors.toList());
    }

    public boolean deleteById(Long id) {
        return findById(id) != null && root.deleteById(id);
    }

    public boolean existsById(Long id) {
        return findById(id) != null;
    }

}
