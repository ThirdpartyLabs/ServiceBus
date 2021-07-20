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

/**
 * Response object that is returned for each request. Contains the value generated by the responder, along with
 * a status and option error code and error message.
 *
 * @param <T> Type of value that should be returned
 */
public class ServiceBusResponse<T>
{
    /**
     * Defines the possible states for a response
     */
    public enum Status
    {
        SUCCESS, ERROR
    }

    private final T responseValue;
    private Status status = Status.SUCCESS;
    private String message = "";
    private int errorCode = 0;

    /**
     * @param responseValue response generated by the responder
     */
    public ServiceBusResponse(T responseValue)
    {
        this.responseValue = responseValue;
    }

    /**
     * The value generated by the responder
     *
     * @return value of type T
     */
    public T getValue()
    {
        return responseValue;
    }

    /**
     * The status of the response
     *
     * @return Status
     */
    public Status getStatus()
    {
        return status;
    }

    /**
     * Set the status of the response.
     * <p>
     * Should be set by responders or error handlers
     *
     * @param status Status
     */
    public void setStatus(Status status)
    {
        this.status = status;
    }

    /**
     * Return the message set in the response.
     * <p>
     * Message is optional, it is intend to be used in error conditions to provide details about what went wrong.
     *
     * @return message string
     */
    public String getMessage()
    {
        return message;
    }

    /**
     * Set the optional message
     * <p>
     * Intend to be used in error conditions to provide details about what went wrong. It can be used for any purpose,
     * but any information that the requester needs from the responder should be included in the response value.
     *
     * @param message Optional status message
     */
    public void setMessage(String message)
    {
        this.message = message;
    }

    /**
     * Get the value set by the responder
     *
     * @return Value of type T
     */
    public T getResponseValue()
    {
        return responseValue;
    }

    /**
     * Return an error code set by the responder
     * <p>
     * These are assigned by the responder only, the bus does not assign error codes
     *
     * @return code
     */
    public int getErrorCode()
    {
        return errorCode;
    }

    /**
     * Set an error code that the requester can use for logging or to determine how to handle an error condition
     *
     * @param errorCode code
     */
    public void setErrorCode(int errorCode)
    {
        this.errorCode = errorCode;
    }
}