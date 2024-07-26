package jpabook.jpashop.domain;

import jakarta.persistence.Embeddable;
import lombok.Getter;

@Embeddable
@Getter
public class Address {
  private String city;
  private String street;
  private String zipcode;

  // JPA 스펙상 엔티티나 임베디드 타입은 자바 기본생성를 public 또는 protected 설정
  protected Address() {
  }

  public Address(String city, String street, String zipcode) {
    this.city = city;
    this.street = street;
    this.zipcode = zipcode;
  }
}
