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

package com.thirdpartylabs.servicebus.exceptions;

import com.thirdpartylabs.servicebus.commands.ServiceBusCommand;

/**
 * Thrown when a command is issued but no executor is registered for the command class
 */
public class ServiceBusCommandExecutorNotAssignedException extends Exception
{
    private final ServiceBusCommand command;

    public ServiceBusCommandExecutorNotAssignedException(String message, ServiceBusCommand command)
    {
        super(message);
        this.command = command;
    }

    /**
     * Get the commend that was issued on the bus
     *
     * @return The issued command
     */
    public ServiceBusCommand getCommand()
    {
        return command;
    }
}
