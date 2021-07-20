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
import com.thirdpartylabs.servicebus.commands.ChangeWombatVisibilityServiceBusCommand;
import com.thirdpartylabs.servicebus.events.ApplicationActivityServiceBusEvent;
import com.thirdpartylabs.servicebus.commands.CommandSubscription;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Worker that runs a lambda ChangeWombatVisibilityServiceBusCommand executor in a thread
 * Keeps track of visibility flags received via the commands so we can compare to those sent by the generator
 */
public class CommandExecutorWorker extends AbstractWorker
{
    private final ServiceBus serviceBus = ServiceBus.getInstance();
    private final List<Boolean> visibilityFlags = new ArrayList<>();

    @Override
    public void run()
    {
        CommandSubscription<ChangeWombatVisibilityServiceBusCommand> subscription = serviceBus.commands.subscribe(ChangeWombatVisibilityServiceBusCommand.class);

        /*
         * Now that the executor is registered, we can set our ready flag. The controller should block until all
         * workers set their ready flags
         */
        ready = true;

        // Pull commands from the subscription queue until we're told to exit
        while (true)
        {
            try
            {
                /*
                    We're dealing with a finite amount of commands, so we don't want to block indefinitely on the queue
                    Poll with  10ms timeout. Timeout will generate an InterruptException, so just catch and ignore
                 */
                ChangeWombatVisibilityServiceBusCommand command = subscription.getQueue().poll(10, TimeUnit.MILLISECONDS);

                if(command != null)
                {
                    executeChangeWombatVisibilityCommand(command);
                }

                // If the queue is empty, indicate that we have nothing to do
                working = (!subscription.getQueue().isEmpty());

                Thread.yield();
            }
            catch (Exception ignored)
            {
            }
        }
    }

    /**
     * A rudimentary executor for our test command. Sets the value of a member variable to the value specified in the
     * command and emits an event.
     *
     * @param command The command to execute
     */
    private void executeChangeWombatVisibilityCommand(ChangeWombatVisibilityServiceBusCommand command)
    {
        visibilityFlags.add(command.isVisible());

        String wombatVisibility = (command.isVisible()) ? "visible" : "invisible";

        //Instantiate an event and emit it on the bus
        ApplicationActivityServiceBusEvent event = new ApplicationActivityServiceBusEvent("Wombat visibility change: wombat is " + wombatVisibility);
        serviceBus.events.emit(event);
    }

    /**
     * @return array of received visibility flags
     */
    public boolean[] getVisibilityFlags()
    {
        boolean[] output = new boolean[visibilityFlags.size()];

        for (int i = 0; i < visibilityFlags.size(); i++)
        {
            output[i] = visibilityFlags.get(i);
        }

        return output;
    }
}
