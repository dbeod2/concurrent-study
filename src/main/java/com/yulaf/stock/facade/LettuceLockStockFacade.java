package com.yulaf.stock.facade;

import com.yulaf.stock.respository.RedisLockRepository;
import com.yulaf.stock.service.StockService;
import org.springframework.stereotype.Component;

@Component
public class LettuceLockStockFacade {

    private final RedisLockRepository redisLockRepository;
    private final StockService stockService;

    public LettuceLockStockFacade(RedisLockRepository redisLockRepository, StockService stockService) {
        this.redisLockRepository = redisLockRepository;
        this.stockService = stockService;
    }

    public void decrease(Long id, Long quantity) throws InterruptedException {
        while(!redisLockRepository.lock(id)) {
            Thread.sleep(100);
        }

        try {
            stockService.decreaseNamedLockStock(id, quantity);
        } finally {
            redisLockRepository.unlock(id);
        }
    }
}
