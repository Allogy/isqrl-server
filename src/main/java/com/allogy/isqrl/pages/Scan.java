package com.allogy.isqrl.pages;

import com.allogy.isqrl.entities.Blip;
import com.allogy.isqrl.helpers.CookieName;
import com.allogy.isqrl.helpers.DomainName;
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
import org.slf4j.Logger;

import javax.servlet.http.Cookie;
import java.util.ArrayList;
import java.util.Date;
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

    @Property
    private String subDomainOf;

    Object onActivate()
    {
        if (possiblySuperDomain ==null || hashY==null || x==null)
        {
            response.setStatus(400);
            return new TextStreamResponse("text/plain", "missing one or more arguments");
        }

        return null;
    }

    @Property
    private String possiblySuperDomain;

    private String hashY;
    private String x;

    @Property
    private List<String> distrustCauses;

    private int cookieDependentCauses;

    Object onActivate(String cookieFilteringDomain, String hashY, String x)
    {
        this.possiblySuperDomain=cookieFilteringDomain;
        this.hashY=hashY;
        this.x=x;

        /*
        "possiblySuperDomain" must be the first parameter (and is added so that) we can restrict the
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
            if (blip.getFullDomainName()==null)
            {
                blip.wait(2000);
                //NB: could *still* be null!
            }
        }
        */

        //NB: the post-side of the equation needs a little help, as it is not fed any of the yummy cookies. So we must account for
        boolean isPostRequest=request.getMethod().equals("POST");
        cookieDependentCauses =0;

        if (blip.getFullDomainName()==null)
        {
            if (!blip.isVoided())
            {
                blip.setVoidMessage("expecting poll() request strictly but shortly before scan() request, not before");
            }

            //Continuing would surly produce NullPointerExeceptions, etc... wait or die.
            response.setStatus(400);
            return new TextStreamResponse("text/plain", "invalid authentication ticket, or slow-auth-server race condition");
        }

        distrustCauses=new ArrayList<String>();

        if (blip.getFullDomainName().equals(cookieFilteringDomain))
        {
            this.subDomainOf=null;
        }
        else
        if (DomainName.isSubdomainOf(blip.getFullDomainName(), possiblySuperDomain))
        {
            this.subDomainOf=blip.getFullDomainName();
        }
        else
        if (!blip.isVoided())
        {
            blip.setVoidMessage("The site may not be configured correctly (or may be spoofed), as the domain names do not match. The QR code says '"+cookieFilteringDomain+"', but the backend says '"+blip.getFullDomainName()+"'.");
        }

        if (blip.getHashZ()!=null && !blip.isVoided())
        {
            blip.setVoidMessage("It looks like somebody else may have seen this qr code, if you hit the back button... then maybe it was you?!?!?!");
        }

        String previousHashY=cookies.readCookieValue(CookieName.forDomainTrust(possiblySuperDomain));

        if (previousHashY==null)
        {
            if (!isPostRequest)
            {
                distrustCauses.add("You have not previously authenticated to this domain name.");
                cookieDependentCauses++;
            }
        }
        else
        if (!previousHashY.equals(hashY))
        {
            distrustCauses.add("The sites identity seems to have changed; it was '"+previousHashY+"', but is now '"+hashY+"'.");
        }

        String signedZCookie=cookies.readCookieValue(CookieName.forZValue(possiblySuperDomain, USER_NUMBER));

        if (signedZCookie==null)
        {
            if (!isPostRequest)
            {
                distrustCauses.add("No matching identity (on this device) was found, so you may be *creating* a new account (or hooking up with an existing one).");
                cookieDependentCauses++;
            }
        }
        else
        if (!serverSignature.prependedSignatureMatches(signedZCookie))
        {
            distrustCauses.add("The authentication cookie stored on your phone was signed by a different authentication service. This is only okay if the site has since changed it's authentication provider.");
        }

        if (blip.isVoided())
        {
            distrustCauses.add("Their chosen authentication mechanisms handed you a voided authentication ticket: "+blip.getVoidMessage());
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
        Cookie[] cookies=cookieSource.getCookies();
        if (cookies==null)
        {
            return messages.get("no-cookies");
        }
        else
        {
            int n=cookieSource.getCookies().length;
            return messages.format("cookie-count", n);
        }
    }

    private
    void setPathLimitedCookie(String name, String value)
    {
        String path=request.getContextPath()+"/scan/"+ possiblySuperDomain +"/";

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
        this.possiblySuperDomain =domain;
        this.hashY=hashY;
        this.x=x;
    }

    public
    String getPossiblyUselessSpoofDetector()
    {
        String cookieName=CookieName.globalSpoofDetector();
        String value=cookies.readCookieValue(cookieName);
        if (value==null)
        {
            value=serverSignature.randomButMemorableString();
            cookies.writeCookieValue(cookieName, value, LONG_COOKIE_LIFETIME);
        }
        return value;
    }

    String[] onPassivate()
    {
        return new String[]{
                possiblySuperDomain,
                hashY,
                x
        };
    }

    public
    int getNumDistrustCauses()
    {
        return distrustCauses.size()- cookieDependentCauses;
    }

    public
    boolean isNotCleanlyTrusted()
    {
        return !distrustCauses.isEmpty();
    }

    public
    String getTrueBlue()
    {
        if (distrustCauses.isEmpty())
        {
            return "blue";
        }
        else
        {
            return "red";
        }
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

    Object onSelectedFromBlacklist()
    {
        /*
        We blacklist only the domain gathered by the referrer field, b/c it is much harder
        to spoof than the effectively-user-provided cookieFilteringPath...
        --
        Said another way, if a user determines "that's not example.com!"... do we ban "example.com"? :)
        --
        TODO: IMO it would be better to ban the root domain (and warn all subs), not a specific subdomain (they are then free to use any other subdomain).
         */
        String domainName=blip.getFullDomainName();
        String cookieName=CookieName.forDomainTrust(domainName);
        String cookieValue=messages.format("blacklisted-on", new Date());
        setPathLimitedCookie(cookieName, cookieValue);

        return Done.class;
    }

    public
    String getUnvalidatedZValue()
    {
        String domainName=possiblySuperDomain;
        String zCookieName=CookieName.forZValue(domainName, USER_NUMBER);
        String signedZ=cookies.readCookieValue(zCookieName);

        if (signedZ==null)
        {
            //TODO: should we replace server-side Z generation with a javascript (client-side) generator? what is the cost/benefit?
            return serverSignature.randomLongishString();
        }
        else
        {
            return serverSignature.removePrependedSignature(signedZ);
        }
    }

    @Inject
    private Logger log;

    Object onSelectedFromContinue()
    {
        String z=request.getParameter("z");
        int oldNumDistrustCauses=Integer.parseInt(request.getParameter("numDistrustCauses"));

        if (blip.isVoided())
        {
            response.setStatus(400);
            return new TextStreamResponse("text/plain", messages.format("blip-voided", blip.getVoidMessage()));
        }

        //TODO: in some cases, we may not be able to guard against a race condition.
        //What are they going to do? Delete the cookie between the rendered page and posted values? I think we are safe.
        if (distrustCauses.size()>oldNumDistrustCauses)
        {
            if (log.isDebugEnabled())
            {
                log.info("The following are the new set of distrustCauses...");
                for (String s : distrustCauses) {
                    log.debug(s);
                }
            }

            response.setStatus(400);
            return new TextStreamResponse("text/plain", messages.format("race-then-now", oldNumDistrustCauses, distrustCauses.size()));
        }

        String domainName= possiblySuperDomain;
        String zCookieName=CookieName.forZValue(domainName, USER_NUMBER);
        String signedZ=serverSignature.prependSignature(z);

        /*
         * It is no small debate as to if we should return Z plain (as if a password) or not...
         * Implementers are unlikely to treat Z as a password (hash/salt, etc), but if they
         * don't then Z is leaving our service in a state that is sufficient (if intercepted) to
         * be sufficient to authenticate.
         *
         * So too we thought to hash this a thousand times (or the like), but pause at this
         * stop-gap authentication solution degrading to a hashing service.
         *
         * What's more, it may be important for migration purposes (to full-SQRL) that this
         * algorithim be well known, or that a sys-admin be able to quickly convert cookies
         * (or the like).
         *
         * All things considered, we opt to just do a single round of sha-1.
         */
        String hashedZ=serverSignature.sha1HexOf(z);

        synchronized (blip)
        {
            blip.setHashZ(hashedZ);
            blip.notifyAll();
        }

        String hashY=blip.getHashY(); //???: or... this.hashY ???? When are they not equal?

        String trustCookieName=CookieName.forDomainTrust(domainName);

        setPathLimitedCookie(trustCookieName, hashY);
        setPathLimitedCookie(zCookieName, signedZ);

        return Done.class;
    }

    public
    String getSubDomainMessage()
    {
        if (subDomainOf!=null)
        {
            return messages.format("sub-domain-of", subDomainOf);
        }
        else
        {
            return "";
        }
    }

}
