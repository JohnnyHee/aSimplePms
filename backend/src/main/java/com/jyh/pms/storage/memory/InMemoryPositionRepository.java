package com.jyh.pms.storage.memory;

import com.jyh.pms.domain.Position;
import com.jyh.pms.storage.PositionRepository;
import com.jyh.pms.storage.SortOption;
import com.jyh.pms.storage.support.AbstractDocumentRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 职位仓储的内存实现（{@code pms.storage=memory}，缺省生效）。
 */
@Component
@ConditionalOnProperty(name = "pms.storage", havingValue = "memory", matchIfMissing = true)
public class InMemoryPositionRepository extends AbstractDocumentRepository<Position> implements PositionRepository {

    public InMemoryPositionRepository(InMemoryDocumentRepository store) {
        super(store.forType(Position.class), Position.class);
    }

    @Override
    public Optional<Position> findByCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return findOne(null, p -> code.equalsIgnoreCase(p.getCode()));
    }

    @Override
    public boolean existsByCode(String code) {
        return findByCode(code).isPresent();
    }

    @Override
    public List<Position> findAllOrderBySortOrder() {
        return find(null, p -> true, SortOption.asc("sortOrder"));
    }
}
