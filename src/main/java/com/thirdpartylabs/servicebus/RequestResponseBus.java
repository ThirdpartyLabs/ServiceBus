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

import com.thirdpartylabs.servicebus.reqres.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * A communication bus enabling decoupled request-response operations. Responders and subscribers are registered for
 * ServiceBusRequest classes, and invoked by the bus when requests of that type are submitted. Only one responder or
 * subscriber may be registered for a ServiceBusRequest type at a time. Registering a new responder or subscriber
 * for a ServiceBusRequest type will replace the old responder/subscriber with the new one.
 * <p>
 * Responders are instances of ServiceBusResponder or lambdas that are compatible with the interface that are
 * executed directly on the bus. This means that they are executed in the thread that submits the request.
 * The RequestResponseBus is thread safe.
 * <p>
 * Subscribers are lightweight wrappers for  inbound and outbound SynchronousQueues that the bus uses to submit
 * requests and receive responses. This enables synchronous multithreaded command execution. Subscribers provide
 * options for configuring queue size and blocking behavior of the bus.
 */
public class RequestResponseBus
{
    private static final RequestResponseBus instance = new RequestResponseBus();

    private RequestResponseBus()
    {

    }

    /**
     * Return the single instance of the RequestResponseBus
     *
     * @return ServiceBus instance
     */
    public static RequestResponseBus getInstance()
    {
        return instance;
    }

    private final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

    /*
     * Responder registry
     */
    private final Map<Class<? extends ServiceBusRequest<?>>, ServiceBusResponder<? extends ServiceBusRequest<?>, ?>> responders = Collections.synchronizedMap(new HashMap<>());

    /*
     * Subscriber registry
     */
    private final Map<Class<? extends ServiceBusRequest<?>>, RequestSubscription<? extends ServiceBusRequest<?>>> subscribers = Collections.synchronizedMap(new HashMap<>());

    /**
     * Submit a request on the bus, receive a response
     *
     * @param request Request to be submitted
     * @param <T>     ServiceBusRequest type
     * @param <V>     Response value type
     * @return ServiceBusResponse
     */
    @SuppressWarnings("unchecked")
    public <T extends ServiceBusRequest<V>, V> ServiceBusResponse<V> submit(T request)
    {
        // Get the class of the provided request
        Class<T> requestClass = (Class<T>) request.getClass();

        // Looks for a responder for the request class
        ServiceBusResponder<T, V> serviceBusResponder = (ServiceBusResponder<T, V>) responders.get(requestClass);

        // If we have an assigned responder, use it to handle the request
        if (serviceBusResponder != null)
        {
            return serviceBusResponder.respond(request);
        }
        else if (subscribers.containsKey(requestClass)) // Look for a subscription
        {
            RequestSubscription<T> sub = (RequestSubscription<T>) subscribers.get(requestClass);

            try
            {
                return sendRequestToSubscriber(request, sub);
            }
            catch (Exception e)
            {
                /*
                    If any errors occur, delegate to our exception handler, which set an an error response on the
                    request
                 */
                return handleException(request, e);
            }
        }

        // If no responder is found, delegate to our noResponderHandler which will create the appropriate error response
        serviceBusResponder = this::noResponderHandler;

        return serviceBusResponder.respond(request);
    }

    /**
     * Submit a request asynchronously
     *
     * @param request Request to be submitted
     * @param <T>     ServiceBusRequest type
     * @param <V>     Response value type
     * @return Future
     */
    public <T extends ServiceBusRequest<V>, V> Future<ServiceBusResponse<V>> submitAsync(T request)
    {
        return singleThreadExecutor.submit(() -> submit(request));
    }

    /**
     * Assign a responder for the provided request type
     *
     * @param requestClass Request type to assign the responder for
     * @param responder    Responder
     * @param <T>          ServiceBusRequest type
     */
    public <T extends ServiceBusRequest<V>, V> void assignResponder(Class<T> requestClass, ServiceBusResponder<T, V> responder)
    {
        responders.put(requestClass, responder);
    }

