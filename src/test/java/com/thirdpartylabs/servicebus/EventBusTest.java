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

import com.thirdpartylabs.servicebus.events.ApplicationActivityServiceBusEvent;
import com.thirdpartylabs.servicebus.events.ApplicationActivityServiceBusEventListener;
import com.thirdpartylabs.servicebus.events.ServiceBusEventListener;
import com.thirdpartylabs.servicebus.events.EventSubscription;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EventBusTest
{
    private EventBus eventBus;
    private final String eventMessage = "Something Happened";
    private String receivedEventMessage;
    private String secondaryReceivedEventMessage;

    @BeforeEach
    void setUp()
    {
        eventBus = EventBus.getInstance();
    }

    @AfterEach
    void tearDown()
    {
        eventBus = null;
        receivedEventMessage = null;
    }

    @Test
    void emit()
    {
        /*
            Instantiate an event and emit it on the EventBus
            Should not produce an exception
         */
        ApplicationActivityServiceBusEvent event = new ApplicationActivityServiceBusEvent(eventMessage);

        eventBus.emit(event);
    }

    @Test
    void listenFor() throws InterruptedException
    {
        /*
            Register a listener for a custom event type

            If the listener will never be removed, a Lambda can be passed info the listenFor method directly.

            In order for the EventBus to be able to identify the assigned Lambda and remove it from the assigned
            listeners, we need to pass the *same instance* of it into both the listenFor and removeListener methods.
         */
        eventBus.listenFor(ApplicationActivityServiceBusEvent.class, this::handleApplicationActivityEvent);

        //Instantiate an event and emit it on the bus
        ApplicationActivityServiceBusEvent event = new ApplicationActivityServiceBusEvent(eventMessage);
        eventBus.emit(event);

        // Wat a little for the listener to work
        Thread.sleep(100);

        /*
            The handler should have received the event and set the receivedEventMessage
            Verify that the Strings match
         */
        assertSame(eventMessage, receivedEventMessage, "Message received from event should match the sent message");
    }

    @Test
    void listenForWithMultipleListeners() throws InterruptedException
    {
        // Register a listener for a custom event type
        eventBus.listenFor(ApplicationActivityServiceBusEvent.class, this::handleApplicationActivityEvent);

        // Register a second listener for a custom event type
        eventBus.listenFor(ApplicationActivityServiceBusEvent.class, this::secondaryEventHandler);

        //Instantiate an event and emit it on the bus
        ApplicationActivityServiceBusEvent event = new ApplicationActivityServiceBusEvent(eventMessage);
        eventBus.emit(event);

        // Wat a little for the listeners to work
        Thread.sleep(100);
        /*
            The handler should have received the event and set the receivedEventMessage
            Verify that the Strings match
         */
        assertSame(eventMessage, receivedEventMessage, "Message received from event via first should match the sent message");

        assertSame(eventMessage, secondaryReceivedEventMessage, "Message received from event via second listener should match the sent message");
    }

    @Test
    void listenWithConcreteListenerClass() throws InterruptedException
    {
        /*
        Instantiate a concrete listener for an event type
        This listener accepts a string as a constructor argument. The string will be updated with the contents
        of the message in the event
         */
        ApplicationActivityServiceBusEventListener listener = new ApplicationActivityServiceBusEventListener();

        // Register a concrete listener instance for a custom event type
        eventBus.listenFor(ApplicationActivityServiceBusEvent.class, listener);

        //Instantiate an event and emit it on the bus
        ApplicationActivityServiceBusEvent event = new ApplicationActivityServiceBusEvent(eventMessage);
        eventBus.emit(event);

        // Wat a little for the listener to work
        Thread.sleep(100);

        /*
            The handler should have received the event and set the message instance variable
            Verify that the Strings match
         */
        assertSame(eventMessage, listener.getMessage(), "Message received from event should match the sent message");
    }

    @Test
    void testRemoveAllListenersForClass() throws InterruptedException
    {
        // Register a listener for a custom event type
        eventBus.listenFor(ApplicationActivityServiceBusEvent.class, this::handleApplicationActivityEvent);

        // Register a second listener for a custom event type
        eventBus.listenFor(ApplicationActivityServiceBusEvent.class, this::secondaryEventHandler);

        // Remove listeners for class
        eventBus.removeListener(ApplicationActivityServiceBusEvent.class);

        //Instantiate an event and emit it on the bus
        ApplicationActivityServiceBusEvent event = new ApplicationActivityServiceBusEvent(eventMessage);
        eventBus.emit(event);

        // Wat a little for the bus to work
        Thread.sleep(100);

        /*
            The handler should have received the event and set the receivedEventMessage
            Verify that the Strings match
         */
        assertNull(receivedEventMessage, "Event listener should not have received the message");

        assertNull(secondaryReceivedEventMessage, "Second event listener should not have received the message");
    }

    @Test
    void testRemoveLambdaListener()
    {
        /*
            In order for the EventBus to be able to identify the assigned Lambda and remove it from the assigned
            listeners, we need to pass the *same instance* of it into both the listenFor and removeListener methods.

            If the listener will never be removed, this is not necessary, the Lambda can be passed info the listenFor
            method directly.
         */
        ServiceBusEventListener<ApplicationActivityServiceBusEvent> listener = this::handleApplicationActivityEvent;

        // Register a listener for a custom event type
        eventBus.listenFor(ApplicationActivityServiceBusEvent.class, listener);

        // Remove the listener
        eventBus.removeListener(ApplicationActivityServiceBusEvent.class, listener);

        //Instantiate an event and emit it on the bus
        ApplicationActivityServiceBusEvent event = new ApplicationActivityServiceBusEvent(eventMessage);
        eventBus.emit(event);

        /*
            The handler should have been removed before the event was emitted, so the receivedEventMessage should
            remain null
         */
        assertNull(receivedEventMessage, "Message should not have been updated bu the listener");
    }

    @Test
    void testRemoveConcreteListener() throws InterruptedException
    {
        /*
        Instantiate a concrete listener for an event type
        This listener accepts a string as a constructor argument. The string will be updated with the contents
        of the message in the event
         */
        ApplicationActivityServiceBusEventListener listener = new ApplicationActivityServiceBusEventListener();

        // Register a concrete listener instance for a custom event type
        eventBus.listenFor(ApplicationActivityServiceBusEvent.class, listener);

        // Remove the listener
        eventBus.removeListener(ApplicationActivityServiceBusEvent.class, listener);

        //Instantiate an event and emit it on the bus
        ApplicationActivityServiceBusEvent event = new ApplicationActivityServiceBusEvent(eventMessage);
        eventBus.emit(event);

        // Wat a little for the bus to work
        Thread.sleep(100);

        /*
            The handler should have been removed before the event was emitted, so the receivedEventMessage should
            remain null
         */
        assertNull(receivedEventMessage, "Message should not have been updated bu the listener");
    }

    @Test
    void testSubscribe() throws InterruptedException
    {
        // Subscribe to events for our event class
        EventSubscription<ApplicationActivityServiceBusEvent> subscription = eventBus.subscribe(ApplicationActivityServiceBusEvent.class);

        //Instantiate an event and emit it on the bus
        ApplicationActivityServiceBusEvent event = new ApplicationActivityServiceBusEvent(eventMessage);
        eventBus.emit(event);

        // Wat a little for the bus to work
        Thread.sleep(100);

        // Dequeue an event from our subscriber queue
        ApplicationActivityServiceBusEvent dequeuedEvent = subscription.getQueue().poll();

        /*
            We should receive the same instance of the message form the queue
         */
        assertSame(event, dequeuedEvent, "Message received from event should match the sent message");
    }

    @Test
    void testSubscribeWithSubscriptionInstance() throws InterruptedException
    {
        // Subscribe to events for our event class
        EventSubscription<ApplicationActivityServiceBusEvent> subscription = new EventSubscription(ApplicationActivityServiceBusEvent.class);
        eventBus.subscribe(subscription);

        //Instantiate an event and emit it on the bus
        ApplicationActivityServiceBusEvent event = new ApplicationActivityServiceBusEvent(eventMessage);
        eventBus.emit(event);

        // Wat a little for the bus to work
        Thread.sleep(100);

        // Dequeue an event from our subscriber queue
        ApplicationActivityServiceBusEvent dequeuedEvent = subscription.getQueue().poll();

        /*
            We should receive the same instance of the message form the queue
         */
        assertSame(event, dequeuedEvent, "Message received from event should match the sent message");
    }

    @Test
    void testUnsubscribe() throws InterruptedException
    {
        // Subscribe to events for our event class
        EventSubscription<ApplicationActivityServiceBusEvent> subscription = eventBus.subscribe(ApplicationActivityServiceBusEvent.class);

        // Unsubscribe
        eventBus.unsubscribe(subscription);

        //Instantiate an event and emit it on the bus
        ApplicationActivityServiceBusEvent event = new ApplicationActivityServiceBusEvent(eventMessage);
        eventBus.emit(event);

        // Wat a little for the bus to work
        Thread.sleep(100);

        // Dequeue an event from our subscriber queue
        ApplicationActivityServiceBusEvent dequeuedEvent = subscription.getQueue().poll();

        /*
            We should receive the same instance of the message form the queue
         */
        assertNull(dequeuedEvent, "Subscriber queue should be null");
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
     * Handle the incoming ApplicationActivityServiceBusEvent and populate secondaryReceivedEventMessage with the
     * message string
     *
     * @param event
     */
    private void secondaryEventHandler(ApplicationActivityServiceBusEvent event)
    {
        secondaryReceivedEventMessage = event.getMessage();
    }
}