package com.yulaf.stock.service;

import com.yulaf.stock.domain.Stock;
import com.yulaf.stock.respository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OptimisticLockStockService {

    private final StockRepository stockRepository;

    public OptimisticLockStockService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Transactional
    public void decrease(Long id, Long quantity) {
        Stock stock = stockRepository.findByIdWithOptimisticLock(id);
        stock.decreaseQuantity(quantity);

        stockRepository.save(stock);
    }
}
