package io.fathom.cloud.lifecycle;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Scope;
import com.google.inject.Scopes;
import com.google.inject.spi.BindingScopingVisitor;

@Singleton
public class Lifecycle {

    private static final Logger log = LoggerFactory.getLogger(Lifecycle.class);

    @Inject
    Injector injector;

    List<LifecycleListener> listeners;

    synchronized List<LifecycleListener> getListeners() {
        if (listeners == null) {
            List<LifecycleListener> listeners = Lists.newArrayList();
            for (Map.Entry<Key<?>, Binding<?>> entry : injector.getAllBindings().entrySet()) {
                // final Binding<?> binding = entry.getValue();

                // Object instance = entry.getValue().getProvider().get();

                BindingScopingVisitor<Boolean> visitor = new IsSingletonBindingScopingVisitor();
                Map<Key<?>, Binding<?>> bindings = injector.getAllBindings();
                for (Binding<?> binding : bindings.values()) {
                    Key<?> key = binding.getKey();
                    // log.debug("Checking binding " + key);

                    Boolean foundSingleton = binding.acceptScopingVisitor(visitor);
                    if (foundSingleton) {
                        Object instance = injector.getInstance(key);
                        // log.debug("\tsingleton: " + instance);

                        if (instance instanceof LifecycleListener) {
                            if (listeners.contains(instance)) {
                                continue;
                            }

                            log.debug("Found binding " + key);
                            log.debug("Found lifecycle listener: {}", instance.getClass());

                            listeners.add((LifecycleListener) instance);
                        }
                    }
                }

                // binding.acceptScopingVisitor(new
                // DefaultBindingScopingVisitor<Void>() {
                // @Override
                // public Void visitEagerSingleton() {
                // Object instance = binding.getProvider().get();
                // foundSingleton(instance);
                // return null;
                // }
                //
                // @Override
                // public Void visitScopeAnnotation(Class<? extends Annotation>
                // scopeAnnotation) {
                // return super.visitScopeAnnotation(scopeAnnotation);
                // }
                //
                // @Override
                // protected Void visitOther() {
                // return super.visitOther();
                // }
                //
                // @Override
                // public Void visitScope(Scope scope) {
                // return super.visitScope(scope);
                // }
                //
                // @Override
                // public Void visitNoScoping() {
                // return super.visitNoScoping();
                // }
                // });

            }
            this.listeners = listeners;
        }
        return listeners;
    }

    static class IsSingletonBindingScopingVisitor implements BindingScopingVisitor<Boolean> {
        @Override
        public Boolean visitEagerSingleton() {
            // log.debug("\tfound eager singleton");
            return Boolean.TRUE;
        }

        @Override
        public Boolean visitScope(Scope scope) {
            // log.debug("\t scope: " + scope);
            return scope == Scopes.SINGLETON;
        }

        @Override
        public Boolean visitScopeAnnotation(Class<? extends Annotation> scopeAnnotation) {
            // log.debug("\t scope annotation: " + scopeAnnotation);
            return scopeAnnotation == Singleton.class;
        }

        @Override
        public Boolean visitNoScoping() {
            return Boolean.FALSE;
        }
    }

    public void start() throws Exception {
        for (LifecycleListener listener : getListeners()) {
            listener.start();
        }
    }
}
