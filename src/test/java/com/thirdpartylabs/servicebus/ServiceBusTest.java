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

import com.thirdpartylabs.servicebus.commands.ChangeWombatVisibilityServiceBusCommand;
import com.thirdpartylabs.servicebus.events.ApplicationActivityServiceBusEvent;
import com.thirdpartylabs.servicebus.exceptions.ServiceBusCommandExecutorNotAssignedException;
import com.thirdpartylabs.servicebus.reqres.ServiceBusResponse;
import com.thirdpartylabs.servicebus.requests.AdditionServiceBusRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServiceBusTest
{
    private ServiceBus serviceBus;

    private String receivedEventMessage;
    private boolean wombatVisibility;

    @BeforeEach
    void setUp()
    {
        serviceBus = ServiceBus.getInstance();
        wombatVisibility = false;
    }

    @AfterEach
    void tearDown()
    {
        serviceBus = null;
    }

    @Test
    void getInstance()
    {
        // Verify that a ServiceBus instance was created in the setUp method.
        assertNotNull(serviceBus, "ServiceBus instance should have been created.");
    }

    @Test
    void testEventBus() throws InterruptedException
    {
        // Register a listener for a custom event type
        serviceBus.events.listenFor(ApplicationActivityServiceBusEvent.class, this::handleApplicationActivityEvent);

        /*
            Set our reference to null and call getInstance again, verify singleton behavior
            by emitting an event and checking to make sure that the registered listener is still operating
         */
        serviceBus = null;
        serviceBus = ServiceBus.getInstance();

        /// Instantiate an event and emit it on the bus
        String eventMessage = "Something Happened";

        ApplicationActivityServiceBusEvent errorEvent = new ApplicationActivityServiceBusEvent(eventMessage);
        serviceBus.events.emit(errorEvent);

        // Wat a little for the bus to work
        Thread.sleep(100);

        /*
            The handler should have received the event and set the receivedEventMessage
            Verify that the Strings match
         */
        assertSame(eventMessage, receivedEventMessage, "Message received from event should match the sent message");
    }

    @Test
    void testCommandBus() throws ServiceBusCommandExecutorNotAssignedException, InterruptedException
    {
        // Register an executor for a custom command
        serviceBus.commands.assignExecutor(ChangeWombatVisibilityServiceBusCommand.class, this::executeChangeWombatVisibilityCommand);

        /*
            Set our reference to null and call getInstance again, verify singleton behavior
            by issuing a command and checking to make sure that the assigned executor is still operating
         */
        serviceBus = null;
        serviceBus = ServiceBus.getInstance();

        // Instantiate a command and issue it on the bus, use the blocking method so we can immediately see the result
        ChangeWombatVisibilityServiceBusCommand command = new ChangeWombatVisibilityServiceBusCommand(true);
        serviceBus.commands.issueBlocking(command);

        // Verify that the executor performed the expected action
        assertTrue(wombatVisibility, "ServiceBusCommand execution should result in updated member variable.");
    }

    @Test
    void testRequestResponseBus()
    {
        // Assign the expected responder for the AdditionServiceBusRequest class
        serviceBus.reqRes.assignResponder(AdditionServiceBusRequest.class, this::handleAddRequest);

        /*
            Set our reference to null and call getInstance again, verify singleton behavior
            by issuing a command and checking to make sure that the assigned executor is still operating
         */
        serviceBus = null;
        serviceBus = ServiceBus.getInstance();

        int a = 10;
        int b = 13;
        int expectedResult = a + b;

        // Create and submit a request
        AdditionServiceBusRequest request = new AdditionServiceBusRequest(a, b);
        ServiceBusResponse<Integer> response = serviceBus.reqRes.submit(request);

        // Verify that expected results were returned
        assertEquals(ServiceBusResponse.Status.SUCCESS, response.getStatus(), "Status should match expected result");
        assertEquals(expectedResult, response.getValue().intValue(), "Response should match expected result");
    }

    /**
     * Handle the incoming ApplicationActivityServiceBusEvent and populate receivedEventMessage with the message string
     *
     * @param event
     */
    private void handleApplicationActivityEvent(ApplicationActivityServiceBusEvent event)
    {
        receivedEventMessage = event.getMessage();
    }

    /**
     * A rudimentary executor for our test command. Sets the value of a member variable to the value specified in the
     * command.
     *
     * @param command
     */
    private void executeChangeWombatVisibilityCommand(ChangeWombatVisibilityServiceBusCommand command)
    {
        wombatVisibility = command.isVisible();
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

        return  new ServiceBusResponse<>(value);
    }
}