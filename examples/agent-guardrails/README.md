# Coding Agent Guardrails — JavaScript 최소 재현

호스트가 정한 역할과 구조화된 도구 요청을 검사하는 독립 예제입니다. 실제 Coding Agent·셸·파일시스템 훅은 연결하지 않고 합성 요청과 메모리 핸들러를 사용합니다.

## 정책

[`guard.mjs`](guard.mjs)의 `evaluate()`가 허용 여부를 반환하고 `guardedRunner()`가 승인된 요청만 핸들러에 전달합니다.

| 역할 | 도구 | 쓰기 범위 |
|---|---|---|
| audit / reproduce / judge | read_file, search_files | 없음 |
| fix | read_file, search_files, write_file | 상대 경로 `work/` 아래 |

삭제·push·셸과 알 수 없는 도구는 모두 거절합니다. 경로는 `/`로 구분된 상대 경로만 받으며 절대 경로·역슬래시·상위 이동·빈 구간·제어 문자는 거절합니다. `work/`는 합성 정책 공간이며 실제 폴더를 만들거나 쓰지 않습니다.

역할은 호스트에서 고정해 요청 필드로 승격하지 못합니다. 도구·경로는 데이터 필드의 immutable snapshot으로 고정합니다. 접근자 프로퍼티는 거절해 검사와 핸들러 호출 사이의 도구 값 변화를 방지합니다. 일반 JSON 형태의 요청을 전제로 하며 임의의 적대적 JavaScript 실행을 격리하는 도구는 아닙니다.

## 실행

Node.js 18+의 내장 테스트 러너만 사용합니다. 저장소 루트에서:

```sh
node --test examples/agent-guardrails/guard.test.mjs
node examples/agent-guardrails/demo.mjs
```

데모는 감사 역할의 정상 읽기를 허용하고 쓰기를 거절합니다. 메모리 핸들러만 호출합니다.

## 테스트

[`guard.test.mjs`](guard.test.mjs)의 20개 테스트는 역할 정책과 핸들러 연결을 확인합니다.

- 오탐: 배포·삭제 단어가 있는 경로의 정상 읽기는 허용.
- 누락: 무해해 보이는 파일 이름의 쓰기, 역할 위장, 상위 이동, 미등록 도구는 거절.
- 거절 요청은 핸들러 미호출, 허용 요청은 1회 호출, 미등록 핸들러는 거절.
- 접근자로 도구를 바꾸는 요청 거절, 검사한 immutable 요청 전달, 핸들러 오류 전파.

## 훅과 OS 권한의 차이

가드는 위임된 호출 경로를 검사할 뿐입니다. 핸들러 직접 호출·심볼릭 링크의 실제 대상·프로세스 밖 동작을 막지 않습니다. 실제 연결 시 호스트가 경로를 해석하고 도구를 제한해야 하며 OS 계정 권한·격리·읽기 전용 마운트는 별도로 적용해야 합니다. 여기서는 OS 권한 격리를 구현하거나 시험하지 않았습니다.

[설계](../../cases/c-safeledger-agent.md) · [실행 결과](../../evidence/test-results.md) · [소개](../../README.md)