    /**
     * Remove responder or subscriber for the provided request type
     *
     * @param requestClass Request type to remove the responder for
     * @param <T>          ServiceBusRequest type
     */
    public <T extends ServiceBusRequest<?>> void removeResponder(Class<T> requestClass)
    {
        responders.remove(requestClass);
        subscribers.remove(requestClass);
    }

    /**
     * Create, assign, and return a subscription for the provided ServiceBusRequest class
     * <p>
     * Subscriptions are intended for use in multithreaded applications where responders should not be run in the
     * thread of the request submitter.
     *
     * @param requestClass Request type to create a subscriber for
     * @param <T>          RequestSubscription type
     * @return Subscription
     */
    public <T extends ServiceBusRequest<?>> RequestSubscription<T> subscribe(Class<T> requestClass)
    {
        RequestSubscription<T> subscription = new RequestSubscription<>(requestClass);

        responders.remove(requestClass);
        subscribers.put(requestClass, subscription);

        return subscription;
    }

    /**
     * Register an existing RequestSubscription
     *
     * @param subscription Subscription to register
     * @param <T>          ServiceBusRequest type
     */
    public <T extends ServiceBusRequest<?>> void subscribe(RequestSubscription<T> subscription)
    {
        responders.remove(subscription.getRequestClass());
        subscribers.put(subscription.getRequestClass(), subscription);
    }

    /**
     * Remove the provided subscription from the bus
     *
     * @param subscription Subscription to remove
     * @param <T>          ServiceBusRequest type
     */
    public <T extends ServiceBusRequest<?>> void unsubscribe(RequestSubscription<T> subscription)
    {
        responders.remove(subscription.getRequestClass());
        subscribers.remove(subscription.getRequestClass());
    }

    /**
     * Set an error response on the request indicating that no responder was found
     *
     * @param request Submitted request
     * @param <T>     ServiceBusRequest type
     * @param <V>     Response value type
     */
    private <T extends ServiceBusRequest<V>, V> ServiceBusResponse<V> noResponderHandler(T request)
    {
        ServiceBusResponse<V> response = new ServiceBusResponse<>(null);
        response.setStatus(ServiceBusResponse.Status.ERROR);
        response.setMessage("No responder assigned for request type " + request.getClass().getName());

        return response;
    }

    /**
     * Set an error response on the request using details from teh provided exception
     *
     * @param request Submitted request
     * @param e       Exception encountered
     * @param <T>     ServiceBusRequest type
     * @param <V>     Response value type
     */
    private <T extends ServiceBusRequest<V>, V> ServiceBusResponse<V> handleException(T request, Exception e)
    {
        ServiceBusResponse<V> response = new ServiceBusResponse<>(null);
        response.setStatus(ServiceBusResponse.Status.ERROR);
        response.setMessage("Exception during request processing " + request.getClass().getName() + ": " + e.getMessage());

        return response;
    }

    /**
     * Send the provided request to a subscription via one of its rendezvous channels. This method handles
     * load balancing across the channels using the balance registers.
     *
     * @param request      The request to process
     * @param subscription Subscription to handle the request
     * @param <T>          ServiceBusRequest type
     * @param <V>          Type of value required by the request
     * @throws InterruptedException Thrown if thread is interrupted while blocking
     */
    @SuppressWarnings("unchecked")
    private <T extends ServiceBusRequest<V>, V> ServiceBusResponse<V> sendRequestToSubscriber(T request, RequestSubscription<T> subscription) throws InterruptedException
    {
        // Get the channel from the subscription
        RequestSubscriptionRendezvousChannel<T> channel = new RequestSubscriptionRendezvousChannel<>();

        // Put the channel on the queue - this must be dont prior to putting the request on the channel because blocking
        subscription.getChannelQueue().put(channel);

        // Put the request on the request queue (blocking)
        channel.getRequestQueue().put(request);

        // Return the response (blocking)
        return (ServiceBusResponse<V>) channel.getResponseQueue().take();
    }

    /**
     * Shut down the thread executor
     */
    public void shutdown()
    {
        singleThreadExecutor.shutdown();
    }
}
