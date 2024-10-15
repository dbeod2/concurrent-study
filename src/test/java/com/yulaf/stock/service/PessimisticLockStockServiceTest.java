package com.yulaf.stock.service;

import com.yulaf.stock.domain.Stock;
import com.yulaf.stock.respository.StockRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class PessimisticLockStockServiceTest {

    @Autowired
    private PessimisticLockStockService pessimisticLockStockService;

    @Autowired
    private StockRepository stockRepository;

    @BeforeEach
    public void before() {
        stockRepository.saveAndFlush(new Stock(1L, 100L));
    }

    @AfterEach
    public void after() {
        stockRepository.deleteAll();
    }

    @Test
    public void 동시에_100_요청() throws InterruptedException {
        int threadCount = 100;

        ExecutorService executorService = Executors.newFixedThreadPool(32); // 멀티스레드 사용

        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < 100; i++) {
            executorService.submit(() -> {
                try {
                    pessimisticLockStockService.decrease(1L, 1L);
                } finally {
                    countDownLatch.countDown();
                }
            });
        }

        countDownLatch.await();

        Stock stock = stockRepository.findById(1L).orElseThrow();
        assertEquals(0, stock.getQuantity());
    }
}
