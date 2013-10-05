package com.allogy.isqrl.pages;

import com.allogy.isqrl.helpers.OutputStreamResponse;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.apache.tapestry5.Asset;
import org.apache.tapestry5.StreamResponse;
import org.apache.tapestry5.annotations.InjectPage;
import org.apache.tapestry5.annotations.Path;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.PageRenderLinkSource;
import org.apache.tapestry5.services.Response;
import org.apache.tapestry5.util.TextStreamResponse;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * User: robert
 * Date: 2013/10/03
 * Time: 11:36 PM
 */
public class QR
{
    private static final boolean QR_GENERATION_DOWN=Boolean.getBoolean("ISQRL_DOWN") || "true".equals(System.getenv("ISQRL_DOWN"));

    private Response response;

    Object onActivate()
    {
        response.setStatus(400);
        return new TextStreamResponse("text/plain", "missing one or more arguments");
    }

    @InjectPage
    private Scan scan;

    @Inject
    private Logger log;

    @Inject
    private PageRenderLinkSource pageRenderLinkSource;

    @Inject
    @Path("context:qr-down.png")
    private Asset qrDownImage;

    Object onActivate(String domainName, String hashY, String xAndFormat) throws WriterException, IOException
    {
        if (QR_GENERATION_DOWN) return new StreamResponse()
        {
            public String getContentType()
            {
                return "image/png";
            }

            public InputStream getStream() throws IOException
            {
                return qrDownImage.getResource().openStream();
            }

            public void prepareResponse(Response response)
            {
                //You might think that we could ask them to cache it a while... but it won't help b/c the url changes constantly (it includes "x").
            }
        };

        String imageFormat="png";
        String x=xAndFormat;

        int period=xAndFormat.lastIndexOf('.');
        if (period > 0 && period < xAndFormat.length())
        {
            x=xAndFormat.substring(0, period);
            String ext=xAndFormat.substring(period+1).toLowerCase();

            if (ext.startsWith("tif"  )) imageFormat="tiff";
            else if (ext.equals("gif" )) imageFormat="gif";
            else if (ext.equals("jpg" )) imageFormat="jpeg";
            else if (ext.equals("jpeg")) imageFormat="jpeg";
            else if (ext.equals("png" )) imageFormat="png";
            else
            {
                response.setStatus(400);
                return new TextStreamResponse("text/plain", "unknown or invalid format: "+ext);
            }
        }

        scan.withDomainHashYAndX(domainName, hashY, x);
        final String url=pageRenderLinkSource.createPageRenderLink(Scan.class).toAbsoluteURI();

        log.trace("encoding url as QR code: {}", url);

        final String finalImageFormat=imageFormat;
        final QRCodeWriter qrCodeWriter=new QRCodeWriter();
        BarcodeFormat barcodeFormat=BarcodeFormat.QR_CODE;
        int width =150;
        int height=150;
        final BitMatrix bitMatrix = qrCodeWriter.encode(url, barcodeFormat, width, height);

        return new OutputStreamResponse()
        {
            public String getContentType()
            {
                return "image/"+finalImageFormat;
            }

            public void writeToStream(OutputStream outputStream) throws IOException
            {
                MatrixToImageWriter.writeToStream(bitMatrix, finalImageFormat, outputStream);
            }

            public void prepareResponse(Response response)
            {
                //no-op...
            }
        };
    }
}
