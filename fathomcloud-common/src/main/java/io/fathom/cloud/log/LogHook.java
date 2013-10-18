package io.fathom.cloud.log;

import java.io.IOException;
import java.util.List;

import com.google.common.collect.Lists;

public abstract class LogHook {
    public static class Hooks {
        final List<LogHook> hooks = Lists.newArrayList();
    }

    static final ThreadLocal<Hooks> HOOKS = new ThreadLocal<Hooks>() {
        @Override
        protected Hooks initialValue() {
            return new Hooks();
        }
    };

    static List<LogHook> getHooks() {
        Hooks hooks = HOOKS.get();
        return hooks.hooks;
    }

    static void addHook(LogHook hook) {
        Hooks hooks = HOOKS.get();
        hooks.hooks.add(hook);
    }

    static void removeHook(LogHook hook) {
        Hooks hooks = HOOKS.get();
        hooks.hooks.remove(hook);
    }

    public void install() {
        addHook(this);
    }

    public void remove() {
        removeHook(this);
    }

    public abstract void log(String loggerName, String message, List<String[]> exceptionStacks, int levelInt)
            throws IOException;
}
