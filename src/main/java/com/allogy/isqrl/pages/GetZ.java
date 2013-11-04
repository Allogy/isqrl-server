package com.allogy.isqrl.pages;

import com.allogy.isqrl.entities.Blip;
import com.allogy.isqrl.services.CrossRoads;
import com.allogy.isqrl.services.ServerSignature;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.Response;
import org.apache.tapestry5.util.TextStreamResponse;
import org.slf4j.Logger;

/**
 * User: robert
 * Date: 2013/10/05
 * Time: 2:11 AM
 */
public class GetZ
{
    private static final boolean LOW_MEMORY_MODE = Boolean.getBoolean("LOW_MEMORY_MODE");

    @Inject
    private Logger log;

    @Inject
    private Request request;

    @Inject
    private Response response;

    @Inject
    private CrossRoads crossRoads;

    @Inject
    private ServerSignature serverSignature;

    private boolean acceptZValuesFromPuts;

    Object onActivate()
    {
        String x=request.getParameter("x");
        String y=request.getParameter("y");

        if (x==null)
        {
            response.setStatus(400);
            return new TextStreamResponse("text/plain", "missing 'x' (via post variable)");
        }

        if (y==null)
        {
            response.setStatus(400);
            return new TextStreamResponse("text/plain", "missing 'y' (via post variable)");
        }

        Blip blip=crossRoads.getOrCreateBlip(x);

        String hashY=serverSignature.sha1HexOf(y);
        if (!hashY.equals(blip.getHashY()))
        {
            log.debug("invalid Y value: y={}", y);
            log.debug("computedHashY={}, blipHashY={}", hashY, blip.getHashY());
            response.setStatus(400);
            return new TextStreamResponse("text/plain", "invalid 'y' value: hashY must equal sha1(y), check for trailing line endings");
        }

        String z=blip.getHashZ();

        if (z==null)
        {
            response.setStatus(400);
            return new TextStreamResponse("text/plain", "'x' and 'y' are correct, but the 'z' value has not appeared... did your poll() call really return 'true'?");
        }

        if (acceptZValuesFromPuts)
        {
            log.debug("acceptZValuesFromPuts=true");
        }
        else
        if (blip.isViaPut())
        {
            response.setStatus(400);
            return new TextStreamResponse("text/plain", "the requested z-value was provided by a direct put-z request, add 'true' parameter to allow this behavior");
        }

        if (LOW_MEMORY_MODE)
        {
            crossRoads.remove(blip);
        }

        blip.setVoidMessage("Successfully processed & removed");

        return new TextStreamResponse("text/plain", z);
    }

    void onActivate(boolean acceptZValuesFromPuts)
    {
        this.acceptZValuesFromPuts=acceptZValuesFromPuts;
    }
}
