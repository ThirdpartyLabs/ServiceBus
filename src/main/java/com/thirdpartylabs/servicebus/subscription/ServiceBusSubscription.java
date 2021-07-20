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
package com.thirdpartylabs.servicebus.subscription;

import java.util.concurrent.TimeUnit;

/**
 * Base class for subscriptions that manages blocking options that the bus will use when sending items to subscribers
 */
public abstract class ServiceBusSubscription
{
    protected boolean blocking = false;
    protected TimeUnit blockingTimeUnit = TimeUnit.MILLISECONDS;
    protected long blockingTimeout = 100;

    /**
     * @return Should the bus block if the queue is full?
     */
    public boolean isBlocking()
    {
        return blocking;
    }

    /**
     * @param blocking Tell the bus to block if the queue is full
     */
    public void setBlocking(boolean blocking)
    {
        this.blocking = blocking;
    }

    /**
     * @return Unit ot time for timeout.
     */
    public TimeUnit getBlockingTimeUnit()
    {
        return blockingTimeUnit;
    }

    /**
     * @param blockingTimeUnit Unit ot time for timeout.
     */
    public void setBlockingTimeUnit(TimeUnit blockingTimeUnit)
    {
        this.blockingTimeUnit = blockingTimeUnit;
    }

    /**
     * @return Time to block if queue is full. Items are dropped if timeout occurs.
     */
    public long getBlockingTimeout()
    {
        return blockingTimeout;
    }

    /**
     * @param blockingTimeout Time to block if queue is full. Items are dropped if timeout occurs.
     */
    public void setBlockingTimeout(long blockingTimeout)
    {
        this.blockingTimeout = blockingTimeout;
    }

    /**
     * Called if an InterruptException occurs while the bus is sending data on the queue. Defaults to noop.
     * Override to do something useful.
     *
     * @param e the exception to handle
     */
    public void handleProducerInterruptException(InterruptedException e)
    {
    }
}
