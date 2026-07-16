package repository;

import models.StaffMember;
import models.Member;
import java.util.*;
import java.util.stream.Collectors;

public class StaffMemberRepository {

    private final MemberRepository root;

    public StaffMemberRepository(MemberRepository root) {
        this.root = root;
    }

    public StaffMember save(StaffMember entity) {
        root.save(entity);
        return entity;
    }

    public StaffMember findById(Long id) {
        Member found = root.findById(id);
        return (found instanceof StaffMember) ? (StaffMember) found : null;
    }

    public List<StaffMember> findAll() {
        return root.findAll().stream()
                .filter(e -> e instanceof StaffMember)
                .map(e -> (StaffMember) e)
                .collect(Collectors.toList());
    }

    public boolean deleteById(Long id) {
        return findById(id) != null && root.deleteById(id);
    }

    public boolean existsById(Long id) {
        return findById(id) != null;
    }

}
