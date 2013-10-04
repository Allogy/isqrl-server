package com.allogy.isqrl.services;


import com.allogy.isqrl.helpers.OutputStreamResponse;
import org.apache.tapestry5.internal.services.StreamResponseResultProcessor;
import org.apache.tapestry5.services.ComponentEventResultProcessor;
import org.apache.tapestry5.services.Response;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * ComponentEventResultProcessor that enables EventHandlers to return Callbacks
 * that can stream arbitrary data to the client.
 *
 * The class was inspired by {@link StreamResponseResultProcessor}
 *
 * @author robert.binna@uibk.ac.at
 * @url http://wiki.apache.org/tapestry/Tapestry5HowToCreateAComponentEventResultProcessor
 */
public class OutputStreamResponseResultProcessor implements ComponentEventResultProcessor<OutputStreamResponse>
{

    private static final int BUFFER_SIZE = 5000;

    private final Response response;

    public OutputStreamResponseResultProcessor(Response response) {
        this.response = response;
    }

    /**
     * Handles OutputStreamResponse
     *
     * @param streamResponse for streaming arbitrary data to the client using the response {@link OutputStream}
     *
     * @see ComponentEventResultProcessor#processResultValue(Object)
     */
    public void processResultValue(OutputStreamResponse streamResponse)
            throws IOException {
        OutputStream out = null;
        try {
            streamResponse.prepareResponse(response);
            out = new BufferedOutputStream(response.getOutputStream(streamResponse.getContentType()), BUFFER_SIZE);
            streamResponse.writeToStream(out);
            out.flush();
            out.close();
            out = null;
        } finally {
            if(out != null) { //can only be the case if an Exception was thrown because out was set to null before
                try {
                    out.close();
                } catch(IOException ioe) {
                    //ignores this IO exception because an exception is already on the way
                }
            }
        }
    }

}
