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

package com.thirdpartylabs.servicebus.commands;

import com.thirdpartylabs.servicebus.subscription.ServiceBusSubscription;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A CommandSubscription is registered with the CommandBus for the specified command class. The subscription contains
 * a BlockingQueue that the CommandBus uses to issue commands of the specified type. The subscription allows
 * configuration options for the blocking behavior on the bus side.
 * <p>
 * Only one subscription or executor is allowed to be registered for each command class. If an executor or a different
 * subscription are registered after an instance has been registered, that instance will no longer receive commands.
 *
 * @param <T>
 */
public class CommandSubscription<T extends ServiceBusCommand> extends ServiceBusSubscription
{
    private final Class<T> commandClass;
    private final BlockingQueue<T> queue;

    /**
     * @param commandClass Command to register on the bus
     */
    public CommandSubscription(Class<T> commandClass)
    {
        this.commandClass = commandClass;
        this.queue = new LinkedBlockingQueue<>();
    }

    /**
     * @param commandClass Command to register on the bus
     * @param queueSize    Maximum number of commands to hold in the queue
     */
    public CommandSubscription(Class<T> commandClass, int queueSize)
    {
        this.commandClass = commandClass;
        this.queue = new LinkedBlockingQueue<>(queueSize);
    }

    /**
     * @param commandClass Command to register on the bus
     * @param queueSize    Maximum number of commands to hold in the queue
     * @param blocking     Whether or not to block if queue is full. If queue is full, commands are dropped. Defaults to false.
     */
    public CommandSubscription(Class<T> commandClass, int queueSize, boolean blocking)
    {
        this.commandClass = commandClass;
        this.blocking = blocking;
        this.queue = new LinkedBlockingQueue<>(queueSize);
    }

    /**
     * @param commandClass    Command to register on the bus
     * @param queueSize       Maximum number of commands to hold in the queue
     * @param blockingTimeout Time in milliseconds to block if queue is full. Commands are dropped if timeout occurs.
     */
    public CommandSubscription(Class<T> commandClass, int queueSize, long blockingTimeout)
    {
        this.commandClass = commandClass;
        this.blocking = true;
        this.blockingTimeout = blockingTimeout;
        this.queue = new LinkedBlockingQueue<>(queueSize);
    }

    /**
     * @param commandClass     Command to register on the bus
     * @param queueSize        Maximum number of commands to hold in the queue
     * @param blockingTimeout  Time to block if queue is full. Commands are dropped if timeout occurs.
     * @param blockingTimeUnit Unit ot time for timeout.
     */
    public CommandSubscription(Class<T> commandClass, int queueSize, long blockingTimeout, TimeUnit blockingTimeUnit)
    {
        this.commandClass = commandClass;
        this.blocking = true;
        this.blockingTimeout = blockingTimeout;
        this.blockingTimeUnit = blockingTimeUnit;
        this.queue = new LinkedBlockingQueue<>(queueSize);
    }

    /**
     * @return Command class for subscription
     */
    public Class<T> getCommandClass()
    {
        return commandClass;
    }

    /**
     * @return The queue used for communication from the bus to the consumer
     */
    public BlockingQueue<T> getQueue()
    {
        return queue;
    }
}
