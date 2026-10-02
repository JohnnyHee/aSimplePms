package com.jyh.pms.storage.memory;

import com.jyh.pms.domain.User;
import com.jyh.pms.storage.UserRepository;
import com.jyh.pms.storage.support.AbstractDocumentRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 人员仓储的内存实现（{@code pms.storage=memory}，缺省生效）。
 *
 * <p>过滤逻辑使用 {@link java.util.function.Predicate} 在内存中完成，
 * 底层委托给 {@link InMemoryDocumentRepository}。
 */
@Component
@ConditionalOnProperty(name = "pms.storage", havingValue = "memory", matchIfMissing = true)
public class InMemoryUserRepository extends AbstractDocumentRepository<User> implements UserRepository {

    public InMemoryUserRepository(InMemoryDocumentRepository store) {
        super(store.forType(User.class), User.class);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return findOne(null, u -> username.equalsIgnoreCase(u.getUsername()));
    }

    @Override
    public boolean existsByUsername(String username) {
        return findByUsername(username).isPresent();
    }

    @Override
    public List<User> findByRoleId(String roleId) {
        if (roleId == null) {
            return List.of();
        }
        return find(null, u -> u.getRoleIds() != null && u.getRoleIds().contains(roleId));
    }

    @Override
    public long countByPositionId(String positionId) {
        if (positionId == null) {
            return 0L;
        }
        return count(null, u -> positionId.equals(u.getPositionId()));
    }
}
