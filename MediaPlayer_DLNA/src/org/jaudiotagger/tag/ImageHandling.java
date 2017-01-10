package org.jaudiotagger.tag;

import org.jaudiotagger.tag.datatype.Artwork;
import org.jaudiotagger.tag.id3.valuepair.ImageFormats;

import android.graphics.Bitmap;
import android.graphics.Matrix;

//import javax.imageio.ImageIO;
//import javax.imageio.ImageWriter;
//import java.awt.*;
//import java.awt.geom.AffineTransform;
//import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * User: paul
 * Date: 11-Dec-2008
 */
public class ImageHandling
{
    /**
     * Resize the image until the total size require to store the image is less than maxsize
     * @param artwork
     * @param maxSize
     * @throws IOException
     */
    public static void reduceQuality(Artwork artwork, int maxSize) throws IOException
    {
        while(artwork.getBinaryData().length > maxSize)
        {
            Bitmap srcImage = artwork.getImage();
            int w = srcImage.getWidth();
            int newSize = w /2;
            makeSmaller(artwork,newSize);
        }
    }
     /**
     * Resize image using Java 2D
      * @param artwork
      * @param size
      * @throws java.io.IOException
      */
    private static void makeSmaller(Artwork artwork,int size) throws IOException
    {
        Bitmap srcImage = (Bitmap)artwork.getImage();

        int w = srcImage.getWidth();
        int h = srcImage.getHeight();

        // Determine the scaling required to get desired result.
        float scaleW = (float) size / (float) w;
        float scaleH = (float) size / (float) h;

        Matrix matrix =new Matrix();
        matrix.postScale(scaleW, scaleH);
        Bitmap newbmp = Bitmap.createBitmap(srcImage, 0, 0, w, h, matrix,true);
        
        if(artwork.getMimeType()!=null && isMimeTypeWritable(artwork.getMimeType()))
        {
            artwork.setBinaryData(writeImage(newbmp,artwork.getMimeType()));
        }
        else
        {
            artwork.setBinaryData(writeImageAsPng(newbmp));
        }
    }

    public static boolean isMimeTypeWritable(String mimeType)
    {
        //Iterator<ImageWriter> writers =  ImageIO.getImageWritersByMIMEType(mimeType);
        //return writers.hasNext();
    	return false;
    }
    /**
     *  Write buffered image as required format
     *
     * @param bi
     * @param mimeType
     * @return
     * @throws IOException
     */
    public static byte[] writeImage(Bitmap bi,String mimeType) 
    {
        /*Iterator<ImageWriter> writers =  ImageIO.getImageWritersByMIMEType(mimeType);
        if(writers.hasNext())
        {
            ImageWriter writer = writers.next();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            writer.setOutput(ImageIO.createImageOutputStream(baos));
            writer.write(bi);
            return baos.toByteArray();
        }
        throw new IOException("Cannot write to this mimetype");*/
    	
    	return null;
    }

    /**
     *
     * @param bi
     * @return
     * @throws IOException
     */
    public static byte[] writeImageAsPng(Bitmap bi) 
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
    	bi.compress(Bitmap.CompressFormat.PNG, 100, baos);
    	return baos.toByteArray();
    }

    /**
     * Show read formats
     *
     * On Windows supports png/jpeg/bmp/gif
     */
    public static void showReadFormats()
    {
        
    }

    /**
     * Show write formats
     *
     * On Windows supports png/jpeg/bmp
     */
    public static void showWriteFormats()
    {
        
    }

}
