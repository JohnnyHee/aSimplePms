package com.jyh.pms.storage.memory;

import com.jyh.pms.domain.Salary;
import com.jyh.pms.domain.SalaryStatus;
import com.jyh.pms.storage.DocumentRepository;
import com.jyh.pms.storage.SalaryRepository;
import com.jyh.pms.storage.SortOption;
import com.jyh.pms.storage.support.AbstractDocumentRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 薪资仓储的内存实现（{@code pms.storage=memory}，缺省生效）。
 */
@Component
@ConditionalOnProperty(name = "pms.storage", havingValue = "memory", matchIfMissing = true)
public class InMemorySalaryRepository extends AbstractDocumentRepository<Salary> implements SalaryRepository {

    public InMemorySalaryRepository(InMemoryDocumentRepository store) {
        super(store.forType(Salary.class), Salary.class);
    }

    @Override
    public List<Salary> findByUidOrderByEffectiveFromDesc(String uid) {
        if (uid == null) {
            return List.of();
        }
        return find(null, s -> uid.equals(s.getUid()), SortOption.desc("effectiveFrom"));
    }

    @Override
    public Optional<Salary> findActiveByUid(String uid) {
        if (uid == null) {
            return Optional.empty();
        }
        return findOne(null, s -> uid.equals(s.getUid()) && SalaryStatus.ACTIVE == s.getStatus());
    }

    @Override
    public long countByUid(String uid) {
        if (uid == null) {
            return 0L;
        }
        return count(null, s -> uid.equals(s.getUid()));
    }

    @Override
    public long countByPositionId(String positionId) {
        if (positionId == null) {
            return 0L;
        }
        return count(null, s -> positionId.equals(s.getPositionId()));
    }
}
