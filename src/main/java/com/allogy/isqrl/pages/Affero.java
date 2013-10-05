package com.allogy.isqrl.pages;

import org.apache.tapestry5.ioc.Messages;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.Response;
import org.apache.tapestry5.util.TextStreamResponse;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * User: robert
 * Date: 2013/10/05
 * Time: 1:07 PM
 */
public class Affero
{
    private static final String AFFERO_SOURCE_CODE_URL = "AFFERO_SOURCE_CODE_URL";

    @Inject
    private Response response;

    @Inject
    private Messages messages;

    Object onActivate() throws MalformedURLException
    {
        if (afferoSourceCodeUrlString == null)
        {
            afferoSourceCodeUrlString = System.getProperty(AFFERO_SOURCE_CODE_URL);

            if (afferoSourceCodeUrlString == null)
            {
                afferoSourceCodeUrlString = System.getenv(AFFERO_SOURCE_CODE_URL);
            }

            if (afferoSourceCodeUrlString==null)
            {
                response.setStatus(500);
                return new TextStreamResponse("text/plain", messages.get("out-of-compliance"));
            }
        }
        if (afferoSourceCodeUrl==null)
        {
            afferoSourceCodeUrl=new URL(afferoSourceCodeUrlString);
        }
        return afferoSourceCodeUrl;
    }

    private static String afferoSourceCodeUrlString;
    private static URL afferoSourceCodeUrl;
}
