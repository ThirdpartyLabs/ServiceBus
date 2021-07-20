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
package com.thirdpartylabs.servicebus.threadtest;

import com.thirdpartylabs.servicebus.ServiceBus;
import com.thirdpartylabs.servicebus.commands.ChangeWombatVisibilityServiceBusCommand;
import com.thirdpartylabs.servicebus.commands.CommandSubscription;

/**
 * Manager for the pooled command executor threaded bus test
 * <p>
 * The specified number of command executors and generators will be created, and the generators will perform
 * the specified number of iterations.
 * <p>
 * The executors will all operate on the queue provided by a subscription, each working in its own thread. Results
 * from the operations will be stored for analysis. The unit test should compare the expected results to the actual
 * results produced.
 */
public class ExecutorPoolTestController
{
    private final EventObserver eventObserver;

    private final GeneratorWorker[] generators;
    private final CommandExecutorPoolWorker[] commandExecutorWorkers;

    // Number of generators to spawn
    private final int generatorCount;

    // Number of responders to spawn
    private final int executorCount;

    // Number of times the generators should perform their operations
    private final int iterations;

    /**
     * @param generatorCount Number of generators to spawn
     * @param executorCount Number of command executor threads to spawn
     * @param iterations Number of times the generator should perform its operations
     */
    public ExecutorPoolTestController(int generatorCount, int executorCount, int iterations)
    {
        this.generatorCount = generatorCount;
        this.executorCount = executorCount;
        this.iterations = iterations;

        // The event observer stays in this thread and listens to all of the events
        eventObserver = new EventObserver();

        //Set up a subscription
        CommandSubscription<ChangeWombatVisibilityServiceBusCommand> subscription = ServiceBus.getInstance().commands.subscribe(ChangeWombatVisibilityServiceBusCommand.class);

        // These workers execute commands received from the CommandBus for the subscription
        commandExecutorWorkers = new CommandExecutorPoolWorker[executorCount];
        for (int i = 0; i < executorCount; i++)
        {
            commandExecutorWorkers[i] = new CommandExecutorPoolWorker(subscription);
        }

        // These workers generate traffic on the bus - configure them to send commands only for this test
        generators = new GeneratorWorker[generatorCount];
        for (int i = 0; i < generatorCount; i++)
        {
            generators[i] = new GeneratorWorker(iterations, true, false, false);
        }
    }

    /**
     * Run the workers
     */
    public void run()
    {
        // Start up the executors
        for (CommandExecutorPoolWorker currWorker : commandExecutorWorkers)
        {
            currWorker.start();
        }

        // Fire it up!
        for (int i = 0; i < generatorCount; i++)
        {
            generators[i].start();
        }

        // Block this thead until the generators set their complete flag and the workers finish their queues
        boolean generatorsComplete = false;
        boolean executorsComplete = false;
        while (!generatorsComplete || !executorsComplete)
        {
            try
            {
                // Iterate through the generators, if any are still working, set the complete flag false
                generatorsComplete = true;
                for (int i = 0; i < generatorCount; i++)
                {
                    if (!generators[i].isComplete())
                    {
                        generatorsComplete = false;
                        break;
                    }
                }

                // Iterate through the listeners, if any are still working, set the complete flag false
                executorsComplete = true;
                for (CommandExecutorPoolWorker currWorker : commandExecutorWorkers)
                {
                    if (currWorker.isWorking())
                    {
                        executorsComplete = false;
                        break;
                    }
                }

                // If the listeners are still working sleep a while
                if (!generatorsComplete || !executorsComplete)
                {
                    Thread.sleep(100);
                }

                Thread.yield();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }

        // Stop the workers
        for (CommandExecutorPoolWorker currWorker : commandExecutorWorkers)
        {
            currWorker.stop();
        }
    }

    /**
     * @return The number of iterations specified for the generator
     */
    public int getIterations()
    {
        return iterations;
    }

    public int getGeneratorCount()
    {
        return generatorCount;
    }

    /**
     * @return The expected number of items generated for each type
     */
    public int getExpectedItemCount()
    {
        return iterations * generatorCount;
    }

    /**
     * @return The expected number of generated events
     */
    public int getExpectedEventCount()
    {
        return iterations * generatorCount;
    }

    /**
     * @return The number of events that the observer received
     */
    public int getObserverEventCount()
    {
        return eventObserver.getReceivedEvents().size();
    }

    /**
     * @return The array of flags received by the command executor
     */
    public boolean[] getReceivedVisibilityFlags()
    {
        int total = 0;
        for (CommandExecutorPoolWorker currWorker : commandExecutorWorkers)
        {
            total += currWorker.getVisibilityFlags().length;
        }

        boolean[] output = new boolean[total];
        int i = 0;
        for (CommandExecutorPoolWorker currWorker : commandExecutorWorkers)
        {
            boolean[] currWorkerFlags = currWorker.getVisibilityFlags();
            for (boolean currFlag : currWorkerFlags)
            {
                output[i++] = currFlag;
            }
        }

        return output;
    }
}
