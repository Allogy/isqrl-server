package com.allogy.isqrl.pages;

import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.Response;
import org.apache.tapestry5.util.TextStreamResponse;

/**
 * User: robert
 * Date: 2013/10/03
 * Time: 11:53 PM
 */
public class Scan
{
    @Inject
    private Response response;

    Object onActivate()
    {
        if (hashY==null || x==null)
        {
            response.setStatus(400);
            return new TextStreamResponse("text/plain", "missing one or more arguments");
        }

        return null;
    }

    private String hashY;
    private String x;

    Object onActivate(String hashY, String x)
    {
        this.hashY=hashY;
        this.x=x;

        response.setStatus(500);
        return new TextStreamResponse("text/plain", "scan function is unimplemented");
    }

    public
    void withHashYAndX(String hashY, String x)
    {
        this.hashY=hashY;
        this.x=x;
    }

    String[] onPassivate()
    {
        return new String[]{
                hashY,
                x
        };
    }
}
