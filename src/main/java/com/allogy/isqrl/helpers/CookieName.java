package com.allogy.isqrl.helpers;

/**
 * User: robert
 * Date: 2013/10/04
 * Time: 2:50 PM
 */
public class CookieName
{
    public static
    String forDomainTrust(String domainName)
    {
        return "isqrl:domain:"+domainName;
    }

    /**
     * Cookies named after this function are intended to store a server-signed Z values.
     *
     * @param domainName
     * @param id_number
     * @return
     */
    public static
    String forZValue(String domainName, int id_number)
    {
        return "isqrl:z:"+domainName+":"+id_number;
    }

    private CookieName() {}

    public static
    String globalSpoofDetector()
    {
        return "isqrl:unspoofed";
    }
}
