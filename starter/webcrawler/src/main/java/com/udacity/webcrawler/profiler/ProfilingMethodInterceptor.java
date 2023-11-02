package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 * @param <T>
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

  private final Clock clock;
  private final ProfilingState state;
  private final Object delegate;

  // TODO: You will need to add more instance fields and constructor arguments to this class.
  ProfilingMethodInterceptor(Clock clock, ProfilingState state, Object delegate) {
	  this.clock = Objects.requireNonNull(clock);
	  this.delegate = delegate;
	  this.state = state;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
	  
        // Invoke toString() and hashCode() directly on the property Map.
	  // change the positon of this code for get time initial
	  Instant startTime = clock.instant();
        try {
          return method.invoke(delegate, args); // object target
        } catch (InvocationTargetException e) {
          throw e.getTargetException();
        } catch (IllegalAccessException e) {
          throw new RuntimeException(e);
        } finally {
        	
        	if (method.isAnnotationPresent(Profiled.class)){
        		// get type duration
                Duration durationTime = Duration.between(startTime, clock.instant());
                state.record(delegate.getClass(), method, durationTime);
            }
		}  
	}
}
