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

import java.util.concurrent.SynchronousQueue;

/**
 * Similar in function to a {@link java.util.concurrent.Exchanger}, but enabling the exchange
 * of ServiceRequest and ServiceResponse objects. This enables pooling of request responders.
 * @param <T> ServiceBusRequest
 */
public class RequestSubscriptionRendezvousChannel<T extends ServiceBusRequest<?>>
{
    /**
     * Queue for sending a request to the responder
     */
    private final SynchronousQueue<T> requestQueue;

    /**
     * Queue for receiving response from responder
     */
    private final SynchronousQueue<ServiceBusResponse<?>> responseQueue;

    public RequestSubscriptionRendezvousChannel()
    {
        this.requestQueue = new SynchronousQueue<>();
        this.responseQueue = new SynchronousQueue<>();
    }

    /**
     * @return Queue that the bus uses to issue requests
     */
    public SynchronousQueue<T> getRequestQueue()
    {
        return requestQueue;
    }

    /**
     * @return Queue for returning ServiceBusResponse objects
     */
    public SynchronousQueue<ServiceBusResponse<?>> getResponseQueue()
    {
        return responseQueue;
    }
}
