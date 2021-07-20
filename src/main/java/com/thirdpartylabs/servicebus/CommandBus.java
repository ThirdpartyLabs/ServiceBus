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

import com.thirdpartylabs.servicebus.commands.CommandSubscription;
import com.thirdpartylabs.servicebus.commands.ServiceBusCommand;
import com.thirdpartylabs.servicebus.commands.ServiceBusCommandExecutor;
import com.thirdpartylabs.servicebus.exceptions.ServiceBusCommandExecutorNotAssignedException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * A communication bus for decoupling commands from their executors. Executors and subscribers are registered for
 * ServiceBusCommand classes, and invoked by the bus when commands of that type are issued. Only one executor or
 * subscriber may be registered for a ServiceBusCommand type at a time. Registering a new executor or subscriber
 * for a ServiceBusCommand type will replace the old executor/subscriber with the new one.
 * <p>
 * Executors are instances of ServiceBusCommandExecutor or lambdas that are compatible with the interface that are
 * executed directly on the bus. This means that they are executed in the thread that issues the command.
 * The CommandBus is thread safe.
 * <p>
 * Subscribers are lightweight wrappers for BlockingQueues that the bus uses to issue commands to a consumer. This
 * allows multithreaded command execution. Subscribers provide options for configuring queue size and
 * blocking behavior of the bus.
 */
public class CommandBus
{
    private static final CommandBus instance = new CommandBus();

    private CommandBus()
    {
    }

    /**
     * Return the single instance of the CommandBus
     *
     * @return ServiceBus instance
     */
    public static CommandBus getInstance()
    {
        return instance;
    }

    private final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

    /*
     * Executor registry
     */
    private final Map<Class<? extends ServiceBusCommand>, ServiceBusCommandExecutor<? extends ServiceBusCommand>> executors = Collections.synchronizedMap(new HashMap<>());

    /*
     * Subscriber registry
     */
    private final Map<Class<? extends ServiceBusCommand>, CommandSubscription<? extends ServiceBusCommand>> subscribers = Collections.synchronizedMap(new HashMap<>());

    /**
     * Issue a command on the bus asynchronously
     * <p>
     * Command will be executed in a new thread, and Future with type Void a is returned. The future
     * is done when either:
     * <p>
     * A. An assigned executor returns from its execute method
     * B. The command is added to a subscriber's queue
     * C. A subscriber's queue is too full to accept the command, and the subscriber's
     * handleProducerInterruptException returns.
     * <p>
     * The primary use case for this method is issuing fire-and-forget commands. If you require synchronous
     * execution, use the issueBlocking method to issue commands to an assigned executor that completes
     * the entire task before returning from its execute method.
     *
     * @param command Command to execute
     * @param <T>     ServiceBusCommand type
     * @throws ServiceBusCommandExecutorNotAssignedException If no executor or subscriber is found
     */
    @SuppressWarnings("unchecked")
    public <T extends ServiceBusCommand> Future<Void> issue(T command) throws ServiceBusCommandExecutorNotAssignedException
    {
        // Get the class of the provided command
        Class<T> commandClass = (Class<T>) command.getClass();

        // Verify that an executor or subscriber is available
        if (!executors.containsKey(commandClass) && !subscribers.containsKey(commandClass))
        {
            throw new ServiceBusCommandExecutorNotAssignedException("No command executor assigned for command type " + commandClass.getName(), command);
        }

        return (Future<Void>) singleThreadExecutor.submit(() -> {
            try
            {
                issueBlocking(command);
            }
            catch (ServiceBusCommandExecutorNotAssignedException e)
            {
                e.printStackTrace();
            }
        });
    }

