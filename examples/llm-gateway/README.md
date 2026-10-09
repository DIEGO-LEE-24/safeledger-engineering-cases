# LLM Gateway — Java 최소 재현

호출 계약과 실패 정책을 검증하는 독립 예제입니다. 원본 제품·LLM SDK·모델을 사용하지 않고 메모리 공급자의 정상·장애 동작을 주입합니다.

## 설계

[`LlmGateway.java`](src/LlmGateway.java)의 `Provider`가 공급자 경계입니다. `DOCUMENT`는 설정된 체인, `JUDGMENT`는 첫 공급자만 사용합니다. `available()`과 `generate()`가 같은 `permittedChain()`을 사용해 첫 공급자만 보는 가용성 결함을 방지합니다.

`generate()`는 사용 불가 공급자를 건너뛰고 선언된 `ProviderFailure`만 대체 조건으로 취급합니다. 일반 프로그래밍 오류는 전파합니다. 성공 결과에 실제 공급자 ID를 반환합니다.

## 실행

JDK 17+와 Python 3.10+가 필요합니다. 저장소 루트에서:

```sh
python -B examples/llm-gateway/run.py
python -B examples/llm-gateway/run.py --demo
```

[`run.py`](run.py)는 Java 파일을 시스템 임시 폴더에서 컴파일·실행하고 종료 시 정리합니다. Maven·Gradle·외부 의존성이 없습니다. 컴파일·테스트 실패는 0이 아닌 종료 코드로 반환합니다.

데모는 첫 공급자 불가 상태에서 문서는 `secondary` 응답을 받고 판단 가용성은 `false`임을 보여 줍니다. 실제 모델 응답이 아닙니다.

## 테스트

[`LlmGatewayTest.java`](tests/LlmGatewayTest.java)는 15개 사례를 실행합니다. 비활성화 가능한 Java `assert` 대신 실패 시 예외와 종료 코드를 사용합니다.

- 정상 문서·판단 호출, 실제 사용 공급자와 호출 횟수.
- 첫 공급자 불가·두 번째 정상: 가용성 회귀와 실제 대체 호출.
- 선언된 장애의 문서 폴백, 판단의 불가·장애에서 대체 미호출.
- 모두 불가·모두 장애, 프로그래밍 오류를 숨기지 않음.
- 누락·빈 경로, 빈 프롬프트, 설정 리스트 변경의 영향 분리.

## 범위

가용성은 순간의 신호입니다. 검사 직후 상태가 바뀔 수 있으며 실제 호출 실패도 별도로 처리합니다. 재시도·서킷브레이커·타임아웃·스트리밍·비용·모델 품질 평가는 포함하지 않습니다. 판단 실패 시 사람 검토 UI도 구현하지 않습니다.

[설계](../../cases/a-safeledger-llm.md) · [실행 결과](../../evidence/test-results.md) · [소개](../../README.md)
