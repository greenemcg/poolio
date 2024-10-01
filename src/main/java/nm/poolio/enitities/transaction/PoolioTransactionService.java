package nm.poolio.enitities.transaction;

import lombok.RequiredArgsConstructor;
import nm.poolio.data.User;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PoolioTransactionService {
    private final PoolioTransactionRepository repository;

    public List<PoolioTransaction> findAllPoolioTransactions() {
        var result = repository.findAll();
        result.sort(Comparator.comparing(PoolioTransaction::getCreatedDate).reversed());
        return result;
    }

    public List<PoolioTransaction> findAllPoolioTransactionsForUser(User user) {
        return repository.findByDebitUserOrCreditUserOrPayAsYouGoUser(
                user, user, user);
    }

    public PoolioTransaction save(PoolioTransaction t) {
        return repository.save(t);
    }

    public int getFunds(User user) {
        Integer debitsAmount = repository.calculateDebitAmount(user);
        Integer creditAmount = repository.calculateCreditAmount(user);

        return (debitsAmount == null ? 0 : debitsAmount) - (creditAmount == null ? 0 : creditAmount);
    }

}
