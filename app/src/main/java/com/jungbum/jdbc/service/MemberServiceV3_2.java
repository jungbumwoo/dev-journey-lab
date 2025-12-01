package com.jungbum.jdbc.service;

import com.jungbum.jdbc.domain.Member;
import com.jungbum.jdbc.repository.MemberRepositoryV3;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.SQLException;


/**
 * transaction - transaction template
 */
@Slf4j
public class MemberServiceV3_2 {

    private final TransactionTemplate txTemplate; // interface 가 아닌 구현체라 테스트 작성하기 좀 어려울 수도?
    private final MemberRepositoryV3 memberRepository;

    public MemberServiceV3_2(PlatformTransactionManager transactionManager, MemberRepositoryV3 memberRepository) {
        this.txTemplate = new TransactionTemplate(transactionManager);
        this.memberRepository = memberRepository;
    }

    public void accountTransfer(String fromId, String toId, int money) throws SQLException {
        // txTemplate 를 사용해서 반복적인 tx 관련 코드들이 제거됨
        txTemplate.executeWithoutResult((status) -> {
            try {
                bizLogic(fromId, toId, money);
            } catch (SQLException e) {
                // bizLogic에서 SQLException이 발생하니 try catch 넣고 SQLException을 람다에서 잡아버렸음.
                // 트랜잭션 보장을 위해선 unchecked(Runtime) 예외나 @Transactional 정책상 rollbackFor에 지정된 exception 타입을 던져줘야함
                throw new IllegalStateException(e);
            }
        });

//        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
//        try {
//            bizLogic(fromId, toId, money);
//            transactionManager.commit(status);
//        } catch (Exception e) {
//            transactionManager.rollback(status);
//            throw new IllegalStateException(e);
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
