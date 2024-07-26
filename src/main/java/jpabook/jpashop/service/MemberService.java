package jpabook.jpashop.service;

import jpabook.jpashop.domain.Member;
import jpabook.jpashop.repository.MemberRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberService {

//   필드 인젝션
//  @Autowired
  private final MemberRepository memberRepository;

//  세터 인젝션
//  @Autowired
//  public void setMemberRepository(MemberRepository memberRepository) {
//    this.memberRepository = memberRepository;
//  }

  // 생성자 인젝션
//  @Autowired // 생성자가 하나만 있는 경우 생략 가능하다.
//  public MemberService(MemberRepository memberRepository) {
//    this.memberRepository = memberRepository;
//  }
  // @RequiredArgsConstructor 를 쓰는걸 추천

  /**
   * 회원 가입
   */
  @Transactional
  // 따로 설정한 것은 이게 우선권을 얻어서 readOnly = false
  public Long join(Member member) {

    validateDuplicateMember(member);
    memberRepository.save(member);
    return member.getId();  // 아이디를 반환
  }

  /**
   * 중복 회원 검증 로직
   */
  private void validateDuplicateMember(Member member) {
    List<Member> findMembers = memberRepository.findByName(member.getName());
    if (!findMembers.isEmpty()) {
      throw new IllegalStateException("이미 존재하는 회원입니다.");
    }
  }

  /**
   * 회원 전체 조회
   */
  public List<Member> findMembers() {
    return memberRepository.findAll();
  }

  public Member findOne(Long memberId) {
    return memberRepository.findOne(memberId);
  }

  @Transactional
  public void update(Long id, String name) {
    // 트랜잭션이 있는 상태에서 조회를 하면 영속성 컨텍스트에서 값을 가져옴
    // 그 다음 값의 이름을 파라미터로 넘어온 값으로 바꾸면 엔티티가 변경됨 
    // 트랜잭션이 끝나고 커밋되는 시점에서 변경 감지를 실행함
    Member member = memberRepository.findOne(id);
    member.setName(name);
  }
}
