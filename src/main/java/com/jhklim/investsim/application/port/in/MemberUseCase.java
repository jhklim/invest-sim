package com.jhklim.investsim.application.port.in;

import com.jhklim.investsim.application.dto.MemberInfoResult;

public interface MemberUseCase {
    MemberInfoResult getMyInfo(Long memberId);
}