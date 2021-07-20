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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * The log worker runs a listener for ApplicationActivityServiceBusEvent in a thread.
 * Received events are collected for analysis
 */
public class EventListenerWorker extends AbstractWorker
{
    private final List<String> receivedEvents = new ArrayList<>();

    @Override
    public void run()
    {
        EventSubscription<ApplicationActivityServiceBusEvent> subscription = ServiceBus.getInstance().events.subscribe(ApplicationActivityServiceBusEvent.class);

        /*
         * Now that the listener is registered, we can set our ready flag. The controller should block until all
         * workers set their ready flags
         */
        ready = true;

        // Pull events from the blocking queue until we're told to stop
        while (true)
        {
            try
            {
                /*
                    We're dealing with a finite amount of events, so we don't want to block indefinitely on the queue
                    Poll with  10ms timeout. Timeout will generate an InterruptException, so just catch and ignore
                 */
                ApplicationActivityServiceBusEvent event = subscription.getQueue().poll(10, TimeUnit.MILLISECONDS);

                if(event != null)
                {
                    receivedEvents.add(event.getMessage());
                }

                // If the queue is empty, indicate that we have nothing to do
                working = (!subscription.getQueue().isEmpty());

                Thread.yield();
            }
            catch (Exception ignored)
            {
            }
        }
    }

    /**
     * @return the collection of received events
     */
    public List<String> getReceivedEvents()
    {
        return receivedEvents;
    }
}
