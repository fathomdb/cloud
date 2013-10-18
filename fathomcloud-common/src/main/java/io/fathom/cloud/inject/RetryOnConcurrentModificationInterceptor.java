package io.fathom.cloud.inject;

//
//import java.lang.reflect.Method;
//import java.util.ConcurrentModificationException;
//
//import org.aopalliance.intercept.MethodInterceptor;
//import org.aopalliance.intercept.MethodInvocation;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//public class RetryOnConcurrentModificationInterceptor implements
//		MethodInterceptor {
//	private static final Logger log = LoggerFactory
//			.getLogger(RetryOnConcurrentModificationInterceptor.class);
//
//	@Override
//	public Object invoke(final MethodInvocation invocation) throws Throwable {
//		final Method method = invocation.getMethod();
//
//		RetryOnConcurrentModification annotation = method
//				.getAnnotation(RetryOnConcurrentModification.class);
//
//		int attempt = 0;
//
//		while (true) {
//			attempt++;
//			try {
//				Object ret = invocation.proceed();
//				return ret;
//			} catch (ConcurrentModificationException e) {
//				boolean retry = false;
//				if (attempt < annotation.maxAttempts()) {
//					retry = true;
//				}
//				if (!retry) {
//					// throw new CloudException(
//					// "Unable to update due to concurrent modification",
//					// e);
//					throw e;
//				}
//			}
//		}
//	}
// }