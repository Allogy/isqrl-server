package com.allogy.isqrl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

/**
 * User: robert
 * Date: 2013/10/03
 * Time: 10:59 PM
 */
@Test
public class QRCodes
{
    @Test
    void benchmark() throws WriterException, IOException
    {
        QRCodeWriter qrCodeWriter=new QRCodeWriter();
        String url="https://isqrl.allogy.com/scan/8b775f16d8fc58dc8fe38c87f66e55d973e23407/A3k2TjrkimptR.png";
        BarcodeFormat barcodeFormat=BarcodeFormat.QR_CODE;
        int width =150;
        int height=150;
        BitMatrix bitMatrix = qrCodeWriter.encode(url, barcodeFormat, width, height);
        //MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
        MatrixToImageWriter.writeToFile(bitMatrix, "PNG", new File("/tmp/qr.png"));
    }

    public static
    void main(String[] args)
    {
        try {
            new QRCodes().benchmark();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
