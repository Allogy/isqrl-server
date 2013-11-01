package com.allogy.isqrl.pages;

import com.allogy.isqrl.helpers.OutputStreamResponse;
import org.apache.tapestry5.services.Response;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Start page of application isqrl.
 */
public class Index
{

    Object onActivate(String filename)
    {
        if (filename.equals("robots.txt"))
        {
            return robotExclusionAdvisory;
        }

        return null;
    }

    private static final
    OutputStreamResponse robotExclusionAdvisory=new OutputStreamResponse()
    {
        public
        String getContentType()
        {
            return "text/plain";
        }

        public
        void writeToStream(OutputStream outputStream) throws IOException
        {
            PrintStream out=new PrintStream(outputStream);
            try
            {
                out.println("User-agent: *");
                out.println("Allow: /index/");
                out.println("Disallow: /");

                // Generated & random data is simply a waste...
                out.println("Disallow: /qr");

                // Must have been an elaborate robot to pick this url up from the javascript...
                out.println("Disallow: /poll");
            }
            finally
            {
                out.close();
            }
        }

        public
        void prepareResponse(Response response)
        {
            //no-op...
        }
    };

}
