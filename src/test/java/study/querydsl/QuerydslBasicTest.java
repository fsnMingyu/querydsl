package study.querydsl;

import com.querydsl.core.QueryFactory;
import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.member;

@SpringBootTest
@Transactional

public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory jpaQueryFactory;


    @BeforeEach
    public void before() {
        jpaQueryFactory = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void startJPQL() {
        //member1을 찾아라.
        String qlString = "select m from Member m where m.username = :username";

        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl() {
        Member findMember = jpaQueryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search() {
        Member findMember = jpaQueryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchAndParam() {
        Member findMember = jpaQueryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"), // and와 같이 처리
                        member.age.eq(10))
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetch() {
//        List<Member> fetch = jpaQueryFactory
//                .selectFrom(member)
//                .fetch(); //List 조회
//
//        Member fetchOne = jpaQueryFactory
//                .selectFrom(member)
//                .fetchOne();  //단건 조회회  여러건 존재시(NonUniqueResultException) 발생.
//
//        Member fetchFirst = jpaQueryFactory
//                .selectFrom(member)
//                .fetchFirst();


        //쿼리 2번 나감. count 페이징 처리를 위해 발생 / content ( 데이터 가지고오는 쿼리 )
//        QueryResults<Member> results = jpaQueryFactory
//                .selectFrom(member)
//                .fetchResults();
//
//        results.getTotal();
//        List<Member> content = results.getResults(); //data;

        long total = jpaQueryFactory
                .selectFrom(member)
                .fetchCount();
    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(DESC)
     * 2. 회원 이름 올림차순(ASC)
     * 단 2에서 회원 이름이 없으면 마지막에 출력 (nulls last)
     */
    @Test
    public void sort(){
        em.persist(new Member(null,100));
        em.persist(new Member("member5",100));
        em.persist(new Member("member6",100));

        List<Member> result = jpaQueryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    public void paging1(){
        List<Member> result = jpaQueryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) //끊어서 가져오는 내용 0부터 시작
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);

    }

    @Test
    public void paging2(){
        QueryResults<Member> queryResults = jpaQueryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) //끊어서 가져오는 내용 0부터 시작
                .limit(2)
                .fetchResults();

        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2);


    }
}
