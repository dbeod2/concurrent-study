# 재고시스템으로 알아보는 동시성이슈 

## 1주차 
재고시스템에서 발생할 수 있는 동시성 이슈 해결방법에 대한 내용이다.

```java
public synchronized void decrease(Long id, Long quantity) {
	Stock stock = stockRepository.findById(id).orElseThrow();
	stock.decreaseQuantity(quantity);

	stockRepository.saveAndFlush(stock);
}
```

위 코드는 단건에 대해서는 성공을 한다.

하지만 100건 동시 요청시에 작업이 제대로 이루어 지지않고 race condition으로 인해 재고 감소가 제대로 일어나지 않는다.

race condition이란 두 개 이상 스레드가 공유 데이터 엑세스를 할 수 있는 문제이다.

이 문제를 해결하기 위해 하나의 스레드 작업이 끝날때까지 기다린 후 작업을 가능하게 해야한다.

**해결 방법**

synchronized를 이용할 수 있다.

synchronized를 사용하면 스레드 동기화를 할수 있고, 하나의 스레드만 특정 코드블록에 접근할 수 있으므로, 메서드가 실행 중일 때 다른 스레드의 접근을 막을 수 있다.

하지만, 서비스 로직에 `@Transactional` 어노테이션을 사용하고 있으면 작업 중 트랜잭션 종료 시점에 트랜잭션을 종료하게 되는데 쿼리 수행 작업 전 새로운 작업이 들어오면 값을 갱신 하기전에 이전 값을 가지고 가기때문에 이전과 동일한 문제가 발생 할 수있다.

→ 이 문제는 고립수준을 serializable로 설정하면 문제가 해결 될 것 같다. 하지만 성능이 중요한 시스템에서는 비효율적일수 있다고 한다.

**synchronized를 사용했을 때 문제점**

**synchronized는** 하나의 프로세스 안에서만 보장되어 **여러 스레드에서 동시에 데이터 접근**을 할 수있게된다. 그럼으로써 결국 race condition을 발생시킨다.

그래서 실제 운영중에서는 사용되지 않는다.

**synchronized를 사용하지 않고 MySql을 활용한 다양한 방법에 대해서 알아보자!**

3가지 lock을 거는 방법이 있다.

1. pessimistic Lock
    1. 실제로 데이터의 락을 걸어서 정합성을 맞추는 것을 말한다.
2. optimistic lock
    1. 실제로 Lock을 사용하지 않고 버전을 이용함으로써 정합성을 맞추는 방법이다.
3. Named Lock
    1. 이름을 가진 metaData locking이다
    2. 이름을 가진 lock을 획득하면 해제될 때 까지 다른 세션은 이 Lock을 획득할 수없도록 한다.

간단하게 알아보았고 위에 3가지 Lock을 거는 방법은 자세하게 알아볼 예정이다.