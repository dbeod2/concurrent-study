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

## 2주차
### Perssimistic Lock (비관적 락/선점 잠금)

- 모든 트랜잭션은 충돌이 발생한다고 가정하고 우선 Lock을 거는 방법이다.
- DB에서 제공하는 Lock 기능을 사용한다.
- JPA를 사용하면 메서드에 `@Lock`  어노테이션과 모드를 설정할 수 있다.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select s from Stock s where s.id =: id")
Stock findById(Long id);
```

비관적 락을 사용하면 `select ... for update` 쿼리가 발생되는데 이 때 **gap lock**을 사용하게된다.

- ***select … for update는 동시성 제어를 위해 특정 row에 배타적 Lock을 거는 행위이다.
  쉽게말하면 락을 걸고 값을 가지고 오는 것이다.***

**장점**

- 충돌이 빈번하게 일어나면 Optimistic Lock보다 성능이 좋을 수 있고, Lock을 통해서 업데이트를 제어해서 데이터 정합성이 보장된다.

**단점**

- 별도 Lock을 잡고 있어서 성능 감소가 있을 수 있다.

### **Optimistic Lock (낙관적 락 / 비선점 잠금)**

- 대부분의 트랜잭션이 충돌하지 않는다고 가정하는 방법이다.
- 읽은 버전에서 수정사항이 생겼을 때 다시 데이터를 읽어와서 작업을 수행해야한다.
- DB의 Lock 기능을 이용하지 않고, JPA가 제공하는 버전 관리 기능을 사용한다.

```java
@Entity
public class Stock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Version 
    private Long version;
}

public interface StockRepository extends JpaRepository<Stock, Long> {
		@Lock(value = LockModeType.OPTIMISTIC)
    @Query("select s from Stock s where s.id = :id")
    Stock findByIdWithOptimisticLock(Long id);
}

```

**장점**

- 별도의 Lock을 잡지 않아서 Perssimistic Lock 보다 성능이 좋다

**단점**

- 최대 단점은 롤백이다!  **충돌이 났다고 한다면 이를 해결하려면 개발자가 수동으로 롤백처리를해야해서**실패시 재시도 요청이 필요하여 직접 로직을 작성해야하는 번거로움이 있다.

### Named Lock

- DB의 Lock 기능을 이용하지 않고, 이름을 가진 Lock을 생성해서 획득하는 방법이다.
- Lock을 획득하고 해제할 때 까지 다른 세션은 해당 Lock을 가질 수없다.

```java
public interface LockRepository extends JpaRepository<Stock, Long> {

    @Query(value = "select get_lock(:key, 3000)", nativeQuery = true)
    void getLock(String key);

    @Query(value = "select release_lock(:key)", nativeQuery = true)
    void releaseLock(String key);
}
```

락 획득(get_lock)과 락 반납(release_lock) 로직을 구현해야한다.

### GET_LOCK

- 입력 받은 이름(key)으로 timeout초 동안 잠금 획득을 시도한다.
- 한 session에서 잠금을 유지하고 있는 동안 다른 session에서 동일한 이름의 잠금을 획득할 수 없다.
- 획득한 Lock은 트랜잭션이 커밋되거나 롤백되어도 해제되지 않는다. 별도의 명령어로 해제하거나 Lock 선점 시간이 끝나야 해제가 된다.

### RELEASE_LOCK

- 입력 받은 이름(key)의 잠금을 해제한다.
- 스레드가 종료된다고 락이 자동으로 해제되지 않아서 반드시 해제시켜야한다.

**장점**

- Named Lock은 Redis를 사용하기 위한 비용을 발생하지 않고, MySQL을 사용해 분산 락을 구현할 수 있다. MySQL 에서는 getLock()을 통해 획득, releaseLock()으로 해지할 수 있다.

**단점**

- 트랜잭션 종료시 세션관리를 해줘야하므로 주의해서 사용해야한다.
- 락을 획득하는 동안 커넥션을 지속적으로 사용하므로, 여러 트랜잭션이 동시에 락을 시도할 때 **Connection Pool**이 부족해질 수 있으므로, 성능 저하나 연결 문제를 일으킬 수있다.

여기까지 MySql로 동시성 문제를 해결하는 방법을 알아봤다.

3개의 Lock 중 상황에 맞게 사용하는게 맞다고 생각이 든다.

---

이어서 Redis를 활용해서 동시성을 해결하는 방법도 있다고 한다.

간단하게 설명으로 끝내려고한다!

redis에서 제공하는 분산 락 라이브러리 **Lettuce, Redisson 이** 두 가지를 통해 해결하는 방법을 알아본다.

### Lettuce

- key, value를 set할 때 기존 값이 없을때 set해주는 spin lock 방식이며, 재시도 로직을 구현해야한다.
   - ***spin lock이란? lock을 사용할 수 잇는지 Lock 획득을 시도하는 방식이다.***

**장점**

- Redis를 이용하고 있어서 Session관리에 신경을 쓰지 않아도 된다는 점

**단점**

- spin lock방식이라 Lock이 해제되었는지 주기적으로 재시도를 해야해서 redis에 부하를 줄 수있다.
   - threed.sleep()을 통해 Lock 재시도 간에 텀을 둬야한다.

### Redisson

- pub-sub 기반 Lock이라고 한다. redis에서는 분산락을 구현할 때 pub/sub패턴을 활용한다.
- Lock을 점유 중인 스레드가 작업을 완료하고 Lock을 해제하면 Redisson은 이 정보를 채널을 통해 대기중인 스레드에게 전송하고, 메시지를 받고 다시 Lock획득을 시도하는 방식이다.

**장점**

- Lock 해제가 되었을 때 한번 혹은 재시도 횟수만큼만 시도하여 redis에 부하를 줄일 수 있다.

**단점**

- 별도의 의존성을 추가해야 한다는 것이다.

---

이렇게 mysql과 redis를 비교해서 동시성 문제를 해결하는 방법에 대해 공부했다.

**Mysql 과 Redis 비교해보고 상황에 맞게 사용하면 좋을 것 같다.**

- mysql - 성능이 레디스보다 좋진않지만 적정 트래픽까지 사용가능하다
- redis - mysql보다 성능이 좋아서 더 많은 요청을 처리할 수있다.

**Reference**

[https://www.inflearn.com/course/동시성이슈-재고시스템/dashboard](https://www.inflearn.com/course/%EB%8F%99%EC%8B%9C%EC%84%B1%EC%9D%B4%EC%8A%88-%EC%9E%AC%EA%B3%A0%EC%8B%9C%EC%8A%A4%ED%85%9C/dashboard)