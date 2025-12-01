package com.jungbum.jdbc.service;

import com.jungbum.jdbc.domain.Member;
import com.jungbum.jdbc.repository.MemberRepositoryV3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.sql.SQLException;

import static com.jungbum.jdbc.connection.ConnectionConst.URL;
import static com.jungbum.jdbc.connection.ConnectionConst.USERNAME;
import static com.jungbum.jdbc.connection.ConnectionConst.PASSWORD;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@SpringBootTest
public class MemberServiceV3_3FailedTest {

    /*
    * @Transactional이 동작하려면: 해당 클래스가 스프링 컨테이너가 관리하는 빈(bean) 이어야 하고 그 빈이 프록시(proxy) 로 감싸져 있어야 함
    *
    * new MemberServiceV3_3(...)로 순수 자바 객체를 만든 상태
    * 이 객체 위에는 스프링 AOP 프록시가 전혀 적용되지 않기 때문에 @Transactional이 적용 안됨
    * */
    public static final String MEMBER_A = "memberA";
    public static final String MEMBER_B = "memberB";
    public static final String MEMBER_EX = "ex";

    private MemberRepositoryV3 memberRepository;
    private MemberServiceV3_3 memberService;

    @BeforeEach
    void before() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(URL, USERNAME, PASSWORD);
        memberRepository = new MemberRepositoryV3(dataSource);
        memberService = new MemberServiceV3_3(memberRepository);
    }

    @AfterEach
    void after() throws SQLException {
        memberRepository.delete(MEMBER_A);
        memberRepository.delete(MEMBER_B);
        memberRepository.delete(MEMBER_EX);
    }

    @Test
    @DisplayName("이체중 예외 발생 - @SpringBootTest 선언해두었음에도 트랜잭션 깨지는 경우")
    void accountTransferEx() throws SQLException {
        //given
        Member memberA = new Member(MEMBER_A, 10000);
        Member memberEx = new Member(MEMBER_EX, 10000);
        memberRepository.save(memberA);
        memberRepository.save(memberEx);

        //when
        assertThatThrownBy(() -> memberService.accountTransfer(memberA.getMemberId(), memberEx.getMemberId(), 2000))
                .isInstanceOf(IllegalStateException.class);

        //then
        Member findMemberA = memberRepository.findById(memberA.getMemberId());
        Member findMemberB = memberRepository.findById(memberEx.getMemberId());
        assertThat(findMemberA.getMoney()).isEqualTo(8000);   // 10,000이어야 트랜잭션이 보장된 상황
        assertThat(findMemberB.getMoney()).isEqualTo(10000);
    }

}