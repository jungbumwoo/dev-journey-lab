package com.jungbum.jdbc.service;

import com.jungbum.jdbc.domain.Member;
import com.jungbum.jdbc.repository.MemberRepositoryV3;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.sql.SQLException;


/**
 * 트랜잭션 - 트랜잭션 매니저
 */
@Slf4j
@RequiredArgsConstructor
public class MemberServiceV3_1 {

    // private final DataSource dataSource;
    // transaction을 추상화함. JDBC transacion, JPA transtaion 등등..  의 구현체에 의존 x
    // txManager가 리소스 동기화를 제공함 - thread 로컬 사용. `org.springframework.transaction.support.TransactionSynchronizationManager`
    // 동기화를 제공하기에 repo에 호출 시 connection을 파라미터로 전달하는 부분을 모두 제거가 가능해짐
    private final PlatformTransactionManager transactionManager; // transactionManager로 네이밍하려다 다른데서 이미 쓴 곳이 있었다는 것 같음
    private final MemberRepositoryV3 memberRepository;

    public void accountTransfer(String fromId, String toId, int money) throws SQLException {
        // 트랜잭션 시작
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());

        try {
            //비즈니스 로직
            bizLogic(fromId, toId, money);
            transactionManager.commit(status);
        } catch (Exception e) {
            transactionManager.rollback(status);
            throw new IllegalStateException(e);
        }
//        finally {
//            release(con); 불필요. 알아서 해줌
//        }
    }

    private void bizLogic(String fromId, String toId, int money) throws SQLException {
        //시작
        Member fromMember = memberRepository.findById(fromId);
        Member toMember = memberRepository.findById(toId);

        memberRepository.update(fromId, fromMember.getMoney() - money);
        validation(toMember);
        memberRepository.update(toId, toMember.getMoney() + money);
    }

    private void validation(Member toMember) {
        if (toMember.getMemberId().equals("ex")) {
            throw new IllegalStateException("이체중 예외 발생");
        }
    }
}
