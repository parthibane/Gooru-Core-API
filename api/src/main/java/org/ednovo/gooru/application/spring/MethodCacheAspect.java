package org.ednovo.gooru.application.spring;

import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.ednovo.gooru.application.util.SerializerUtil;
import org.ednovo.gooru.core.constant.ConstantProperties;
import org.ednovo.gooru.domain.service.redis.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;

@Aspect
public class MethodCacheAspect extends SerializerUtil implements ConstantProperties{

	@Autowired
	private RedisService redisService;

	@Pointcut("execution(* org.ednovo.gooru.controllers.*.*RestController.*(..)) || " + "execution(* org.ednovo.gooru.controllers.*.*.*Rest*Controller.*(..))) ")
	public void cacheCheckPointcut() {
	}

	@AfterReturning(pointcut = "cacheCheckPointcut() && @annotation(redisCache)", returning="model")
	public void cache(JoinPoint jointPoint, RedisCache redisCache,  Object model) throws Throwable {
		String redisKey = generateKey(redisCache.key());
		if(getRedisService().getValue(redisKey) == null){
			Map<String, Object> data = ((ModelAndView) model).getModel();
			if(redisCache.ttl() != 0){
				getRedisService().putValue(redisKey, (String)data.get(MODEL), redisCache.ttl());
			}
			else{
				getRedisService().putValue(redisKey, (String)data.get(MODEL));
			}
		}
		else if(redisCache.ttl() == -1){
				getRedisService().deleteKey(redisKey);
			}
	}
	
	@Around(value = "cacheCheckPointcut() && @annotation(redisCache)")
	public Object cache(ProceedingJoinPoint pjp, RedisCache redisCache) throws Throwable{
		String redisKey = generateKey(redisCache.key());
		String data =getRedisService().getValue(redisKey);
		if(data != null){
			return toModelAndView(data);
		} 
		return pjp.proceed();
	}
	
	private String generateKey(String prefixKey){
		HttpServletRequest request = null;
		if (RequestContextHolder.getRequestAttributes() != null) {
			request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		}
		request.getMethod();
		StringBuilder redisKey = new StringBuilder(prefixKey);
		//to get the path variable
				Map<?, ?> path = (Map<?, ?>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
				Iterator<?> entries = path.entrySet().iterator();
				while (entries.hasNext()) {
				    Map.Entry entry = (Map.Entry) entries.next();
				    redisKey.append(HYPHEN).append(entry.getValue());
				}
		//get param value
		Map<String, String> parameters  = request.getParameterMap();
		for (String key : parameters.keySet()) {
		    if(!key.contains(SESSION)){
		    	redisKey.append(HYPHEN).append(request.getParameter(key));
		    }
		}
		System.out.println("Key :  "+redisKey);
		return redisKey.toString();
	}

	public RedisService getRedisService() {
		return redisService;
	}
	
}
