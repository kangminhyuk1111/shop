package com.example.shoppingmall.board.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.shoppingmall.auth.application.token.TokenProvider;
import com.example.shoppingmall.auth.domain.member.Member;
import com.example.shoppingmall.auth.domain.member.MemberRepository;
import com.example.shoppingmall.board.application.dto.CreateMatchRequest;
import com.example.shoppingmall.board.application.dto.DeleteMatchRequest;
import com.example.shoppingmall.board.application.dto.FindMatchByMatchIdRequest;
import com.example.shoppingmall.board.application.dto.MatchResponse;
import com.example.shoppingmall.board.domain.match.Match;
import com.example.shoppingmall.board.domain.match.MatchRepository;
import com.example.shoppingmall.board.domain.type.MatchCategory;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

  @Mock
  private MatchRepository matchRepository;

  @Mock
  private MemberRepository memberRepository;

  @Mock
  private TokenProvider tokenProvider;

  @InjectMocks
  private MatchService matchService;

  private Member member;
  private String token;
  private Long userId;
  private Long matchId;

  @BeforeEach
  void setUp() {
    userId = 1L;
    matchId = 1L;
    token = "valid-jwt-token";

    member = Member.builder()
        .id(userId)
        .build();
  }

  @Nested
  @DisplayName("매치 생성 테스트")
  class CreateMatchTest {

    @Test
    @DisplayName("매치 생성 성공 - 토큰 방식")
    void createMatch_withToken_success() {
      // given
      CreateMatchRequest createRequest = new CreateMatchRequest(
          MatchCategory.MATCHING,
          "새로운 매치",
          "매치 내용",
          "부산시 해운대구",
          LocalDateTime.now().plusDays(2),
          8
      );

      Match mockMatch = mock(Match.class);

      when(tokenProvider.getUserId(token)).thenReturn(userId);
      when(memberRepository.findById(userId)).thenReturn(Optional.of(member));
      when(matchRepository.save(any(Match.class))).thenReturn(mockMatch);

      // when
      Match result = matchService.createMatch(userId, createRequest);

      // then
      assertThat(result).isNotNull();
      verify(tokenProvider).getUserId(token);
      verify(memberRepository).findById(userId);
      verify(matchRepository).save(any(Match.class));
    }

    @Test
    @DisplayName("매치 생성 실패 - 존재하지 않는 유저")
    void createMatch_userNotFound() {
      // given
      CreateMatchRequest createRequest = new CreateMatchRequest(
          MatchCategory.MATCHING,
          "새로운 매치",
          "매치 내용",
          "부산시 해운대구",
          LocalDateTime.now().plusDays(2),
          8
      );

      when(tokenProvider.getUserId(token)).thenReturn(userId);
      when(memberRepository.findById(userId)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> matchService.createMatch(userId, createRequest))
          .isInstanceOf(RuntimeException.class)
          .hasMessage("유저가 존재하지 않습니다.");

      verify(tokenProvider).getUserId(token);
      verify(memberRepository).findById(userId);
      verify(matchRepository, never()).save(any(Match.class));
    }
  }

  @Nested
  @DisplayName("매치 삭제 테스트")
  class DeleteMatchTest {

    @Test
    @DisplayName("매치 삭제 성공 - 토큰 방식")
    void deleteMatch_withToken_success() {
      // given
      DeleteMatchRequest deleteRequest = new DeleteMatchRequest(matchId);
      Match mockMatch = mock(Match.class);

      when(tokenProvider.getUserId(token)).thenReturn(userId);
      when(matchRepository.findById(matchId)).thenReturn(Optional.of(mockMatch));
      when(mockMatch.isOwnedBy(userId)).thenReturn(true);

      // when
      matchService.deleteMatch(userId, deleteRequest);

      // then
      verify(tokenProvider).getUserId(token);
      verify(matchRepository).findById(matchId);
      verify(mockMatch).isOwnedBy(userId);
      verify(matchRepository).delete(mockMatch);
    }

    @Test
    @DisplayName("매치 삭제 실패 - 존재하지 않는 매치")
    void deleteMatch_matchNotFound() {
      // given
      DeleteMatchRequest deleteRequest = new DeleteMatchRequest(matchId);

      when(tokenProvider.getUserId(token)).thenReturn(userId);
      when(matchRepository.findById(matchId)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> matchService.deleteMatch(userId, deleteRequest))
          .isInstanceOf(RuntimeException.class)
          .hasMessage("매치를 찾을 수 없습니다.");

      verify(tokenProvider).getUserId(token);
      verify(matchRepository).findById(matchId);
      verify(matchRepository, never()).delete(any(Match.class));
    }

    @Test
    @DisplayName("매치 삭제 실패 - 권한 없음 (작성자가 아님)")
    void deleteMatch_unauthorizedUser() {
      // given
      DeleteMatchRequest deleteRequest = new DeleteMatchRequest(matchId);
      Long otherUserId = 999L;
      Match mockMatch = mock(Match.class);

      when(tokenProvider.getUserId(token)).thenReturn(otherUserId);
      when(matchRepository.findById(matchId)).thenReturn(Optional.of(mockMatch));
      when(mockMatch.isOwnedBy(otherUserId)).thenReturn(false);

      // when & then
      assertThatThrownBy(() -> matchService.deleteMatch(userId, deleteRequest))
          .isInstanceOf(RuntimeException.class)
          .hasMessage("작성자가 아니기 때문에 삭제할 수 없습니다.");

      verify(tokenProvider).getUserId(token);
      verify(matchRepository).findById(matchId);
      verify(mockMatch).isOwnedBy(otherUserId);
      verify(matchRepository, never()).delete(any(Match.class));
    }
  }

  @Nested
  @DisplayName("매치 조회 테스트")
  class FindMatchTest {

    @Test
    @DisplayName("모든 매치 조회 성공")
    void findAllMatches_success() {
      // given
      Match mockMatch = mock(Match.class);
      List<Match> matches = Arrays.asList(mockMatch);

      when(matchRepository.findAll()).thenReturn(matches);

      // when
      List<MatchResponse> result = matchService.findAllMatches();

      // then
      assertThat(result).isNotNull();
      assertThat(result).hasSize(1);
      verify(matchRepository).findAll();
    }

    @Test
    @DisplayName("매치 ID로 조회 성공")
    void findMatchById_success() {
      // given
      FindMatchByMatchIdRequest findRequest = new FindMatchByMatchIdRequest(matchId);
      Match mockMatch = mock(Match.class);

      when(matchRepository.findById(matchId)).thenReturn(Optional.of(mockMatch));

      // when
      MatchResponse result = matchService.findMatchById(findRequest);

      // then
      assertThat(result).isNotNull();
      verify(matchRepository).findById(matchId);
    }

    @Test
    @DisplayName("매치 ID로 조회 실패 - 존재하지 않는 매치")
    void findMatchById_matchNotFound() {
      // given
      FindMatchByMatchIdRequest findRequest = new FindMatchByMatchIdRequest(matchId);

      when(matchRepository.findById(matchId)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> matchService.findMatchById(findRequest))
          .isInstanceOf(RuntimeException.class)
          .hasMessage("게시물이 존재하지 않습니다.");

      verify(matchRepository).findById(matchId);
    }
  }
}