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
import com.thirdpartylabs.servicebus.reqres.RequestSubscriptionRendezvousChannel;
import com.thirdpartylabs.servicebus.reqres.ServiceBusResponse;
import com.thirdpartylabs.servicebus.events.ApplicationActivityServiceBusEvent;
import com.thirdpartylabs.servicebus.requests.AdditionServiceBusRequest;
import com.thirdpartylabs.servicebus.reqres.RequestSubscription;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * Worker that runs an AdditionServiceBusRequest responder
 */
public class RequestResponderWorker extends AbstractWorker
{
    private final ServiceBus serviceBus = ServiceBus.getInstance();
    private final List<int[]> calculations = new ArrayList<>();

    @Override
    public void run()
    {
        RequestSubscription<AdditionServiceBusRequest> subscription = serviceBus.reqRes.subscribe(AdditionServiceBusRequest.class);
        BlockingQueue<RequestSubscriptionRendezvousChannel<AdditionServiceBusRequest>> channelQueue = subscription.getChannelQueue();
        /*
         * Now that the responder is registered, we can set our ready flag. The controller should block until all
         * workers set their ready flags
         */
        ready = true;

        // Block until we're told to stop
        while (true)
        {
            try
            {
                /*
                 * A rendezvous channel contains a request queue and a response queue, with a fixed length of one,
                 * where the producer blocks until the consumer collects the item that the producer placed on the
                 * request queue. Once the consumer takes the item, the producer then blocks while waiting for an item
                 * on the response queue. This enables synchronous bi-directional communication.
                 *
                 * The producer side of the queue allows multiple threads to block while waiting for an opening. In
                 * our test case here, we potentially have many generators sending requests to a pool of responders.
                 * The RequestResponseBus performs load balancing across the pool of rendezvous channels
                 * for a subscription, so we should se even distribution of the requests across our pool of workers.
                 */

                // Take a channel from the queue
                RequestSubscriptionRendezvousChannel<AdditionServiceBusRequest> rendezvousChannel = channelQueue.take();

                // Take the request from the request queue
                AdditionServiceBusRequest request = rendezvousChannel.getRequestQueue().take();

                // Generate a response
                ServiceBusResponse<Integer> response = handleAddRequest(request);

                // Return the response on the response queue
                rendezvousChannel.getResponseQueue().put(response);

                // If the queue is empty, indicate that we have nothing to do
                working = (!channelQueue.isEmpty());

                Thread.yield();
            }
            catch (Exception ignored)
            {
            }
        }
    }

    /**
     * Handle AdditionServiceBusRequest - add the integers assigned to A and B, set the result in the response,
     * and set the response on the request
     *
     * @param request AdditionServiceBusRequest submitted via the bus
     */
    private ServiceBusResponse<Integer> handleAddRequest(AdditionServiceBusRequest request)
    {
        int value = request.getA() + request.getB();

        // Save the expected value and actual value
        calculations.add(new int[]{request.getA(), request.getB(), value});

        //Instantiate an event and emit it on the bus
        ApplicationActivityServiceBusEvent event = new ApplicationActivityServiceBusEvent(String.format("Calculator addition %d+%d=%d", request.getA(), request.getB(), value));
        serviceBus.events.emit(event);

        return new ServiceBusResponse<>(value);
    }

    public List<int[]> getCalculations()
    {
        return calculations;
    }
}