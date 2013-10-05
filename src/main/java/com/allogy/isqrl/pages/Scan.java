package com.allogy.isqrl.pages;

import com.allogy.isqrl.entities.Blip;
import com.allogy.isqrl.helpers.CookieName;
import com.allogy.isqrl.services.CrossRoads;
import com.allogy.isqrl.services.ServerSignature;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.internal.services.CookieSink;
import org.apache.tapestry5.internal.services.CookieSource;
import org.apache.tapestry5.ioc.Messages;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.services.Cookies;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.Response;
import org.apache.tapestry5.util.TextStreamResponse;

import javax.servlet.http.Cookie;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * User: robert
 * Date: 2013/10/03
 * Time: 11:53 PM
 */
public class Scan
{
    /**
     * In case we decide to build out "multiple user accounts" later.
     */
    private static final int USER_NUMBER = 0;
    private static final int LONG_COOKIE_LIFETIME = (int) TimeUnit.DAYS.toSeconds(365 * 10);

    @Inject
    private Cookies cookies;

    @Inject
    private Request request;

    @Inject
    private Response response;

    @Inject
    private CrossRoads crossRoads;

    @Inject
    private ServerSignature serverSignature;

    @Property
    private Blip blip;

    Object onActivate()
    {
        if (cookieFilteringDomain==null || hashY==null || x==null)
        {
            response.setStatus(400);
            return new TextStreamResponse("text/plain", "missing one or more arguments");
        }

        return null;
    }

    private String cookieFilteringDomain;
    private String hashY;
    private String x;

    @Property
    private List<String> distrustCauses;

    Object onActivate(String cookieFilteringDomain, String hashY, String x)
    {
        this.cookieFilteringDomain=cookieFilteringDomain;
        this.hashY=hashY;
        this.x=x;

        /*
        "cookieFilteringDomain" must be the first parameter (and is added so that) we can restrict the
        set of cookies we receive from the client. This is important so that we don't get a million
        cookies sent for "all the sites you visit" for every request. That would be a huge scalability
        issue.
         */

        this.blip=crossRoads.getOrCreateBlip(x);

        if (blip.getHashY()==null)
        {
            blip.setHashY(hashY);
        }
        else
        if (!hashY.equals(blip.getHashY()))
        {
            if (!blip.isVoided())
            {
                blip.setVoidMessage("hashY mismatch from Scan page");
            }
        }

        /*
        Wait for up to two seconds for the blip's domain name to come in.
        NB: it *SHOULD* have already arrived a long time ago, so this may be a bit pedantic.
        * /
        synchronized (blip)
        {
            if (blip.getDomainName()==null)
            {
                blip.wait(2000);
                //NB: could *still* be null!
            }
        }
        */

        if (blip.getDomainName()==null)
        {
            if (!blip.isVoided())
            {
                blip.setVoidMessage("expecting poll() request strictly but shortly before scan() request, not before");
            }

            //Continuing would surly produce NullPointerExeceptions, etc... wait or die.
            response.setStatus(400);
            return new TextStreamResponse("text/plain", "invalid authentication ticket, or slow-auth-server race condition");
        }

        //TODO: I think this same logic is needed in the event handler!
        if (blip.getZ()!=null && !blip.isVoided())
        {
            blip.setVoidMessage("sorry, this qr code has been scanned by someone else");
        }

        String previousHashY=cookies.readCookieValue(CookieName.forDomainTrust(blip.getDomainName()));

        distrustCauses=new ArrayList<String>();

        if (previousHashY==null)
        {
            distrustCauses.add("You have not previously authenticated to this domain name.");
        }
        else
        if (!previousHashY.equals(hashY))
        {
            distrustCauses.add("The sites identity seems to have changed; it was '"+previousHashY+"', but is now '"+hashY+"'.");
        }

        if (!blip.getDomainName().equals(cookieFilteringDomain))
        {
            distrustCauses.add("The site may not be configured correctly (or be spoofed), as the domain names do not match. The QR code says '"+cookieFilteringDomain+"', but the backend says '"+blip.getDomainName()+"'.");
        }

        String signedZCookie=cookies.readCookieValue(CookieName.forZValue(blip.getDomainName(), USER_NUMBER));

        if (signedZCookie==null)
        {
            distrustCauses.add("No matching identity (on this device) was found, so you may be *creating* a new account (or hooking up with an existing one).");
        }
        else
        if (!serverSignature.prependedSignatureMatches(signedZCookie))
        {
            distrustCauses.add("The authentication cookie stored on your phone was signed by a different authentication service. This is only okay if the site has since changed it's authentication provider.");
        }

        if (blip.isVoided())
        {
            distrustCauses.add("Their chosen authentication mechanisms handed you a voided authentication ticket.");
        }

        //TODO: if this project is long-lived, all the above distrust causes should be offloaded to the message system (for localization)

        return null;
    }

    @Inject
    private CookieSink cookieSink;

    @Inject
    private CookieSource cookieSource;

    public
    String getCookieCount()
    {
        int n=cookieSource.getCookies().length;
        return messages.format("cookie-count", n);
    }

    private
    void setPathLimitedCookie(String name, String value)
    {
        String path=request.getContextPath()+"/scan/"+cookieFilteringDomain+"/";

        //cookies.writeDomainCookieValue(name, value, path, domain, LONG_COOKIE_LIFETIME); ???
        //We have to compose the cookie ourself or else we could change the symbol: "tapestry.default-cookie-max-age"

        Cookie cookie=new Cookie(name, value);
        cookie.setPath(path);
        cookie.setSecure(productionMode);
        cookie.setMaxAge(LONG_COOKIE_LIFETIME);

        cookieSink.addCookie(cookie);
    }

    @Inject
    @Symbol(SymbolConstants.PRODUCTION_MODE)
    private boolean productionMode;

    public
    void withDomainHashYAndX(String domain, String hashY, String x)
    {
        this.cookieFilteringDomain=domain;
        this.hashY=hashY;
        this.x=x;
    }

    String[] onPassivate()
    {
        return new String[]{
                cookieFilteringDomain,
                hashY,
                x
        };
    }

    public
    boolean isNotCleanlyTrusted()
    {
        return !distrustCauses.isEmpty();
    }

    @Property
    private String cause;

    @Inject
    private Messages messages;

    public
    String getContinueMessage()
    {
        if (distrustCauses.isEmpty())
        {
            /*
            Technically speaking, they have already "sent" the auth (to the auth server), so this is from the users standpoint...
            Should we "send" it to the originating party? Also, using "send" helps gather the idea that the tokens are stored
            device-side, which is better prep and understanding for full-SQRL adoption.
             */
            return messages.get("send-auth");
        }
        else
        {
            return messages.get("continue-with-risk");
        }
    }

    Object onSelectedFromContinue()
    {
        response.setStatus(400);
        return new TextStreamResponse("text/plain", "continue is not yet implemented");
    }

    Object onSelectedFromBlacklist()
    {
        response.setStatus(400);
        return new TextStreamResponse("text/plain", "blacklist is not yet implemented");
    }

}
