/*
 * ServiceBus inter-component communication bus
 *
 * Copyright (c) 2021- Rob Ruchte, rob@thirdpartylabs.com
 *
 * Licensed under the License specified in file LICENSE, included with the source code.
 * You may not use this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thirdpartylabs.servicebus;

import com.thirdpartylabs.servicebus.events.EventSubscription;
import com.thirdpartylabs.servicebus.events.ServiceBusEvent;
import com.thirdpartylabs.servicebus.events.ServiceBusEventListener;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A communication bus for decoupling events from their listeners. Listeners and subscribers are registered for
 * ServiceBusEvent classes, and invoked by the bus when events of that type are emitted. There si no limit to the
 * number of listeners or subscribers that may be registered for a ServiceBusEvent type. Events will be delivered to
 * all configured listeners and subscribers of the given type.
 * <p>
 * Listeners are instances of ServiceBusEventListener or lambdas that are compatible with the interface that are
 * executed directly on the bus. This means that they are executed in the thread that emits the event.
 * The EventBus is thread safe.
 * <p>
 * Subscribers are lightweight wrappers for BlockingQueues that the bus uses to emit events to consumers. This
 * allows multithreaded event propagation. Subscribers provide options for configuring queue size and
 * blocking behavior of the bus.
 */
public class EventBus
{
    private static final EventBus instance = new EventBus();

    private EventBus()
    {
    }

    /**
     * Return the single instance of the EventBus
     *
     * @return ServiceBus instance
     */
    public static EventBus getInstance()
    {
        return instance;
    }

    private final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

    /*
     * Listener registry
     */
    private final Map<Class<? extends ServiceBusEvent>, Set<ServiceBusEventListener<? extends ServiceBusEvent>>> listeners = Collections.synchronizedMap(new HashMap<>());

    /*
     * Subscriber registry
     */
    private final Map<Class<? extends ServiceBusEvent>, Set<EventSubscription<? extends ServiceBusEvent>>> subscribers = Collections.synchronizedMap(new HashMap<>());

    /**
     * Emit an event on the bus
     *
     * @param serviceBusEvent Event to be emitted
     * @param <T>             ServiceBusEvent type
     */
    public <T extends ServiceBusEvent> void emitAsync(T serviceBusEvent)
    {
        singleThreadExecutor.submit(() -> emit(serviceBusEvent));
    }

    /**
     * Emit an event on the bus - potentially blocking
     *
     * @param serviceBusEvent Event to be emitted
     * @param <T>             ServiceBusEvent type
     */
    @SuppressWarnings("unchecked")
    public <T extends ServiceBusEvent> void emit(T serviceBusEvent)
    {
        // Get the class of the provided event
        Class<T> eventClass = (Class<T>) serviceBusEvent.getClass();

        // Look for listeners for the provided class
        Set<ServiceBusEventListener<? extends ServiceBusEvent>> serviceBusEventListeners = listeners.get(eventClass);

        if (serviceBusEventListeners != null)
        {
            // Execute each of the configured event handlers
            for (ServiceBusEventListener<? extends ServiceBusEvent> currServiceBusEventListener : serviceBusEventListeners)
            {
                ServiceBusEventListener<T> myListener = (ServiceBusEventListener<T>) currServiceBusEventListener;
                myListener.handle(serviceBusEvent);
            }
        }

        // Look for subscribers for the provided class
        Set<EventSubscription<? extends ServiceBusEvent>> serviceBusEventSubscribers = subscribers.get(eventClass);

        if (serviceBusEventSubscribers != null)
        {
            // Push the event onto each of the subscriber queues
            for (EventSubscription<? extends ServiceBusEvent> currSubscriber : serviceBusEventSubscribers)
            {
                EventSubscription<T> sub = (EventSubscription<T>) currSubscriber;

                // If the subscription is configured for blocking on the producer side, determine how to proceed
                if (sub.isBlocking())
                {
                    try
                    {
                        /*
                            If a timeout is configured, offer the command to the queue with the timeout parameters
                            from the subscription
                         */
                        if (sub.getBlockingTimeout() > 0)
                        {
                            sub.getQueue().offer(serviceBusEvent, sub.getBlockingTimeout(), sub.getBlockingTimeUnit());
                        }
                        else // No timeout, so just block until there is room in the queue
                        {
                            sub.getQueue().put(serviceBusEvent);
                        }
                    }
                    catch (InterruptedException e)
                    {
                        /*
                            If we are interrupted whilst blocking, delegate the exception to the
                            handleProducerInterruptException method on the subscription.
                         */
                        sub.handleProducerInterruptException(e);
                    }
                }
                else // Not blocking, simply attempt to add the command to the subscription queue
                {
                    sub.getQueue().offer(serviceBusEvent);
                }
            }
        }
    }

    /**
     * Register a listener for an event class
     *
     * @param eventClass Event type to listen for
     * @param listener   Listener
     * @param <T>        ServiceBusEvent type
     */
    public <T extends ServiceBusEvent> void listenFor(Class<T> eventClass, ServiceBusEventListener<T> listener)
    {
        if (!listeners.containsKey(eventClass))
        {
            listeners.put(eventClass, Collections.synchronizedSet(new HashSet<>()));
        }

        listeners.get(eventClass).add(listener);
    }

    /**
     * Remove the listener for the specified event
     *
     * @param eventClass Event type to listen for
     * @param listener   Listener
     * @param <T>        ServiceBusEvent type
     */
    public <T extends ServiceBusEvent> void removeListener(Class<T> eventClass, ServiceBusEventListener<T> listener)
    {
        if (listeners.containsKey(eventClass))
        {
            listeners.get(eventClass).remove(listener);
        }
    }

    /**
     * Remove all listeners and subscribers for the specified event type
     *
     * @param eventClass Event type to listen for
     * @param <T>        ServiceBusEvent type
     */
    public <T extends ServiceBusEvent> void removeListener(Class<T> eventClass)
    {
        listeners.remove(eventClass);
        subscribers.remove(eventClass);
    }

    public <T extends ServiceBusEvent> EventSubscription<T> subscribe(Class<T> eventClass)
    {
        EventSubscription<T> subscription = new EventSubscription<>(eventClass);

        if (!subscribers.containsKey(eventClass))
        {
            subscribers.put(eventClass, Collections.synchronizedSet(new HashSet<>()));
        }

        subscribers.get(eventClass).add(subscription);

        return subscription;
    }

    /**
     * Create, assign, and return a subscription for the provided ServiceBusEvent class
     * <p>
     * Subscriptions are intended for use in multithreaded applications where listeners should not be run in the
     * thread of the event emitter.
     *
     * @param subscription Event subscription
     * @param <T> Subscription type
     */
    public <T extends ServiceBusEvent> void subscribe(EventSubscription<T> subscription)
    {
        if (!subscribers.containsKey(subscription.getEventClass()))
        {
            subscribers.put(subscription.getEventClass(), Collections.synchronizedSet(new HashSet<>()));
        }

        subscribers.get(subscription.getEventClass()).add(subscription);
    }

    /**
     * Remove the provided subscription from the bus
     *
     * @param subscription Event subscription
     * @param <T> Subscription type
     */
    public <T extends ServiceBusEvent> void unsubscribe(EventSubscription<T> subscription)
    {
        if (subscribers.containsKey(subscription.getEventClass()))
        {
            subscribers.get(subscription.getEventClass()).remove(subscription);
        }
    }

    /**
     * Shut down the thread executor
     */
    public void shutdown()
    {
        singleThreadExecutor.shutdown();
    }
}
