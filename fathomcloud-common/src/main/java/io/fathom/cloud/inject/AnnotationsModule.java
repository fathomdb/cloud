package io.fathom.cloud.inject;

//
//import com.google.inject.AbstractModule;
//import com.google.inject.matcher.Matchers;
//
//public class AnnotationsModule extends AbstractModule {
//
//	@Override
//	protected void configure() {
//		// {
//		// AutoRetryInterceptor interceptor = new AutoRetryInterceptor();
//		// bindInterceptor(Matchers.any(),
//		// Matchers.annotatedWith(AutoRetry.class),
//		// interceptor);
//		// }
//
//		{
//			RetryOnConcurrentModificationInterceptor interceptor = new RetryOnConcurrentModificationInterceptor();
//			bindInterceptor(
//					Matchers.any(),
//					Matchers.annotatedWith(RetryOnConcurrentModification.class),
//					interceptor);
//		}
//	}
// }