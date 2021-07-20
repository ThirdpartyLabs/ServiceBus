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
import com.thirdpartylabs.servicebus.commands.ChangeWombatVisibilityServiceBusCommandExecutor;
import com.thirdpartylabs.servicebus.exceptions.ServiceBusCommandExecutorNotAssignedException;
import com.thirdpartylabs.servicebus.commands.CommandSubscription;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class CommandBusTest
{
    private CommandBus commandBus;
    private boolean wombatVisibility;
    private boolean executorToOverrideCalled;

    @BeforeEach
    void setUp()
    {
        commandBus = CommandBus.getInstance();
        wombatVisibility = false;
        executorToOverrideCalled = false;
    }

    @AfterEach
    void tearDown()
    {
        commandBus = null;
    }

    @Test
    void assignExecutor()
    {
        // Ensure that a proper executor assignment does not raise an exception
        commandBus.assignExecutor(ChangeWombatVisibilityServiceBusCommand.class, this::executeChangeWombatVisibilityCommand);
    }

    @Test
    void issue() throws ServiceBusCommandExecutorNotAssignedException, ExecutionException, InterruptedException, TimeoutException
    {
        /*
        Assign our executeChangeWombatVisibilityCommand method as the handler
        for the ChangeWombatVisibilityServiceBusCommand class
         */
        commandBus.assignExecutor(ChangeWombatVisibilityServiceBusCommand.class, this::executeChangeWombatVisibilityCommand);

        // Instantiate a command and issue it on the bus
        ChangeWombatVisibilityServiceBusCommand command = new ChangeWombatVisibilityServiceBusCommand(true);

        Future<Void> future = commandBus.issue(command);

        /*
            Block while waiting for a max of 100ms

            It is not necessary to do this unless you need to do something with the results of the command. You can
            just fire and forget with commandBus.issue(command);

            For these test, we need to make sure that the commands executed properly, so we will use issueBlocking
            from here on out.
         */
        future.get(100, TimeUnit.MILLISECONDS);

        // Verify that the command handler performed the expected action
        assertTrue(wombatVisibility, "ServiceBusCommand execution should result in updated member variable.");
    }

    @Test
    void testExecutorsAreReplaced() throws ServiceBusCommandExecutorNotAssignedException
    {
        //Assign the bogus handler that should be overridden for the ChangeWombatVisibilityServiceBusCommand class
        commandBus.assignExecutor(ChangeWombatVisibilityServiceBusCommand.class, this::executeChangeWombatVisibilityCommand);

        /*
        Assign our executeChangeWombatVisibilityCommand method as the handler
        for the ChangeWombatVisibilityServiceBusCommand class
        */
        commandBus.assignExecutor(ChangeWombatVisibilityServiceBusCommand.class, this::executeChangeWombatVisibilityCommand);

        // Instantiate a command and issue it on the bus
        ChangeWombatVisibilityServiceBusCommand command = new ChangeWombatVisibilityServiceBusCommand(true);
        commandBus.issueBlocking(command);

        // Verify that executor that was initially assigned was not called
        assertFalse(executorToOverrideCalled, "Overridden executor should not have been called");

        // Verify that the command handler performed the expected action
        assertTrue(wombatVisibility, "ServiceBusCommand execution should result in updated member variable.");
    }

    @Test
    void issueWithConcreteExecutorClass() throws ServiceBusCommandExecutorNotAssignedException
    {
        ChangeWombatVisibilityServiceBusCommandExecutor executor = new ChangeWombatVisibilityServiceBusCommandExecutor();
        /*
        Assign our executeChangeWombatVisibilityCommand method as the handler
        for the ChangeWombatVisibilityServiceBusCommand class
         */
        commandBus.assignExecutor(ChangeWombatVisibilityServiceBusCommand.class, executor);

        // Instantiate a command and issue it on the bus
        ChangeWombatVisibilityServiceBusCommand command = new ChangeWombatVisibilityServiceBusCommand(true);
        commandBus.issueBlocking(command);

        // Verify that the command handler performed the expected action
        assertTrue(executor.isWombatVisible(), "ServiceBusCommand execution should result in updated member variable.");
    }

    @Test
    void testRemoveExecutor()
    {
        /*
        Assign our executeChangeWombatVisibilityCommand method as the handler
        for the ChangeWombatVisibilityServiceBusCommand class
         */
        commandBus.assignExecutor(ChangeWombatVisibilityServiceBusCommand.class, this::executeChangeWombatVisibilityCommand);

        commandBus.removeExecutor(ChangeWombatVisibilityServiceBusCommand.class);

        // Instantiate a command and issue it on the bus
        ChangeWombatVisibilityServiceBusCommand command = new ChangeWombatVisibilityServiceBusCommand(true);

        assertThrows(ServiceBusCommandExecutorNotAssignedException.class, () -> {
            commandBus.issue(command);
        }, "ServiceBusCommandExecutorNotAssignedException should be thrown");

        assertThrows(ServiceBusCommandExecutorNotAssignedException.class, () -> {
            commandBus.issueBlocking(command);
        }, "ServiceBusCommandExecutorNotAssignedException should be thrown");
    }

    @Test
    void testSubscribe() throws ServiceBusCommandExecutorNotAssignedException
    {
        // Subscribe to commands
        CommandSubscription<ChangeWombatVisibilityServiceBusCommand> subscription = commandBus.subscribe(ChangeWombatVisibilityServiceBusCommand.class);

        // Instantiate a command and issue it on the bus
        ChangeWombatVisibilityServiceBusCommand command = new ChangeWombatVisibilityServiceBusCommand(true);

        commandBus.issueBlocking(command);

        ChangeWombatVisibilityServiceBusCommand receivedCommand = subscription.getQueue().poll();

        // Verify that the command handler performed the expected action
        assertEquals(command, receivedCommand, "Command received from queue should be same as sent command");
    }

    @Test
    void testSubscribeWithSubscriptionInstance() throws ServiceBusCommandExecutorNotAssignedException
    {
        // Subscribe to commands
        CommandSubscription<ChangeWombatVisibilityServiceBusCommand> subscription = new CommandSubscription<>(ChangeWombatVisibilityServiceBusCommand.class);
        commandBus.subscribe(subscription);

        // Instantiate a command and issue it on the bus
        ChangeWombatVisibilityServiceBusCommand command = new ChangeWombatVisibilityServiceBusCommand(true);

        commandBus.issueBlocking(command);

        ChangeWombatVisibilityServiceBusCommand receivedCommand = subscription.getQueue().poll();

        // Verify that the command handler performed the expected action
        assertEquals(command, receivedCommand, "Command received from queue should be same as sent command");
    }

    @Test
    void testUnsubscribe() throws ServiceBusCommandExecutorNotAssignedException
    {
        // Subscribe to commands
        CommandSubscription<ChangeWombatVisibilityServiceBusCommand> subscription = commandBus.subscribe(ChangeWombatVisibilityServiceBusCommand.class);

        // Unsubscribe
        commandBus.unsubscribe(subscription);

        // Instantiate a command and issue it on the bus
        ChangeWombatVisibilityServiceBusCommand command = new ChangeWombatVisibilityServiceBusCommand(true);

        assertThrows(ServiceBusCommandExecutorNotAssignedException.class, () -> {
            commandBus.issueBlocking(command);
        }, "ServiceBusCommandExecutorNotAssignedException should be thrown");
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
     * This executor should be overridden, it simply sets a flag to indicate whether or not it has been called
     *
     * @param command
     */
    private void executeChangeWombatVisibilityCommandToOverride(ChangeWombatVisibilityServiceBusCommand command)
    {
        executorToOverrideCalled = true;
    }
}