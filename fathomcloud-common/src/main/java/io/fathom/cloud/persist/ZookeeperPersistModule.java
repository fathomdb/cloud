package io.fathom.cloud.persist;

import org.aopalliance.intercept.MethodInterceptor;

import com.google.inject.Singleton;
import com.google.inject.persist.PersistModule;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.UnitOfWork;

public final class ZookeeperPersistModule extends PersistModule {
    // private final String jpaUnit;
    //
    // public ZookeeperPersistModule(String jpaUnit) {
    // Preconditions.checkArgument(null != jpaUnit && jpaUnit.length() > 0,
    // "JPA unit name must be a non-empty string.");
    // this.jpaUnit = jpaUnit;
    // }

    // private Properties properties;
    private MethodInterceptor transactionInterceptor;

    @Override
    protected void configurePersistence() {
        // bindConstant().annotatedWith(Jpa.class).to(jpaUnit);
        //
        // if (null != properties) {
        // bind(Properties.class).annotatedWith(Jpa.class).toInstance(
        // properties);
        // } else {
        // bind(Properties.class).annotatedWith(Jpa.class).toProvider(
        // Providers.<Properties> of(null));
        // }

        bind(ZookeeperPersistService.class).in(Singleton.class);

        bind(PersistService.class).to(ZookeeperPersistService.class);
        bind(UnitOfWork.class).to(ZookeeperPersistService.class);
        bind(ZookeeperEntityManager.class).toProvider(ZookeeperPersistService.class);
        bind(ZookeeperEntityManagerFactory.class)
                .toProvider(ZookeeperPersistService.EntityManagerFactoryProvider.class);

        transactionInterceptor = new ZookeeperLocalTxnInterceptor();
        requestInjection(transactionInterceptor);

        // // Bind dynamic finders.
        // for (Class<?> finder : dynamicFinders) {
        // bindFinder(finder);
        // }
    }

    @Override
    protected MethodInterceptor getTransactionInterceptor() {
        return transactionInterceptor;
    }

    // /**
    // * Configures the JPA persistence provider with a set of properties.
    // *
    // * @param properties
    // * A set of name value pairs that configure a JPA persistence
    // * provider as per the specification.
    // */
    // public JpaPersistModule properties(Properties properties) {
    // this.properties = properties;
    // return this;
    // }

    // private final List<Class<?>> dynamicFinders = Lists.newArrayList();

    // /**
    // * Adds an interface to this module to use as a dynamic finder.
    // *
    // * @param iface
    // * Any interface type whose methods are all dynamic finders.
    // */
    // public <T> JpaPersistModule addFinder(Class<T> iface) {
    // dynamicFinders.add(iface);
    // return this;
    // }
    //
    // private <T> void bindFinder(Class<T> iface) {
    // if (!isDynamicFinderValid(iface)) {
    // return;
    // }
    //
    // InvocationHandler finderInvoker = new InvocationHandler() {
    // @Inject
    // JpaFinderProxy finderProxy;
    //
    // @Override
    // public Object invoke(final Object thisObject, final Method method,
    // final Object[] args) throws Throwable {
    //
    // // Don't intercept non-finder methods like equals and hashcode.
    // if (!method.isAnnotationPresent(Finder.class)) {
    // // NOTE(dhanji): This is not ideal, we are using the
    // // invocation handler's equals
    // // and hashcode as a proxy (!) for the proxy's equals and
    // // hashcode.
    // return method.invoke(this, args);
    // }
    //
    // return finderProxy.invoke(new MethodInvocation() {
    // @Override
    // public Method getMethod() {
    // return method;
    // }
    //
    // @Override
    // public Object[] getArguments() {
    // return null == args ? new Object[0] : args;
    // }
    //
    // @Override
    // public Object proceed() throws Throwable {
    // return method.invoke(thisObject, args);
    // }
    //
    // @Override
    // public Object getThis() {
    // throw new UnsupportedOperationException(
    // "Bottomless proxies don't expose a this.");
    // }
    //
    // @Override
    // public AccessibleObject getStaticPart() {
    // throw new UnsupportedOperationException();
    // }
    // });
    // }
    // };
    // requestInjection(finderInvoker);
    //
    // @SuppressWarnings("unchecked")
    // // Proxy must produce instance of type given.
    // T proxy = (T) Proxy.newProxyInstance(Thread.currentThread()
    // .getContextClassLoader(), new Class<?>[] { iface },
    // finderInvoker);
    //
    // bind(iface).toInstance(proxy);
    // }
    //
    // private boolean isDynamicFinderValid(Class<?> iface) {
    // boolean valid = true;
    // if (!iface.isInterface()) {
    // addError(iface
    // + " is not an interface. Dynamic Finders must be interfaces.");
    // valid = false;
    // }
    //
    // for (Method method : iface.getMethods()) {
    // DynamicFinder finder = DynamicFinder.from(method);
    // if (null == finder) {
    // addError("Dynamic Finder methods must be annotated with @Finder, but "
    // + iface + "." + method.getName() + " was not");
    // valid = false;
    // }
    // }
    // return valid;
    // }
}
