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

import java.util.ArrayList;
import java.util.List;

/**
 * Manager for the threaded smoke test
 * <p>
 * The specified number of generators will be created, and will perform the specified number of iterations.
 * <p>
 * The event listeners, command executors, and responders will each run a single instance on its own thread and operate
 * in subscription mode, each with a queue of items to process
 * <p>
 * The responder is a bit of a special case, responder subscriptions with only one responder worker utilise a pair
 * of SynchronousQueues, which causes producers to block while waiting to communicate with the responder.
 */
public class SmokeTestController
{
    private final EventObserver eventObserver;
    private final RequestResponderWorker requestResponderWorker;
    private final CommandExecutorWorker commandExecutorWorker;
    private final EventListenerWorker eventListenerWorker;

    private final GeneratorWorker[] generators;

    // Number of generators to spawn
    private final int generatorCount;

    // Number of times the generator should perform its operations
    private final int iterations;

    /**
     * @param generatorCount Number of generators to spawn
     * @param iterations Number of times the generator should perform its operations
     */
    public SmokeTestController(int generatorCount, int iterations)
    {
        this.generatorCount = generatorCount;
        this.iterations = iterations;

        // The event observer stays in this thread and listens to all of the events
        eventObserver = new EventObserver();

        // The log worker is essentially identical to the observer, but its listener runs in a worker thread
        eventListenerWorker = new EventListenerWorker();

        // This worker runs a AdditionServiceBusRequest executor in its own thread
        requestResponderWorker = new RequestResponderWorker();

        // This worker runs a ChangeWombatVisibilityServiceBusCommand executor in its own thread
        commandExecutorWorker = new CommandExecutorWorker();

        /*
            These workers generate traffic on the bus - configure to send commands and requests but not events
            for this test
         */
        generators = new GeneratorWorker[generatorCount];
        for(int i = 0; i < generatorCount; i++)
        {
            generators[i] = new GeneratorWorker(iterations, true, true, false);
        }
    }

    /**
     * Run the workers
     */
    public void run()
    {
        /*
         * First start the workers that run listeners/handlers on the bus. Since those facilities are registered
         * when the thread starts running, they require a brief period of time to initialize. They will set ready
         * flags when their initialization is complete.
         */
        eventListenerWorker.start();
        requestResponderWorker.start();
        commandExecutorWorker.start();

        // Wait until our workers set up their listeners on the bus, then start the generator
        while (!eventListenerWorker.isReady() || !requestResponderWorker.isReady() || !commandExecutorWorker.isReady())
        {
            try
            {
                Thread.sleep(10);
                Thread.yield();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        // Fire it up!
        for(int i=0; i < generatorCount; i++)
        {
            generators[i].start();
        }

        // Block this thead until the generators set their complete flag and the workers finish their queues
        boolean generatorsComplete = false;
        while (!generatorsComplete || eventListenerWorker.isWorking() || requestResponderWorker.isWorking() || commandExecutorWorker.isWorking())
        {
            try
            {
                // Iterate through the generators, if any are still working, set the complete flag false
                generatorsComplete = true;
                for(int i=0; i < generatorCount; i++)
                {
                    if(!generators[i].isComplete())
                    {
                        generatorsComplete = false;
                    }
                }

                // If the generators are still working, sleep a while
                if(!generatorsComplete)
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
        requestResponderWorker.stop();
        commandExecutorWorker.stop();
        eventListenerWorker.stop();
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
        return iterations * generatorCount * 2;
    }

    /**
     * @return The number of events that the observer received
     */
    public int getObserverEventCount()
    {
        return eventObserver.getReceivedEvents().size();
    }

    /**
     * @return The number of events that the log worker received
     */
    public int getLogWorkerEventCount()
    {
        return eventListenerWorker.getReceivedEvents().size();
    }

    /**
     * @return The array of flags received by the command executor
     */
    public boolean[] getReceivedVisibilityFlags()
    {
        return commandExecutorWorker.getVisibilityFlags();
    }

    /**
     * @return The collection of expected to actual request-response calculations
     */
    public List<int[]> getCalculatorWorkerCalculations()
    {
        return requestResponderWorker.getCalculations();
    }

    /**
     * @return The collection of expected to actual request-response calculations
     */
    public List<int[]> getGeneratorCalculations()
    {
        List<int[]> output = new ArrayList<>();

        for(int i=0; i < generatorCount; i++)
        {
            output.addAll(generators[i].getCalculations());
        }

        return output;
    }
}
