# 분리 후 독립 예제 실행 결과

실행일: 2026-10-09. 이 로컬 디렉터리의 Java·JavaScript 예제를 다시 실행했습니다. 원본 SafeLedger의 성능·운영·모델 비교 결과가 아닙니다.

## 환경·결과

Windows, Java/Javac17.0.12, Python3.12.10, Node.js24.16.0. 외부 패키지 설치·API 키·네트워크 없이 실행했습니다. 최소 요구 버전 전체의 호환성 시험은 하지 않았습니다.

| 예제 | 테스트 | 통과 | 실패 | skip | 종료 코드 |
|---|---:|---:|---:|---:|---:|
| LLM Gateway | 15 | 15 | 0 | 0 | 0 |
| Agent Guardrails | 20 | 20 | 0 | 0 | 0 |
| 합계 | 35 | 35 | 0 | 0 | — |

카운트는 테스트 사례 수이며 합성 요청 루프를 별도로 중복 집계하지 않습니다. 예상 장애의 예외·거절을 확인한 경우도 정상 통과입니다.

## 실행 명령

저장소 루트에서 실행했습니다.

```sh
python -B examples/llm-gateway/run.py
node --test examples/agent-guardrails/guard.test.mjs
```

확인한 출력:

```text
Java: RESULT passed=15 failed=0
Node: tests 20, pass 20, fail 0, cancelled 0, skipped 0, todo 0
```

README의 두 데모 명령도 종료 코드0으로 실행했습니다. Java는 문서의 두 번째 공급자 응답·판단 불가, JavaScript는 읽기 허용·쓰기 거절을 출력했습니다. Java 컴파일은 임시 폴더에서 정리됐으며 제품 API·셸·실제 파일 변경을 호출하는 데모가 아닙니다.

## 파일 동일성과 식별

아래7개 구현·테스트·러너·데모 파일을 기존 통합 예제와 SHA-256으로 대조해 모두 동일함을 확인했습니다. 새 기능이나 테스트를 추가하지 않았습니다. 통합 폴더의 기존22개 파일은 변경하지 않았습니다. 분리 후 검증 당시에는 파일 해시로 실행 버전을 식별했습니다.

| 파일 | SHA-256 |
|---|---|
| [LlmGateway.java](../examples/llm-gateway/src/LlmGateway.java) | `5bfb8d039138c9525607d558fb419b9e32e6f2457277848c5aa92658d4d346f3` |
| [LlmGatewayTest.java](../examples/llm-gateway/tests/LlmGatewayTest.java) | `39f9f83a785998a3e8b088a19a328f1fd80a2b9e355e3a11fe9bb70b69b82216` |
| [run.py](../examples/llm-gateway/run.py) | `0369126ee849b98dff807e9225b76e05c6db21e848a1bba74075d06375da5234` |
| [Demo.java](../examples/llm-gateway/src/Demo.java) | `01bd45218170e2081530ba887e9905537a538712026d3fc062e105d8a77848a7` |
| [guard.mjs](../examples/agent-guardrails/guard.mjs) | `8b4da4ef2d5ea5e774ab8e47d4ff53b6a46b4a786634e6e7f54fdaece9edc2e0` |
| [guard.test.mjs](../examples/agent-guardrails/guard.test.mjs) | `76038d38f62ba89bc920605d95b4569fd5a41d2847302530584b49ea9bdc1bf6` |
| [demo.mjs](../examples/agent-guardrails/demo.mjs) | `4614e27bb11c01930c918ad6a6cba2e473d0d39010e8d054d58c36e775319bf3` |

결과는 예제의 합성 입력 계약에 한정합니다. 실제 모델 품질·API 장애·전체 우회 차단·OS 권한 격리를 증명하지 않습니다. 코드 변경 시 다시 실행하고 파일 식별을 갱신해야 합니다.

## 로컬 구성 검수

정적 점검은 허용 파일17개·로컬 링크51개·Mermaid4개를 확인했고 검출 문제는0건입니다. 통합 폴더22개 파일의 분리 전후 해시도 모두 같았습니다. 위 수치는 분리 후 로컬 검증 기록입니다. 공개 범위는 프로젝트명·일반 소개와 독립 예제 자료로 한정합니다.

[근거 기준](README.md) · [소개](../README.md)
