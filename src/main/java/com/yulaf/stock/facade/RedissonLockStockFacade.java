package com.yulaf.stock.facade;

import com.yulaf.stock.service.StockService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RedissonLockStockFacade {

    private final RedissonClient redissonClient;

    private final StockService stockService;

    public RedissonLockStockFacade(RedissonClient redissonClient, StockService stockService) {
        this.redissonClient = redissonClient;
        this.stockService = stockService;
    }

    public void decrease(Long id, Long quantity) {

        RLock lock = redissonClient.getLock(id.toString()); // lock 객체를 가지고온다.

        try {
            boolean available = lock.tryLock(15, 1, TimeUnit.SECONDS); // Lock을 몇초동안 점유할것인지 설정하고 Lock을 획득한다.

            if (!available) {
                System.out.println("lock 획득 실패"); // lock 획득 실패
                return;
            }
            stockService.decreaseNamedLockStock(id, quantity);
            
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }
}
