package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryDto;
import jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * xToOne (ManyToOne, OneToOne)
 * Order
 * Order -> Member
 * Order -> Delivery
 */

@RestController
@AllArgsConstructor
public class OrderSimpleApiController {
  private final OrderRepository orderRepository;
  private final OrderSimpleQueryRepository orderSimpleQueryRepository;

  @GetMapping("api/v1/simple-orders")
  public List<Order> ordersV1() {
    List<Order> all = orderRepository.findAllByCriteria(new OrderSearch());
    for (Order order : all) {
      order.getMember().getName();  // Lazy 강제 초기화
      order.getDelivery().getAddress(); // Lazy 강제 초기화
    }

    return all;
  }

  // v1과 v2 모두 LAZY 로딩으로 인한 sql 쿼리가 너무 많이 호출 되는 문제가 있음.
  @GetMapping("api/v2/simple-orders")
  public List<SimpleOrderDto> ordersV2() {
    // ORDER 2개
    // N + 1 의 문제 -> 1 + 회원 N + 배송 N (여기서는 2개) 문제 -> 총 5번 실행
    List<Order> orders = orderRepository.findAllByCriteria(new OrderSearch());

    List<SimpleOrderDto> result = orders.stream()
        .map(o -> new SimpleOrderDto(o))
        .collect(Collectors.toList());

    return result;
  }

  @GetMapping("api/v3/simple-orders")
  public List<SimpleOrderDto> ordersV3() {
    List<Order> orders = orderRepository.findAllWithMemberDelivery();
    List<SimpleOrderDto> result = orders.stream()
        .map(o -> new SimpleOrderDto(o))
        .collect(Collectors.toList());
    return result;
  }

  @GetMapping("api/v4/simple-orders")
  public List<OrderSimpleQueryDto> ordersV4() {
    return orderSimpleQueryRepository.findOrderDtos();
  }

  @Data
  static class SimpleOrderDto {
    private Long orderId;
    private String name;
    private LocalDateTime orderDate;
    private OrderStatus orderStatus;
    private Address address;

    public SimpleOrderDto(Order order) {
      orderId = order.getId();
      name = order.getMember().getName(); // LAZY 초기화
      orderDate = order.getOrderDate();
      orderStatus = order.getStatus();
      address = order.getDelivery().getAddress(); // LAZY 초기화
    }
  }
}