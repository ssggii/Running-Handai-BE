package com.server.running_handai.global.log;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        addRequestInfoToMdc(servletRequest); // 요청 ID와 IP 주소를 MDC에 추가

        try {
            filterChain.doFilter(servletRequest, servletResponse); // 다음 필터 또는 서블릿으로 요청 전달
        } finally {
            MDC.clear(); // 요청 처리가 끝나면 MDC 제거
        }
    }

    /**
     * 요청이 들어오면 고유한 요청 ID와 클라이언트 IP 주소를 MDC에 추가합니다.
     *
     * @param request ServletRequest 객체
     */
    private void addRequestInfoToMdc(ServletRequest request) {
        // 고유한 요청 ID 생성 및 MDC에 추가
        MDC.put("requestId", UUID.randomUUID().toString().substring(0, 8));

        // IP 주소 추출 및 MDC에 추가
        String clientIp = getClientIp((HttpServletRequest) request);
        MDC.put("clientIp", clientIp);
    }

    /**
     * 프록시 및 로드밸런서 환경을 포함한 다양한 네트워크 환경에서
     * 실제 클라이언트의 IP 주소를 반환합니다.
     *
     * @param request HttpServletRequest 객체
     * @return 클라이언트의 IP 주소
     */
    private String getClientIp(HttpServletRequest request) {
        // X-Forwarded-For 헤더를 가장 먼저 확인 (가장 표준적인 방법)
        String ip = request.getHeader("X-Forwarded-For");

        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For 헤더는 "ClientIP, Proxy1, Proxy2"와 같이 여러 IP를 포함할 수 있다.
            // 이 경우 가장 첫 번째 IP가 실제 클라이언트의 IP이다.
            return ip.split(",")[0].trim();
        }

        // 다른 프록시 관련 헤더들을 순차적으로 확인
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }

        // 위의 모든 헤더가 없는 경우, 최후의 수단으로 getRemoteAddr() 사용
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        return ip;
    }
}
