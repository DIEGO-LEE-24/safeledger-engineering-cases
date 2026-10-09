# 근거 기준

독립 예제의 구현 근거는 이 저장소의 독립 예제 코드·테스트·[분리 후 실행 기록](test-results.md)입니다.

- LLM Gateway: [구현](../examples/llm-gateway/src/LlmGateway.java) → [15개 테스트](../examples/llm-gateway/tests/LlmGatewayTest.java).
- Guardrails: [구현](../examples/agent-guardrails/guard.mjs) → [20개 테스트](../examples/agent-guardrails/guard.test.mjs).

구현·테스트·러너·데모는 통합 예제에서 바이트 변경 없이 옮겼습니다. 분리 후 실행 기록은 당시의 파일 해시로 실행 버전을 식별합니다. 문서와 정적 점검의 허용 목록만 새 범위에 맞췄습니다.

합성 테스트 통과는 원본 SafeLedger의 코드·개인 기여·모델 품질·운영 성과를 증명하지 않습니다. 실제 모델 비교는 버전·조건·입력·분모·캐시·무효 측정을 구별해야 하며 원본 기록 공개는 별도 권리 확인 대상입니다.

팀 원본 자료·성과·수상·개인 기여 상세는 저장소 밖에서 확인합니다. [파일·링크 점검](../tools/check_repository.py)은 권리 확인이나 제품 실행 검증을 대신하지 않습니다.

[목록](../README.md)
