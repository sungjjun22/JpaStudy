## 엔티티 설계시 주의할 점
- 엔티티에는 가급적 Setter를 사용하지 말자
- Setter가 모두 열려있다. 변경포인트가 너무 많아서, 유니보수가 어렵다.

### 모든 연관관계는 지연로딩으로 설정 ★★★★★
- 즉시로딩 (EAGER) 은 예측이 어렵고, 어떤 SQL이 실행될지 추적하기 어렵다. 특히 JPQL을 실행할 때 N+1 문제가 자주 발생한다.
  - 즉시로딩은 멤버를 조회할 때 오더를 전체 조회한다. (연관된 데이터들을 다 조회한다.)
- 실무에서 모든 연관관계는 지연로딩(LAZY)으로 설정해야 한다.
- 연관된 엔티티를 함께 DB에서 조회해야 하면, fetch, join 또는 엔티티 그래프 기능을 사용한다.
- @XToOne(OneToOne, ManyToOne) 관계는 기본이 즉시로딩이므로 직접 지연로딩으로 설정해야 한다.

### 컬렉션은 필드에서 초기화 하자.
컬렉션은 필드에서 바로 초기화 하는 것이 안전하다.
- null 문제에서 안전하다.
- 하이버네이트는 엔티티를 영속화 할 때, 컬렉션을 감싸서 하이버네이트가 제공하는 내장 컬렉션으로 변경한다. 만약 getOrders() 처럼 임의의 메서드에서 컬렉션을 잘못 생성하면
하이버네이트 내부 메커니즘에 문제가 발생할 수 있다. 따라서 필드레벨에서 생성하는 것이 가장 안전하고, 코드도 간결하다.

### 참고
주문 서비스의 주문과 주문 취소 메서드를 보면 비즈니스 로직 대부분이 엔티티에 있다. 서비스 계층은 단순히 엔티티에 필요한 요청을 위임하는 역할을 한다. 
이처럼 엔티티가 비즈니스 로직을 가지고 객체 지향의 특성을 적극적으로 활용하는 것을 <Strong>도메인 모델 패턴</Strong> 이라고 한다.
반대로 엔티티에는 비즈니스 로직이 거의 없고, 서비스 계층에서 대부분의 비즈니스 로직을 처리하는 것을 트랜잭션 스크립트 패턴이라고 한다.
뭐가 더 유지보수하기 쉬운지 고민해보자.

### 변경 감지와 병합 (merge)
- 준영속 엔티티
  - 영속성 컨텍스트가 더는 관리하지 않는 엔티티를 말한다. (여기서는 `itemService.saveItem(book)`에서 수정을 시도하는 `Book` 객체다. `Book` 객체는 이미 DB에 한번 저장되어서 식별자가 존재한다. 이렇게 임의로 만들어낸 엔티티도 기존 식별자를 가지고 있으면 준영속 엔티티로 볼 수 있다.)
- 준영속 엔티티를 수정하는 2가지 방법
  - 변경 감지 기능 사용
  - 병합 (`merge`) 사용

#### 병합 동작 방식
1. `merge()`를 실행한다.
2. 파라미터로 넘어온 준영속 엔티티의 식별자 값으로 1차 캐시에서 엔티티를 조회한다.
2-1. 만약 1차 캐시에 엔티티가 없으면 데이터베이스에서 엔티티를 조회하고 1차 캐시에 저장한다.
3. 조회한 영속 엔티티 (`mergeMember`)에`member` 엔티티의 값을 채워 넣는다.(member 엔티티의 모든 값을 mergeMember에 밀어 넣는다. 이때 mergeMember의 "회원1"이라는 이름이 ""회원명변경" 으로 바뀐다.)
4. 영속 상태인 mergeMember를 반환한다.

#### 병합시 동작 방식을 간단히 정리
1. 준영속 엔티티의 식별자 값으로 영속 엔티티를 조회한다.
2. 영속 엔티티의 값을 준영속 엔티티의 값으로 모두 교체한다.(병합한다.)
3. 트랜잭션 커밋 시점에 변경 감지 기능이 동작해서 데이터베이스에 UPDATE SQL이 실행

주의) 변경 감지 기능을 사용하면 원하는 속성만 선택해서 변경할 수 있지만, 병합을 사용하면 모든 속성이 변경된다. 병합시 값이 없으면 `null` 로 옵데이트 할 위험도 있다. (병합은 모든 필드를 교체한다.)

