package com.jyh.pms.storage.memory;

import com.jyh.pms.domain.Role;
import com.jyh.pms.storage.DocumentRepository;
import com.jyh.pms.storage.RoleRepository;
import com.jyh.pms.storage.support.AbstractDocumentRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 角色仓储的内存实现（{@code pms.storage=memory}，缺省生效）。
 *
 * <p>批量编码查询用 {@link Set} 判断，避免在谓词里做线性查找。
 */
@Component
@ConditionalOnProperty(name = "pms.storage", havingValue = "memory", matchIfMissing = true)
public class InMemoryRoleRepository extends AbstractDocumentRepository<Role> implements RoleRepository {

    public InMemoryRoleRepository(InMemoryDocumentRepository store) {
        super(store.forType(Role.class), Role.class);
    }

    @Override
    public Optional<Role> findByCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return findOne(null, r -> code.equalsIgnoreCase(r.getCode()));
    }

    @Override
    public boolean existsByCode(String code) {
        return findByCode(code).isPresent();
    }

    @Override
    public List<Role> findByCodeIn(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }
        Set<String> wanted = new HashSet<>(codes);
        return find(null, r -> r.getCode() != null && wanted.contains(r.getCode()));
    }
}
