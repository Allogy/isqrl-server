package com.allogy.isqrl.helpers;

/**
 * User: robert
 * Date: 2013/10/04
 * Time: 1:51 AM
 */
public class DomainName
{
    public static
    String fromReferrer(String referrer)
    {
        /*
        "https://a.b/h/x".length() == 15
         */
        if (referrer==null || referrer.length()<11) return null;

        /*
        "https://a.b/h/x".lastIndexOf('/') == 11
         */
        int safeScanStart=9;
        int thirdSlash=referrer.indexOf('/', safeScanStart);
        if (thirdSlash<=0) return null;

        int nonUserPasswordColon;
        int start;

        int atSign=referrer.lastIndexOf('@', thirdSlash-1);
        if (atSign<=0)
        {
            int secondSlash=referrer.lastIndexOf('/', thirdSlash-1);
            if (secondSlash<=0) return null;
            start=secondSlash;
            nonUserPasswordColon=referrer.indexOf(':', secondSlash);
        }
        else
        {
            start=atSign;
            nonUserPasswordColon=referrer.lastIndexOf(':', thirdSlash);
        }

        int end;

        if (nonUserPasswordColon<0 || nonUserPasswordColon>thirdSlash)
        {
            end=thirdSlash;
        }
        else
        {
            end=nonUserPasswordColon;
        }

        String domain=referrer.substring(start+1, end);

        return domain;

    }

    private DomainName(){}

    public static
    boolean isSubdomainOf(String subDomain, String higherDomain)
    {
        //Prevent matching top level domains (e.g. if they claim a domain of "com")
        //Not that it would hurt, I suppose... just wouldn't be useful (bad reporting to user).
        if (higherDomain.indexOf('.')<=0) return false;

        //e.g. "www.apple.com".endsWith("apple.com") -> true
        return subDomain.toLowerCase().endsWith(higherDomain.toLowerCase());
    }
}
