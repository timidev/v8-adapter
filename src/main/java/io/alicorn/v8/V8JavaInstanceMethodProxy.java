package io.alicorn.v8;

import com.eclipsesource.v8.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Proxies an instance method of a Java class and makes it callable from the V8 context.
 *
 * @author Brandon Sanders [brandon@alicorn.io]
 */
final class V8JavaInstanceMethodProxy extends V8JavaMethodProxy {

    // Cache for the V8 runtime this proxy exists on.
    private final V8JavaCache cache;

    public V8JavaInstanceMethodProxy(String name, V8JavaCache cache) {
        super(name);
        this.cache = cache;
    }

    public JavaCallback getCallbackForInstance(final Object o) {
        return new JavaCallback() {
            @Override public Object invoke(V8Object receiver, V8Array parameters) {
                //See if a method exists.
                Object[] coercedArguments = null;
                Method coercedMethod = null;
                for (Method method : getMethodSignatures()) {
                    try {
                        coercedArguments = V8JavaObjectUtils.translateJavascriptArgumentsToJava(method.isVarArgs(), method.getParameterTypes(), parameters, receiver, cache);
                        coercedMethod = method;
                        break;
                    } catch (IllegalArgumentException e) {
                        //TODO: Exception to manage flow here is abysmal. Some critical information is being ignored which is unacceptable.
                    }
                }

                if (coercedArguments == null) {
                    StringBuilder errorMessage = new StringBuilder("No signature exists for ");
                    errorMessage.append(getMethodName());
                    errorMessage.append(" with parameters [");
                    for (int i = 0; i < parameters.length(); i++) {
                        Object obj = parameters.get(i);
                        errorMessage.append(String.valueOf(parameters.get(i))).append(", ");
                        if (obj instanceof V8Value) {
                            ((V8Value) obj).release();
                        }
                    }
                    errorMessage.append("].");
                    throw new IllegalArgumentException(errorMessage.toString());
                }

                //Invoke the method.
                try {
                    return V8JavaObjectUtils.translateJavaArgumentToJavascript(coercedMethod.invoke(o, coercedArguments), V8JavaObjectUtils.getRuntimeSarcastically(receiver), cache);
                } catch (IllegalAccessException e) {
                    throw new IllegalArgumentException("Method received invalid arguments [" + e.getMessage() + "]!");
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e.getCause());
                }
            }
        };
    }
}