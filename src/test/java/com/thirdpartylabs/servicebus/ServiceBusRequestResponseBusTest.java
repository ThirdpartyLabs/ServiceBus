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

import com.thirdpartylabs.servicebus.reqres.RequestSubscriptionRendezvousChannel;
import com.thirdpartylabs.servicebus.reqres.ServiceBusResponse;
import com.thirdpartylabs.servicebus.requests.AdditionServiceBusRequest;
import com.thirdpartylabs.servicebus.requests.AdditionServiceBusResponder;
import com.thirdpartylabs.servicebus.reqres.RequestSubscription;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ServiceBusRequestResponseBusTest
{
    private RequestResponseBus reqResBus;
    private final int expectedErrorCode = 404;
    private final String expectedErrorMessage = "This is an error message";
    private boolean responderToOverrideCalled;

    @BeforeEach
    void setUp()
    {
        reqResBus = RequestResponseBus.getInstance();
        responderToOverrideCalled = false;
    }

    @AfterEach
    void tearDown()
    {
        reqResBus = null;
    }

    @Test
    void assignResponder()
    {
        reqResBus.assignResponder(AdditionServiceBusRequest.class, this::handleAddRequest);
    }

    @Test
    void submit()
    {
        // Assign a responder for the AdditionServiceBusRequest class
        reqResBus.assignResponder(AdditionServiceBusRequest.class, this::handleAddRequest);

        int a = 10;
        int b = 13;
        int expectedResult = a + b;

        // Create and submit a request
        AdditionServiceBusRequest request = new AdditionServiceBusRequest(a, b);
        ServiceBusResponse<Integer> response = reqResBus.submit(request);

        // Verify that expected results were returned
        assertEquals(ServiceBusResponse.Status.SUCCESS, response.getStatus(), "Status should match expected result");
        assertEquals(expectedResult, response.getValue().intValue(), "Response should match expected result");
    }

    @Test
    void submitAsync() throws ExecutionException, InterruptedException, TimeoutException
    {
        // Assign a responder for the AdditionServiceBusRequest class
        reqResBus.assignResponder(AdditionServiceBusRequest.class, this::handleAddRequest);

        int a = 10;
        int b = 13;
        int expectedResult = a + b;

        // Create and submit a request
        AdditionServiceBusRequest request = new AdditionServiceBusRequest(a, b);
        Future<ServiceBusResponse<Integer>> future = reqResBus.submitAsync(request);

        ServiceBusResponse<Integer> response = future.get(1000, TimeUnit.MILLISECONDS);

        // Verify that expected results were returned
        assertEquals(ServiceBusResponse.Status.SUCCESS, response.getStatus(), "Status should match expected result");
        assertEquals(expectedResult, response.getValue().intValue(), "Response should match expected result");
    }

    @Test
    void submitWithLambdaResponder()
    {
        // Assign a lambda responder for the AdditionServiceBusRequest class
        reqResBus.assignResponder(AdditionServiceBusRequest.class, (request) -> {
            int value = request.getA() + request.getB();

            return new ServiceBusResponse<>(value);
        });

        int a = 10;
        int b = 13;
        int expectedResult = a + b;

        // Create and submit a request
        AdditionServiceBusRequest request = new AdditionServiceBusRequest(a, b);
        ServiceBusResponse<Integer> response = reqResBus.submit(request);

        // Verify that expected results were returned
        assertEquals(ServiceBusResponse.Status.SUCCESS, response.getStatus(), "Status should match expected result");
        assertEquals(expectedResult, response.getValue().intValue(), "Response should match expected result");
    }

    @Test
    void testErrorCondition()
    {
        // Assign the error generating responder for the AdditionServiceBusRequest class
        reqResBus.assignResponder(AdditionServiceBusRequest.class, this::handleAddRequestWithError);

        // Create and submit a request
        AdditionServiceBusRequest request = new AdditionServiceBusRequest(1, 2);
        ServiceBusResponse<Integer> response = reqResBus.submit(request);

        // Verify error state
        assertEquals(ServiceBusResponse.Status.ERROR, response.getStatus(), "Status should match expected result");
        assertEquals(expectedErrorCode, response.getErrorCode(), "Error code should match expected result");
        assertEquals(expectedErrorMessage, response.getMessage(), "Error message should match expected result");
    }

    @Test
    void testRespondersAreReplaced()
    {
        // Assign the bogus responder that should be overridden for the AdditionServiceBusRequest class
        reqResBus.assignResponder(AdditionServiceBusRequest.class, this::responderToOverride);

        // Assign the expected responder for the AdditionServiceBusRequest class
        reqResBus.assignResponder(AdditionServiceBusRequest.class, this::handleAddRequest);

        int a = 10;
        int b = 13;
        int expectedResult = a + b;

        // Create and submit a request
        AdditionServiceBusRequest request = new AdditionServiceBusRequest(a, b);
        ServiceBusResponse<Integer> response = reqResBus.submit(request);

        // Make sure the first responder was not called
        assertFalse(responderToOverrideCalled, "Overridden responder should not have been called");

        // Make sure the second responder functioned correctly
        assertEquals(ServiceBusResponse.Status.SUCCESS, response.getStatus(), "Status should match expected result");
        assertEquals(expectedResult, response.getValue().intValue(), "Response should match expected result");
    }

    @Test
    void testSubmitWithConcreteResponderInstance()
    {
        // Instantiate a concrete responder for the request
        AdditionServiceBusResponder responder = new AdditionServiceBusResponder();

        // Assign a responder for the AdditionServiceBusRequest class
        reqResBus.assignResponder(AdditionServiceBusRequest.class, responder);

        int a = 10;
        int b = 13;
        int expectedResult = a + b;

        // Create and submit a request
        AdditionServiceBusRequest request = new AdditionServiceBusRequest(a, b);
        ServiceBusResponse<Integer> response = reqResBus.submit(request);

        // Verify that expected results were returned
        assertEquals(ServiceBusResponse.Status.SUCCESS, response.getStatus(), "Status should match expected result");
        assertEquals(expectedResult, response.getValue().intValue(), "Response should match expected result");
    }

    @Test
    void testRemoveResponder()
    {
        // Assign a responder for the AdditionServiceBusRequest class
        reqResBus.assignResponder(AdditionServiceBusRequest.class, this::handleAddRequest);

        reqResBus.removeResponder(AdditionServiceBusRequest.class);

        int a = 10;
        int b = 13;

        // Create and submit a request
        AdditionServiceBusRequest request = new AdditionServiceBusRequest(a, b);
        ServiceBusResponse<Integer> response = reqResBus.submit(request);

        // Verify that expected results were returned
        assertEquals(ServiceBusResponse.Status.ERROR, response.getStatus(), "Status should be ERROR");
    }

    @Test
    void testSubscribe()
    {
        // Assign a responder for the AdditionServiceBusRequest class
        RequestSubscription<AdditionServiceBusRequest> subscription = reqResBus.subscribe(AdditionServiceBusRequest.class);

        Thread t = new Thread(new CalculatorWorker(subscription));
        t.start();

        int a = 10;
        int b = 13;
        int expectedResult = a + b;

        // Create and submit a request
        AdditionServiceBusRequest request = new AdditionServiceBusRequest(a, b);
        ServiceBusResponse<Integer> response = reqResBus.submit(request);

        // Verify that expected results were returned
        assertEquals(ServiceBusResponse.Status.SUCCESS, response.getStatus(), "Status should match expected result");
        assertEquals(expectedResult, response.getValue().intValue(), "Response should match expected result");
    }

    @Test
    void testSubscribeWithSubscriptionInstance()
    {
        // Assign a responder for the AdditionServiceBusRequest class
        RequestSubscription<AdditionServiceBusRequest> subscription = new RequestSubscription<>(AdditionServiceBusRequest.class);
        reqResBus.subscribe(subscription);

        Thread t = new Thread(new CalculatorWorker(subscription));
        t.start();

        int a = 10;
        int b = 13;
        int expectedResult = a + b;

        // Create and submit a request
        AdditionServiceBusRequest request = new AdditionServiceBusRequest(a, b);
        ServiceBusResponse<Integer> response = reqResBus.submit(request);

        // Verify that expected results were returned
        assertEquals(ServiceBusResponse.Status.SUCCESS, response.getStatus(), "Status should match expected result");
        assertEquals(expectedResult, response.getValue().intValue(), "Response should match expected result");
    }

    @Test
    void testUnsubscribe()
    {
        // Assign a responder for the AdditionServiceBusRequest class
        RequestSubscription<AdditionServiceBusRequest> subscription = reqResBus.subscribe(AdditionServiceBusRequest.class);

        reqResBus.unsubscribe(subscription);

        int a = 10;
        int b = 13;

        // Create and submit a request
        AdditionServiceBusRequest request = new AdditionServiceBusRequest(a, b);
        ServiceBusResponse<Integer> response = reqResBus.submit(request);

        // Verify that expected results were returned
        assertEquals(ServiceBusResponse.Status.ERROR, response.getStatus(), "Status should be ERROR");
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

        return new ServiceBusResponse<>(value);
    }

    /**
     * Respond with errors
     * The status should be set to error, an error message and error code should be set
     *
     * @param request AdditionServiceBusRequest submitted via the bus
     */
    private ServiceBusResponse<Integer> handleAddRequestWithError(AdditionServiceBusRequest request)
    {
        ServiceBusResponse<Integer> response = new ServiceBusResponse<>(13);
        response.setStatus(ServiceBusResponse.Status.ERROR);
        response.setMessage(expectedErrorMessage);
        response.setErrorCode(expectedErrorCode);

        return response;
    }

    /**
     * This responder should never be called, it should be overridden when another responder is assigned for
     * the AdditionServiceBusRequest class. Set the responderToOverrideCalled if called.
     *
     * @param request AdditionServiceBusRequest submitted via the bus
     */
    private ServiceBusResponse<Integer> responderToOverride(AdditionServiceBusRequest request)
    {
        responderToOverrideCalled = true;

        return new ServiceBusResponse<>(null);
    }

    private static class CalculatorWorker implements Runnable
    {
        RequestSubscription<AdditionServiceBusRequest> subscription;

        public CalculatorWorker(RequestSubscription<AdditionServiceBusRequest> subscription)
        {
            this.subscription = subscription;
        }

        @Override
        public void run()
        {
            while (true)
            {
                try
                {
                    RequestSubscriptionRendezvousChannel<AdditionServiceBusRequest> channel = subscription.getChannelQueue().take();
                    AdditionServiceBusRequest request = channel.getRequestQueue().take();

                    int value = request.getA() + request.getB();

                    ServiceBusResponse<Integer> response = new ServiceBusResponse<>(value);

                    channel.getResponseQueue().put(response);

                    Thread.yield();
                }
                catch (Exception ignored)
                {
                }
            }
        }
    }
}