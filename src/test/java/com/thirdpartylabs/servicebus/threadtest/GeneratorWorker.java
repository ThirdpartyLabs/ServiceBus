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
import com.thirdpartylabs.servicebus.reqres.ServiceBusResponse;
import com.thirdpartylabs.servicebus.commands.ChangeWombatVisibilityServiceBusCommand;
import com.thirdpartylabs.servicebus.events.ApplicationActivityServiceBusEvent;
import com.thirdpartylabs.servicebus.exceptions.ServiceBusCommandExecutorNotAssignedException;
import com.thirdpartylabs.servicebus.requests.AdditionServiceBusRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Thread worker that generates commands and requests on the service bus
 * For each iteration, a command and a request will be sent. Expectations and results are collected for
 * testing.
 */
public class GeneratorWorker extends AbstractWorker
{
    private final ServiceBus serviceBus;
    private final int iterations;
    private int iterationCount = 0;
    private final List<int[]> calculations = new ArrayList<>();
    private final List<String> events = new ArrayList<>();
    private final boolean[] visibilityFlags;

    private final boolean runCommands;
    private final boolean runRequests;
    private final boolean runEvents;

    /**
     * @param iterations Number of times the generator should perform its operations
     */
    public GeneratorWorker(int iterations)
    {
        runCommands = true;
        runRequests = true;
        runEvents = true;

        this.iterations = iterations;

        serviceBus = ServiceBus.getInstance();

        // Instantiate an array of the proper length for the visibility flags send over the command bus
        visibilityFlags = new boolean[iterations];
    }

    /**
     * @param iterations Number of times the generator should perform its operations
     */
    public GeneratorWorker(int iterations, boolean runCommands, boolean runRequests, boolean runEvents)
    {
        this.iterations = iterations;
        this.runCommands = runCommands;
        this.runRequests = runRequests;
        this.runEvents = runEvents;

        serviceBus = ServiceBus.getInstance();

        // Instantiate an array of the proper length for the visibility flags send over the command bus
        visibilityFlags = new boolean[iterations];
    }

    @Override
    public void run()
    {
        // Nothing to set up, signal that we're good to go
        ready = true;

        // Loop until we have reached the specified number of iterations. Bail out if we're told to stop.
        while (iterations > iterationCount)
        {
            if(runCommands)
            {
                issueCommand();
            }

            if(runRequests)
            {
                submitRequest();
            }

            if(runEvents)
            {
                emitEvent();
            }

            // Iterate here in case we need the count in the methods we're calling
            iterationCount++;

            Thread.yield();
        }
    }

    /**
     * Issue a ChangeWombatVisibilityServiceBusCommand on the bus with a random boolean value.
     * Store the value so we can compare to the commands received by the executor worker
     */
    private void issueCommand()
    {
        // Instantiate a command and issue it on the bus
        boolean coinFlip = (Math.random() < 0.5);

        // Save the value so we can check the sequence in the executor later
        visibilityFlags[iterationCount] = coinFlip;

        ChangeWombatVisibilityServiceBusCommand command = new ChangeWombatVisibilityServiceBusCommand(coinFlip);

        try
        {
            serviceBus.commands.issue(command);
        }
        catch (ServiceBusCommandExecutorNotAssignedException e)
        {
            /*
                We don't need to fdo anything here, this will result in an unexpected message count
                which should cause the test to fail.
             */
        }
    }

    /**
     * Submit a AdditionServiceBusRequest with random values
     * The expected result is stored along with the actual result from the response for testing later
     */
    private void submitRequest()
    {
        // Create and submit a request
        int a = (int) Math.round(Math.random() * 100);
        int b = (int) Math.round(Math.random() * 100);
        int expectedResult = a + b;

        AdditionServiceBusRequest request = new AdditionServiceBusRequest(a, b);
        ServiceBusResponse<Integer> response = serviceBus.reqRes.submit(request);

        if(response.getStatus() == ServiceBusResponse.Status.SUCCESS)
        {
            // Save the expected value and actual value
            calculations.add(new int[]{expectedResult, response.getValue()});
        }
        else
        {
            System.out.println(response.getMessage());
        }
    }

    /**
     * Emit an ApplicationActivityServiceBusEvent with random values
     * The message is stored  for testing later
     */
    private void emitEvent()
    {
        int a = (int) Math.round(Math.random() * 100);

        String message = "Here is an event with a random number in it. "+a;

        events.add(message);

        //Instantiate an event and emit it on the bus
        ApplicationActivityServiceBusEvent event = new ApplicationActivityServiceBusEvent(message);
        serviceBus.events.emit(event);
    }

    /**
     * @return boolean indicating whether or not the generator has finished the iterations
     */
    public boolean isComplete()
    {
        return iterationCount >= iterations;
    }

    /**
     * @return List of int arrays containing the expected and actual calculations
     */
    public List<int[]> getCalculations()
    {
        return calculations;
    }

    /**
     * @return Array of wombat visibility flags sent via the command bus
     */
    public boolean[] getVisibilityFlags()
    {
        return visibilityFlags;
    }

    /**
     * @return List of strings sent in ApplicationActivityServiceBusEvent objects on the bus
     */
    public List<String> getEvents()
    {
        return events;
    }
}
