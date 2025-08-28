package com.shared.shared_starter_ratelimit;
import com.common.context.ContextManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import jakarta.servlet.*; import jakarta.servlet.http.*; import java.io.IOException; import java.time.Duration;
public class RateLimitFilter implements Filter {
  private final StringRedisTemplate redis; private final RateLimitProps props;
  public RateLimitFilter(StringRedisTemplate r, RateLimitProps p){ this.redis=r; this.props=p; }
  @Override public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest)request; HttpServletResponse resp=(HttpServletResponse)response;
    String key = keyFor(req);
    String bucket = "rl:"+key;
    Long current = redis.opsForValue().increment(bucket);
    if (current!=null && current==1) redis.expire(bucket, Duration.ofMinutes(1));
    int cap = props.getCapacity();
    resp.setHeader("X-RateLimit-Limit", String.valueOf(cap));
    resp.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, cap-(current==null?0:current))));
    if (current!=null && current>cap){ resp.setStatus(429); resp.getWriter().write("Rate limit exceeded"); return; }
    chain.doFilter(request, response);
  }
  private String keyFor(HttpServletRequest req){
    return switch (props.getKeyStrategy()) {
      case "ip" -> req.getRemoteAddr();
      case "user" -> req.getUserPrincipal()!=null? req.getUserPrincipal().getName() : "anon";
      default -> ContextManager.Tenant.get()!=null? ContextManager.Tenant.get(): "public";
    };
  }
}
