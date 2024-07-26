package jpabook.jpashop.domain;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class Test {
  private String name;
  private int age;

  public Test(String name, int age) {
    this.name = name;
    this.age = age;
  }

  void out() {
    System.out.println("이름: " + name + "나이: " + age);
  }
}

class TestMain{
  public static void main(String[] args) {
    Test test = new Test();
    test.out();
  }
}
