package com.server.running_handai.domain.admin.controller;

import com.server.running_handai.domain.admin.dto.AdminLoginRequestDto;
import com.server.running_handai.domain.admin.service.AdminService;
import com.server.running_handai.domain.member.dto.TokenResponseDto;
import com.server.running_handai.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.server.running_handai.global.response.ResponseCode.*;

@Hidden
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/login")
    public ResponseEntity<CommonResponse<TokenResponseDto>> adminLogin(@RequestBody AdminLoginRequestDto request) {
        TokenResponseDto tokenResponseDto = adminService.login(request);
        return ResponseEntity.ok(CommonResponse.success(SUCCESS, tokenResponseDto));
    }

}
