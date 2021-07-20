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

package com.thirdpartylabs.servicebus.reqres;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A RequestSubscription is registered with the RequestResponseBus for the specified command class.
 * The subscription contains an inbound and outbound SynchronousQueue that are used for bidirectional communication
 * between the RequestResponseBus and the responder. ServiceBusRequest objects are passed on the requestQueue, and
 * ServiceBusResponse objects are returned on the responseQueue
 * <p>
 * Only one subscription or responder is allowed to be registered for each ServiceBusRequest class. If an executor or
 * a different subscription are registered after an instance has been registered, that instance will no longer receive
 * requests.
 * <p>
 * By default, each subscriber supports a single channel, meaning only on request may be processed at a time. To
 * support thread pooling for responders, multiple channels are supported for each subscriber. When using multiple
 * channels, each channel must be assigned to a consumer. The bus will send requests to free channels as needed,
 * if the bus attempts to send a request to a channel with no consumer listening, it will block indefinitely.
 *
 * @param <T>
 */
public class RequestSubscription<T extends ServiceBusRequest<?>>
{
    /**
     * Request type
     */
    private final Class<T> requestClass;

    /**
     * Rendezvous channel queue. Channels are placed on the queue as requests are received
     */
    private final BlockingQueue<RequestSubscriptionRendezvousChannel<T>> channelQueue = new LinkedBlockingQueue<>();

    /**
     * Create a subscription with one channel
     *
     * @param requestClass Request class to register for on the bus
     */
    public RequestSubscription(Class<T> requestClass)
    {
        this.requestClass = requestClass;
    }

    /**
     * @return Request class to register for on the bus
     */
    public Class<T> getRequestClass()
    {
        return requestClass;
    }

    /**
     * Get the array of rendezvous channels
     *
     * @return Rendezvous channels
     */
    public BlockingQueue<RequestSubscriptionRendezvousChannel<T>> getChannelQueue()
    {
        return channelQueue;
    }
}
