package com.yulaf.stock.service;

import com.yulaf.stock.domain.Stock;
import com.yulaf.stock.respository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockService {

    private final StockRepository stockRepository;

    public StockService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

//    @Transactional
    public synchronized void decrease(Long id, Long quantity) {
        // stock id와 감소시킬 stock 재고수
        // stock 조회
        // 재고 감소
        // 갱신된 값을 저장한다.

        Stock stock = stockRepository.findById(id).orElseThrow();
        stock.decreaseQuantity(quantity);

        stockRepository.saveAndFlush(stock);
    }
}
