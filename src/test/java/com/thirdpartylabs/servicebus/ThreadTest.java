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

import com.thirdpartylabs.servicebus.threadtest.ExecutorPoolTestController;
import com.thirdpartylabs.servicebus.threadtest.ListenerPoolTestController;
import com.thirdpartylabs.servicebus.threadtest.ResponderPoolTestController;
import com.thirdpartylabs.servicebus.threadtest.SmokeTestController;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ThreadTest
{
    /**
     * Test threaded execution for all three busses
     * A single instance each of event listeners, commend executors, and request responders are run in their own
     * threads. A configurable sized pool of test data generators run for a configurable number of iterations, sending
     * commands and requests across the bus. The command executor and request responder will in turn emit an event for
     * each item received. We can then collect the data produced to verify that it matches or expectations.
     */
    @Test
    void testThreaded()
    {
        // Number of generator threads to run
        int generatorCount = 100;

        // Number of commands and requests for each generator thread to dispatch on the bus
        int iterations = 1000;

        // Instantiate smokeTestController and run the threaded test
        SmokeTestController smokeTestController = new SmokeTestController(generatorCount, iterations);
        smokeTestController.run();

        // Collect the various metrics and data generated
        int expectedItemCount = smokeTestController.getExpectedItemCount();
        int expectedEventCount = smokeTestController.getExpectedEventCount();
        int observerEventCount = smokeTestController.getObserverEventCount();
        int logWorkerEventCount = smokeTestController.getLogWorkerEventCount();
        boolean[] receivedVisibilityFlags = smokeTestController.getReceivedVisibilityFlags();
        List<int[]> generatorCalculationResults = smokeTestController.getGeneratorCalculations();
        List<int[]> workerCalculationResults = smokeTestController.getCalculatorWorkerCalculations();

        assertEquals(expectedEventCount, observerEventCount, "Observer should have received the expected number of events");
        assertEquals(expectedEventCount, logWorkerEventCount, "Log worker should have received the expected number of events");

        assertEquals(expectedItemCount, receivedVisibilityFlags.length, "Expected visibility array length should equal number of iterations");

        assertEquals(expectedItemCount, generatorCalculationResults.size(), "Generator calculation result length should equal expected number");
        assertEquals(expectedItemCount, workerCalculationResults.size(), "Worker calculation result length should equal expected number");

        // Verify that each calculation performed was correct - generator provides expected and actual calculations
        for (int[] currCalcArray : generatorCalculationResults)
        {
            assertEquals(currCalcArray[0], currCalcArray[1], "Expected and actual calculation results should be equal");
        }

        // Verify that each calculation performed was correct - worker provides addends and results
        for (int[] currCalcArray : workerCalculationResults)
        {
            assertEquals(currCalcArray[0] + currCalcArray[1], currCalcArray[2], "Expected and actual calculation results should be equal");
        }
    }

    /**
     * Test pooling of request responders for a RequestSubscription. Multiple responders and multiple generators, each
     * in a separate thread, communicate synchronously via the RequestResponse bus
     */
    @Test
    void testResponderChannels()
    {
        // Number of responder channels to spawn
        int responderCount = 10;

        // Number of generator threads to run
        int generatorCount = 100;

        // Number of commands and requests for each generator thread to dispatch on the bus
        int iterations = 1000;

        // Instantiate smokeTestController and run the threaded test
        ResponderPoolTestController responderPoolTestController = new ResponderPoolTestController(generatorCount, responderCount, iterations);
        responderPoolTestController.run();

        // Collect the various metrics and data generated
        int expectedItemCount = responderPoolTestController.getExpectedItemCount();
        int expectedEventCount = responderPoolTestController.getExpectedEventCount();
        int observerEventCount = responderPoolTestController.getObserverEventCount();
        int logWorkerEventCount = responderPoolTestController.getLogWorkerEventCount();
        List<int[]> generatorCalculationResults = responderPoolTestController.getGeneratorCalculations();
        List<int[]> workerCalculationResults = responderPoolTestController.getCalculatorWorkerCalculations();

        assertEquals(expectedEventCount, observerEventCount, "Observer should have received the expected number of events");
        assertEquals(expectedEventCount, logWorkerEventCount, "Log worker should have received the expected number of events");

        assertEquals(expectedItemCount, generatorCalculationResults.size(), "Generator calculation result length should equal expected number");
        assertEquals(expectedItemCount, workerCalculationResults.size(), "Worker calculation result length should equal expected number");

        // Verify that each calculation performed was correct - generator provides expected and actual calculations
        for (int[] currCalcArray : generatorCalculationResults)
        {
            assertEquals(currCalcArray[0], currCalcArray[1], "Expected and actual calculation results should be equal");
        }

        // Verify that each calculation performed was correct - worker provides addends and results
        for (int[] currCalcArray : workerCalculationResults)
        {
            assertEquals(currCalcArray[0] + currCalcArray[1], currCalcArray[2], "Expected and actual calculation results should be equal");
        }
    }

    /**
     * Test pooling of event listeners for an EventSubscription. Multiple listeners receive events from multiple
     * generators via the EventBus, each in a separate thread.
     */
    @Test
    void testListenerPools()
    {
        // Number of responder channels to spawn
        int listenerCount = 10;

        // Number of generator threads to run
        int generatorCount = 100;

        // Number of commands and requests for each generator thread to dispatch on the bus
        int iterations = 1000;

        // Instantiate smokeTestController and run the threaded test
        ListenerPoolTestController listenerPoolTestController = new ListenerPoolTestController(generatorCount, listenerCount, iterations);
        listenerPoolTestController.run();

        // Collect the various metrics and data generated
        int expectedEventCount = listenerPoolTestController.getExpectedEventCount();
        int observerEventCount = listenerPoolTestController.getObserverEventCount();
        int logWorkerEventCount = listenerPoolTestController.getLogWorkerEventCount();

        /*
            The observer should only have half of the expected count, the workers are listening to two
            identical subscriptions
         */
        assertEquals(expectedEventCount / 2, observerEventCount, "Observer should have received the expected number of events");
        assertEquals(expectedEventCount, logWorkerEventCount, "Log worker should have received the expected number of events");
    }

    /**
     * Test pooling of command executors for an CommandSubscription. Multiple executors receive commands from multiple
     * generators via the CommandBus, each in a separate thread.
     */
    @Test
    void testExecutorPools()
    {
        // Number of responder channels to spawn
        int executorCount = 10;

        // Number of generator threads to run
        int generatorCount = 100;

        // Number of commands and requests for each generator thread to dispatch on the bus
        int iterations = 1000;

        // Instantiate smokeTestController and run the threaded test
        ExecutorPoolTestController executorPoolTestController = new ExecutorPoolTestController(generatorCount, executorCount, iterations);
        executorPoolTestController.run();

        // Collect the various metrics and data generated
        int expectedItemCount = executorPoolTestController.getExpectedItemCount();
        int expectedEventCount = executorPoolTestController.getExpectedEventCount();
        int observerEventCount = executorPoolTestController.getObserverEventCount();
        boolean[] receivedVisibilityFlags = executorPoolTestController.getReceivedVisibilityFlags();

        assertEquals(expectedEventCount, observerEventCount, "Observer should have received the expected number of events");

        assertEquals(expectedItemCount, receivedVisibilityFlags.length, "Expected visibility array length should equal number of iterations");
    }
}