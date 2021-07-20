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
import com.thirdpartylabs.servicebus.events.ApplicationActivityServiceBusEvent;
import com.thirdpartylabs.servicebus.events.EventSubscription;

/**
 * Manager for the pooled listener threaded bus test
 * <p>
 * The specified number of event listeners and generators will be created, and the generators will perform
 * the specified number of iterations.
 * <p>
 * We will run listeners for two subscriptions to the same event type to verify that they each receive a separate
 * stream. We are also verifying that non-subscription listeners have their own stream.
 * <p>
 * The listeners will all operate on the queue provided by a subscription, each working in its own thread. Results
 * from the operations will be stored for analysis. The unit test should compare the expected results to the actual
 * results produced.
 */
public class ListenerPoolTestController
{
    private final EventObserver eventObserver;

    private final GeneratorWorker[] generators;
    private final EventListenerPoolWorker[] eventListenerWorkers;

    // Number of generators to spawn
    private final int generatorCount;

    // Number of responders to spawn
    private final int listenerCount;

    // Number of times the generators should perform their operations
    private final int iterations;

    /**
     * @param generatorCount Number of generators to spawn
     * @param listenerCount  Number of listeners to spawn
     * @param iterations     Number of times the generator should perform its operations
     */
    public ListenerPoolTestController(int generatorCount, int listenerCount, int iterations)
    {
        this.generatorCount = generatorCount;
        this.listenerCount = listenerCount;
        this.iterations = iterations;

        // The event observer stays in this thread and listens to all of the events
        eventObserver = new EventObserver();

        // These workers listen for events
        eventListenerWorkers = new EventListenerPoolWorker[listenerCount*2];

        // Set up two subscriptions
        EventSubscription<ApplicationActivityServiceBusEvent> subscription1 = ServiceBus.getInstance().events.subscribe(ApplicationActivityServiceBusEvent.class);
        EventSubscription<ApplicationActivityServiceBusEvent> subscription2 = ServiceBus.getInstance().events.subscribe(ApplicationActivityServiceBusEvent.class);

        // Assign listeners for the first subscription
        for (int i = 0; i < listenerCount; i++)
        {
            eventListenerWorkers[i] = new EventListenerPoolWorker(subscription1);
        }

        // Assign listeners for the second subscription
        for (int i = listenerCount; i < listenerCount*2; i++)
        {
            eventListenerWorkers[i] = new EventListenerPoolWorker(subscription2);
        }

        // These workers generate traffic on the bus - configure them to send events only for this test
        generators = new GeneratorWorker[generatorCount];
        for (int i = 0; i < generatorCount; i++)
        {
            generators[i] = new GeneratorWorker(iterations, false, false, true);
        }
    }

    /**
     * Run the workers
     */
    public void run()
    {
        // Start up the listeners
        for (EventListenerPoolWorker currWorker : eventListenerWorkers)
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
        boolean listenersComplete = false;
        while (!generatorsComplete || !listenersComplete)
        {
            try
            {
                // Iterate through the generators, if any are still working, set the complete flag false
                generatorsComplete = true;
                for (GeneratorWorker currGenerator : generators)
                {
                    if (!currGenerator.isComplete())
                    {
                        generatorsComplete = false;
                        break;
                    }
                }

                // Iterate through the listeners, if any are still working, set the complete flag false
                listenersComplete = true;
                for (EventListenerPoolWorker currWorker : eventListenerWorkers)
                {
                    if (currWorker.isWorking())
                    {
                        listenersComplete = false;
                        break;
                    }
                }

                // If the listeners are still working sleep a while
                if (!generatorsComplete || !listenersComplete)
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
        for (EventListenerPoolWorker currWorker : eventListenerWorkers)
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
     * @return The expected number of generated events
     */
    public int getExpectedEventCount()
    {
        // We're running two subscriptions to verify that each subscription receives its own event stream
        return (iterations * 2) * generatorCount;
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
        int total = 0;

        for (EventListenerPoolWorker currWorker : eventListenerWorkers)
        {
            total += currWorker.getReceivedEvents().size();
        }

        return total;
    }
}
