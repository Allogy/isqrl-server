package com.allogy.isqrl.helpers;

import org.apache.tapestry5.StreamResponse;
import org.apache.tapestry5.services.Response;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Callback for directly writing to the outputstream from an action method
 * This class was inspired by {@link StreamResponse}
 *
 * @author robert.binna@uibk.ac.at
 * @url http://wiki.apache.org/tapestry/Tapestry5HowToCreateAComponentEventResultProcessor
 */
public interface OutputStreamResponse {

    /**
     * Returns the content type to be reported to the client.
     */
    String getContentType();

    /**
     * Implements a callback to directly write to the output stream.
     * The stream will be closed after this method returns.
     * The provided stream is wrapped in a {@link BufferedOutputStream} for efficiency.
     */
    public void writeToStream(OutputStream out) throws IOException;

    /**
     * Prepares the response before it is sent to the client. This is the place to set any response headers (e.g.
     * content-disposition).
     *
     * @param response Response that will be sent.
     */
    void prepareResponse(Response response);

}
