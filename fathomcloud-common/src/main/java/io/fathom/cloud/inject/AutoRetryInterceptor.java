package io.fathom.cloud.inject;

//
//import java.lang.reflect.Method;
//
//import org.aopalliance.intercept.MethodInterceptor;
//import org.aopalliance.intercept.MethodInvocation;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//public class AutoRetryInterceptor implements MethodInterceptor {
//	private static final Logger log = LoggerFactory
//			.getLogger(AutoRetryInterceptor.class);
//
//	@Override
//	public Object invoke(final MethodInvocation invocation) throws Throwable {
//		final Method method = invocation.getMethod();
//
//		AutoRetry annotation = method.getAnnotation(AutoRetry.class);
//
//		int attempt = 0;
//
//		while (true) {
//			attempt++;
//			try {
//				Object ret = invocation.proceed();
//				return ret;
//			} catch (Throwable t) {
//				boolean retry = false;
//				if (attempt < annotation.maxAttempts()) {
//					for (Class<?> e : annotation.value()) {
//						if (e.isInstance(t)) {
//							retry = true;
//							break;
//						}
//					}
//					if (!retry) {
//						throw t;
//					}
//				}
//			}
//		}
//	}
// }