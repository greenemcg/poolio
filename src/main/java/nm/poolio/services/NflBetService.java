package nm.poolio.services;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.executable.ValidateOnExecution;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nm.poolio.data.User;
import nm.poolio.enitities.bet.GameBet;
import nm.poolio.enitities.transaction.PoolioTransaction;
import nm.poolio.enitities.transaction.PoolioTransactionService;
import nm.poolio.enitities.transaction.PoolioTransactionType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Slf4j
@Service
public class NflBetService {
    @Getter
    @Value("${poolio.bet.banker:Bet Banker}")
    private String betBanker;

   private final PoolioTransactionService poolioTransactionService;
   private final UserService userService;


    @Transactional
    public PoolioTransaction createAcceptProposalTransaction(@NotNull User player, @NotNull GameBet gameBet, @NotNull Integer betAmount) {

        var banker = userService.findByUserName(getBetBanker());

        PoolioTransaction transaction = new PoolioTransaction();
        transaction.setDebitUser(player);
        transaction.setCreditUser(gameBet.getProposer());
        transaction.setAmount(betAmount);
        transaction.setType(PoolioTransactionType.ACCEPT_PROPOSAL);

        transaction.setGameBet(gameBet);
        return poolioTransactionService.save(transaction);


    }

}
