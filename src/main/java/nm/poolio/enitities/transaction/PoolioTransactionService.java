package nm.poolio.enitities.transaction;

import lombok.RequiredArgsConstructor;
import nm.poolio.data.User;
import nm.poolio.model.enums.Season;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PoolioTransactionService {
    private final PoolioTransactionRepository repository;

    public List<PoolioTransaction> findAllPoolioTransactions() {
        var result = repository.findBySeason(Season.getCurrent());
        result.sort(Comparator.comparing(PoolioTransaction::getCreatedDate).reversed());
        return result;
    }

    public List<PoolioTransaction> findAllPoolioTransactionsForUser(User user) {
        return repository.findByDebitUserOrCreditUserOrPayAsYouGoUserAndSeason(
                user, user, user, Season.getCurrent());
    }

    public PoolioTransaction save(PoolioTransaction t) {
        return repository.save(t);
    }

    public int getFunds(User user) {
        Integer debitsAmount = repository.calculateDebitAmount(user, Season.getCurrent());
        Integer creditAmount = repository.calculateCreditAmount(user, Season.getCurrent());

        return (debitsAmount == null ? 0 : debitsAmount) - (creditAmount == null ? 0 : creditAmount);
    }

}
