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

package com.thirdpartylabs.servicebus.events;

import com.thirdpartylabs.servicebus.subscription.ServiceBusSubscription;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * An EventSubscription is registered with the EventBus for the specified event class. The subscription contains
 * a BlockingQueue that the EventBus uses to emit events of the specified type. The subscription allows
 * configuration options for the blocking behavior on the bus side.
 * <p>
 * Multiple subscriptions and listeners may be registered for each event type.
 *
 * @param <T>
 */
public class EventSubscription<T extends ServiceBusEvent> extends ServiceBusSubscription
{
    private final Class<T> eventClass;
    private final BlockingQueue<T> queue;


    /**
     * @param eventClass Event type to listen for on the bus
     */
    public EventSubscription(Class<T> eventClass)
    {
        this.eventClass = eventClass;
        this.queue = new LinkedBlockingQueue<>();
    }

    /**
     * @param eventClass Event type to listen for on the bus
     * @param queueSize  Maximum number of events to hold in the queue
     */
    public EventSubscription(Class<T> eventClass, int queueSize)
    {
        this.eventClass = eventClass;
        this.queue = new LinkedBlockingQueue<>(queueSize);
    }

    /**
     * @param eventClass Event type to listen for on the bus
     * @param queueSize  Maximum number of events to hold in the queue
     * @param blocking   Whether or not to block if queue is full. If queue is full, events are dropped. Defaults to false.
     */
    public EventSubscription(Class<T> eventClass, int queueSize, boolean blocking)
    {
        this.eventClass = eventClass;
        this.blocking = blocking;
        this.queue = new LinkedBlockingQueue<>(queueSize);
    }

    /**
     * @param eventClass      Event type to listen for on the bus
     * @param queueSize       Maximum number of events to hold in the queue
     * @param blockingTimeout Time in milliseconds to block if queue is full. Events are dropped if timeout occurs.
     */
    public EventSubscription(Class<T> eventClass, int queueSize, long blockingTimeout)
    {
        this.eventClass = eventClass;
        this.blocking = true;
        this.blockingTimeout = blockingTimeout;
        this.queue = new LinkedBlockingQueue<>(queueSize);
    }

    /**
     * @param eventClass       Event type to listen for on the bus
     * @param queueSize        Maximum number of events to hold in the queue
     * @param blockingTimeout  Time to block if queue is full. Events are dropped if timeout occurs.
     * @param blockingTimeUnit Unit ot time for timeout.
     */
    public EventSubscription(Class<T> eventClass, int queueSize, long blockingTimeout, TimeUnit blockingTimeUnit)
    {
        this.eventClass = eventClass;
        this.blocking = true;
        this.blockingTimeout = blockingTimeout;
        this.blockingTimeUnit = blockingTimeUnit;
        this.queue = new LinkedBlockingQueue<>(queueSize);
    }

    /**
     * @return Event type that the subscription is registered for
     */
    public Class<T> getEventClass()
    {
        return eventClass;
    }

    /**
     * @return The queue used for communication from the bus to the consumer
     */
    public BlockingQueue<T> getQueue()
    {
        return queue;
    }
}
