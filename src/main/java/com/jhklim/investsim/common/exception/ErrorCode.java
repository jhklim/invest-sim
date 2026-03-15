package com.jhklim.investsim.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 400 Bad Request
    INSUFFICIENT_BALANCE(400, "INSUFFICIENT_BALANCE", "잔고가 부족합니다."),
    NO_OPEN_POSITION(400, "NO_OPEN_POSITION", "매도할 포지션이 없습니다."),

    // 401 Unauthorized
    INVALID_PASSWORD(401, "INVALID_PASSWORD", "비밀번호가 일치하지 않습니다."),
    INVALID_REFRESH_TOKEN(401, "INVALID_REFRESH_TOKEN", "유효하지 않은 리프레시 토큰입니다."),

    // 404 Not Found
    MEMBER_NOT_FOUND(404, "MEMBER_NOT_FOUND", "회원이 존재하지 않습니다."),
    STRATEGY_NOT_FOUND(404, "STRATEGY_NOT_FOUND", "전략이 존재하지 않습니다."),

    // 403 Forbidden
    SIGNUP_LIMIT_EXCEEDED(403, "SIGNUP_LIMIT_EXCEEDED", "회원가입 가능 인원을 초과했습니다."),
    STRATEGY_LIMIT_EXCEEDED(403, "STRATEGY_LIMIT_EXCEEDED", "전략 생성 가능 개수를 초과했습니다."),

    // 409 Conflict
    DUPLICATE_EMAIL(409, "DUPLICATE_EMAIL", "이미 존재하는 이메일입니다."),

    // 500 Internal Server Error
    CURRENT_PRICE_NOT_FOUND(500, "CURRENT_PRICE_NOT_FOUND", "현재가 정보가 없습니다."),
    INTERNAL_ERROR(500, "INTERNAL_ERROR", "서버 오류가 발생했습니다.");

    private final int httpStatus;
    private final String code;
    private final String message;
}