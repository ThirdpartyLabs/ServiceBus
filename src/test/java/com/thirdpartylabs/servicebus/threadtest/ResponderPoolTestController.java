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
import com.thirdpartylabs.servicebus.requests.AdditionServiceBusRequest;
import com.thirdpartylabs.servicebus.reqres.RequestSubscription;

import java.util.ArrayList;
import java.util.List;

/**
 * Manager for the pooled responder threaded bus test
 * <p>
 * The specified number of responders and generators will be created, and the generators will perform
 * the specified number of iterations.
 * <p>
 * The responders will all operate on the set of channels provided by a subscription, each working in its own thread.
 * Results from the operations will be stored for analysis. The unit test should compare the expected results to the
 * actual results produced.
 */
public class ResponderPoolTestController
{
    private final EventObserver eventObserver;
    private final EventListenerWorker eventListenerWorker;

    private final GeneratorWorker[] generators;
    private final ArrayList<RequestResponderChannelWorker> requestResponderWorkers;

    // Number of generators to spawn
    private final int generatorCount;

    // Number of responders to spawn
    private final int responderCount;

    // Number of times the generators should perform their operations
    private final int iterations;

    private final RequestSubscription<AdditionServiceBusRequest> subscription;

    /**
     * @param generatorCount Number of generators to spawn
     * @param responderCount Number of responders to spawn
     * @param iterations     Number of times the generator should perform its operations
     */
    public ResponderPoolTestController(int generatorCount, int responderCount, int iterations)
    {
        this.generatorCount = generatorCount;
        this.responderCount = responderCount;
        this.iterations = iterations;

        // The event observer stays in this thread and listens to all of the events
        eventObserver = new EventObserver();

        // The event listener worker is essentially identical to the observer, but its listener runs in a worker thread
        eventListenerWorker = new EventListenerWorker();

        // These worker runs a generate requests on the bus
        requestResponderWorkers = new ArrayList<>(responderCount);
        subscription = ServiceBus.getInstance().reqRes.subscribe(AdditionServiceBusRequest.class);
        for (int i = 0; i < responderCount; i++)
        {
            requestResponderWorkers.add(new RequestResponderChannelWorker(subscription));
        }

        // These workers generate traffic on the bus - configure them to send requests only for this test
        generators = new GeneratorWorker[generatorCount];
        for (int i = 0; i < generatorCount; i++)
        {
            generators[i] = new GeneratorWorker(iterations, false, true, false);
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

        // Wait until our workers set up their listeners on the bus, then start the generator
        while (!eventListenerWorker.isReady())
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

        // Start up the responders
        for (RequestResponderChannelWorker currWorker : requestResponderWorkers)
        {
            currWorker.start();
        }

        // Fire it up!
        for (GeneratorWorker currGenerator : generators)
        {
            currGenerator.start();
        }

        // Block this thead until the generators set their complete flag and the workers finish their queues
        boolean generatorsComplete = false;
        while (!generatorsComplete || eventListenerWorker.isWorking())
        {
            try
            {
                // Iterate through the generators, if any are still working, set the complete flag false
                generatorsComplete = true;
                for (GeneratorWorker currWorker : generators)
                {
                    if (!currWorker.isComplete())
                    {
                        generatorsComplete = false;
                        break;
                    }
                }

                /*
                    If the generators are still working, scale the worker pool size then sleep a while
                 */
                if (!generatorsComplete)
                {
                    int freeWorkers = 0;
                    for(RequestResponderChannelWorker currWorker : requestResponderWorkers)
                    {
                        if(!currWorker.isWorking())
                        {
                            // Kill ~.5 idle workers
                            if(Math.random() < .5)
                            {
                                requestResponderWorkers.remove(currWorker);
                                currWorker.stop();
                            }
                            else
                            {
                                freeWorkers++;
                            }
                        }
                    }

                    if(freeWorkers < 2)
                    {
                        RequestResponderChannelWorker newWorker = new RequestResponderChannelWorker(subscription);

                        newWorker.start();

                        requestResponderWorkers.add(newWorker);
                    }

                    Thread.sleep(100);
                }

                Thread.yield();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }

        // Stop the event listener worker
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
     * @return The number of events that the log worker received
     */
    public int getLogWorkerEventCount()
    {
        return eventListenerWorker.getReceivedEvents().size();
    }

    /**
     * @return The collection of expected to actual request-response calculations
     */
    public List<int[]> getCalculatorWorkerCalculations()
    {
        List<int[]> output = new ArrayList<>();

        for (RequestResponderChannelWorker currWorker : requestResponderWorkers)
        {
            output.addAll(currWorker.getCalculations());
        }

        return output;
    }

    /**
     * @return The collection of expected to actual request-response calculations
     */
    public List<int[]> getGeneratorCalculations()
    {
        List<int[]> output = new ArrayList<>();

        for (GeneratorWorker currGenerator : generators)
        {
            output.addAll(currGenerator.getCalculations());
        }

        return output;
    }
}