    /**
     * Issue a command on the bus
     * <p>
     * Assigned executors will be run in the caller's thread. Subscriber context is dependant upon their implementation.
     * <p>
     * The primary use case for this method is issuing synchronous commands. If you require synchronous
     * execution, use this method to issue commands to an assigned executor that completes the entire task before
     * returning from its execute method.
     *
     * @param command Command to be executed
     * @param <T>     ServiceBusCommand type
     * @throws ServiceBusCommandExecutorNotAssignedException if no executor or subscriber is found
     */
    @SuppressWarnings("unchecked")
    public <T extends ServiceBusCommand> void issueBlocking(T command) throws ServiceBusCommandExecutorNotAssignedException
    {
        // Get the class of the provided command
        Class<T> commandClass = (Class<T>) command.getClass();

        // Look for an assigned executor for the command class
        ServiceBusCommandExecutor<T> serviceBusCommandExecutor = (ServiceBusCommandExecutor<T>) executors.get(commandClass);

        boolean executorFound = false;

        // If we have an assigned executor, use it to execute the command
        if (serviceBusCommandExecutor != null)
        {
            executorFound = true;
            serviceBusCommandExecutor.execute(command);
        }
        else if (subscribers.containsKey(commandClass)) // Look for a subscription
        {
            executorFound = true;
            CommandSubscription<T> sub = (CommandSubscription<T>) subscribers.get(commandClass);

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
                        sub.getQueue().offer(command, sub.getBlockingTimeout(), sub.getBlockingTimeUnit());
                    }
                    else // No timeout, so just block until there is room in the queue
                    {
                        sub.getQueue().put(command);
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
                sub.getQueue().offer(command);
            }
        }

        // If no executor or subscription is found for the command class, throw an exception
        if (!executorFound)
        {
            throw new ServiceBusCommandExecutorNotAssignedException("No command executor assigned for command type " + commandClass.getName(), command);
        }
    }

    /**
     * Assign a command executor for a command class
     *
     * @param commandClass Command type to register the executor for
     * @param executor     Executor
     * @param <T>          ServiceBusCommandExecutor type
     */
    public <T extends ServiceBusCommand> void assignExecutor(Class<T> commandClass, ServiceBusCommandExecutor<T> executor)
    {
        subscribers.remove(commandClass);
        executors.put(commandClass, executor);
    }

    /**
     * Remove executor assignments or subscriptions for the provided ServiceBusCommand type
     *
     * @param commandClass Command type to remove assigned executors and subscribers
     * @param <T> Command type
     */
    public <T extends ServiceBusCommand> void removeExecutor(Class<T> commandClass)
    {
        executors.remove(commandClass);
        subscribers.remove(commandClass);
    }

    /**
     * Create, assign, and return a subscription for the provided ServiceBusCommand class
     * <p>
     * Subscriptions are intended for use in multithreaded applications where executors should not be run in the
     * thread of the command issuer.
     *
     * @param commandClass class to register subscriber for
     * @param <T>          ServiceBusCommand type
     * @return Subscription
     */
    public <T extends ServiceBusCommand> CommandSubscription<T> subscribe(Class<T> commandClass)
    {
        CommandSubscription<T> subscription = new CommandSubscription<>(commandClass);

        removeExecutor(commandClass);
        subscribers.put(commandClass, subscription);

        return subscription;
    }

    /**
     * Register an existing CommandSubscription
     *
     * @param subscription Subscription to register
     * @param <T>          ServiceBusCommand type
     */
    public <T extends ServiceBusCommand> void subscribe(CommandSubscription<T> subscription)
    {
        removeExecutor(subscription.getCommandClass());
        subscribers.put(subscription.getCommandClass(), subscription);
    }

    /**
     * Remove the provided subscription from the bus
     *
     * @param subscription Subscription to remove from the bus
     * @param <T> Command type
     */
    public <T extends ServiceBusCommand> void unsubscribe(CommandSubscription<T> subscription)
    {
        subscribers.remove(subscription.getCommandClass());
    }

    /**
     * Shut down the thread executor
     */
    public void shutdown()
    {
        singleThreadExecutor.shutdown();
    }
}