### 새로운 엔티티 저장과 준영속 엔티티 병합을 편리하게 한번에 처리
상품 리포지토리에선 `save()` 메서드를 유심히 봐야되는데, 이 메서드 하나로 저장과 수정(병합)을 다 처리한다. 코드를 보면 식별자가 값이 없으면 새로운 엔티티로 판단해서 `persist()`로 영속화하고 만약 식별자 값이 있으면 이미 한번 영속화 되었던 엔티티로 판단해서 `merge()`로 수정(병합) 한다. 결국 여기서의 저장(save) 이라는 의미는 신규 데이터를 저장하는 것뿐만 아니라 변경된 데이터의 저장이라는 의미도 포함한다. 이렇게 함으로써 이 메서드를 사용하는 클라이언트는 저장과 수정을 구분하지 않아도 되므로 클라이언트의 로직이 단순해진다.
여기서 사용하는 수정(병합)은 준영속 상태의 엔티티를 수정할 때 사용한다. 영속 상태의 엔티티는 변경 감지 (dirty checking)기능이 동작해서 트랜잭션을 커밋할 때 자동으로 수정되므로 별도의 수정 메서드를 호출할 필요가 없고 그런 메서드도 없다.

### 가장 좋은 해결 방법
#### 엔티티를 변경할 때는 항상 변경 감지를 사용
- 컨트롤러에서 어설프게 엔티티를 생성하지 말자
- 트랜잭션이 있는 서비스 계층에 식별자('id')와 변경할 데이터를 명확하게 전달(parameter or dto)
- 트랜잭션이 있는 서비스 계층에서 영속 상태의 엔티티를 조회하고, 엔티티의 데이터를 직적 변경하자
- 트랜잭션 커밋 시점에 변경 감지가 실행된다.

## 조회 V1 : 응답 값으로 엔티티를 직접 외부에 노출
`@GetMapping("/api/v1/members")
public List<Member> membersV1() {
return memberService.findMembers();
}`
- 문제점
  - 엔티티에 프레젠테이션 계층을 위한 로직이 추가된다.
  - 기본적으로 엔티티의 모든 값이 노출된다.
  - 응답 스펙을 맞추기 위해 로직이 추가된다.(@JsonIgnore, 별도의 뷰 로직 등등)
  - 실무에서는 같은 엔티티에 대해 API가 용도에 따라 다양하게 만들어지는데, 한 엔티티에 각각의 API를 위한 프레젠테이션 응답 로직을 담기는 어렵다.
  - 엔티티가 변경되면 API 스펙이 변한다.
  - 추가로 컬렉션을 직접 반환하면 항후 API 스펙을 변경하기 어렵다. (별도의 Result 클래스 생성으로 해결)
- 결론
  - API 응답 스펙에 맞추어 별도의 DTO를 반환한다.


## API 개발 고급
- 조회용 샘플 데이터 입력
- 지연 로딩과 조회 성능 최적화
- 컬렉션 조회 최적화
- 페이징과 한계 돌파
- OSIV와 성능 최적화

### 페이징과 한계 돌파
- 컬렉션을 페치 조인하면 페이징이 불가능
  - 컬렉션을 페치 조인하면 일대다 조인이 발생하므로 데이터가 예측할 수 없이 증가한다.
  - 일대다에서 1 을 기준으로 페이징을 하는 것이 목적이다. 그런데 데이터는 N 을 기준으로 row가 생성된다.
  - Order 를 기준으로 페이징하고 싶은데, N인 OrderItem 이 기준이 되어버린다.
- 이 경우 하이버네이트는 경고 로그를 남기고 모든 DB 데이터를 읽어서 메모리에서 페이징을 시도한다. 최악의 경우 장애로 이어진다.

#### 한계 돌파
페이징 + 컬렉션 엔티티를 함께 조회하려면 어떻게 해야될까???

1. 먼저 ToOne 관계는 모두 페치조인한다. ToOne 관계는 row 수를 증가시키지 않으므로 페이징 쿼리에 영향을 주지 않는다.
2. 컬렉션은 지연 로딩으로 조회한다.
3. 지연 로딩 성능 최적화를 위해 `hibernate.default_batch_fetch_size`,`@BatchSize`를 적용한다.
  - `hibernate.default_batch_fetch_size`: 글로벌 설정
  - `@BatchSize`: 개별 최적화
  - 이 옵션을 사용하면 컬렉션이나 프록시 객체를 한거뻔에 설정한 size 만큼 IN 쿼리로 조회한다.
장점
  - 쿼리 호출 수가 1 + N -> 1 + 1 로 최적화 된다.
  - 조인보다 DB 데이터 전송량이 최적화 된다.(Order와 OrderItem을 조인하면 Order가 OrderItem 만큼 중복해서 조회된다. 이 방법은 각각 조회하므로 전송해야할 중복 데이터가 없다.)
  - 페치 조인 방식과 비교해서 쿼리 호출 수가 약간 증가하지만, DB 데이터 전송량이 감소한다.
  - 컬렉션 페치 조인은 페이징이 불가능 하지만 이 방법은 페이징이 가능하다.
결론
  - ToOne 관계는 페치 조인해도 페이징에 영향을 주지 않는다. 따라서 ToOne 관계는 페치조인으로 쿼리 수를 줄이고 해결하고, 나머지는 
`hibernate.default_batch_fetch_size` 로 최적화 하자.





